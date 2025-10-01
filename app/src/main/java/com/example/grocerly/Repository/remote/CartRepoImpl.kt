package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.DeliveryCharge
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Constants.CART
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.Constants.QUANTITY
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.PackUp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@ActivityRetainedScoped
class CartRepoImpl @Inject constructor(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    val userId = auth.currentUser?.uid.toString()

    private val cartRef = db.collection(USERS).document(userId).collection(CART)


    suspend fun addProductToCart(cartProduct: CartProduct): NetworkResult<Unit> {
        return try {

            val existingDoc = cartRef.document(cartProduct.product.productId).get().await()
            val existingProduct = existingDoc.toObject(CartProduct::class.java)

            if (existingProduct?.product?.productId == cartProduct.product.productId) {

                val newProduct = existingProduct.copy(
                    deliveryDate = getFutureDateString(
                        cartProduct.product.packUpTime,
                        "dd MMMM, E"
                    )
                )
                if (newProduct.quantity <= (cartProduct.product.maxQuantity ?: 1)) {
                    cartRef.document(existingProduct.product.productId)
                        .update(newProduct.toHashMap()).await()
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error("Maximum Quantity")
                }

            } else {
                val updated = cartProduct.copy(
                    deliveryDate = getFutureDateString(
                        cartProduct.product.packUpTime,
                        "dd MMMM, E"
                    )
                )

                cartRef.document(cartProduct.product.productId).set(updated).await()
                NetworkResult.Success(Unit)

            }


        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown Error Occurred")
        }

    }

    private fun CartProduct.toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "product" to product,
            "deliveryDate" to deliveryDate
        )
    }

    suspend fun updateQuantity(cartProduct: CartProduct): NetworkResult<Unit> {
        val maxQuantity = cartProduct.product.maxQuantity ?: 1
        val documentRef: DocumentReference = cartRef.document(cartProduct.product.productId)
        return try {

            val updatedQuantity = cartProduct.quantity.coerceAtMost(maxQuantity)
            documentRef.update(QUANTITY, updatedQuantity).await()

            if (cartProduct.quantity > maxQuantity) {
                NetworkResult.Error("Maximum quantity allowed for \n${cartProduct.product.itemName} is $maxQuantity.")
            } else {
                NetworkResult.Success(Unit)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message.toString())
        }
    }


    fun fetchAllCartItems(): Flow<NetworkResult<List<CartProduct>>> = callbackFlow {
        var sycJob: Job? = null
        val listener = cartRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                trySend(
                    NetworkResult.Error(
                        exception.message ?: "Unable to fetch Products,Please try later...."
                    )
                )
            }

            if (snapshot == null || snapshot.isEmpty) {
                trySend(NetworkResult.Success(emptyList()))
            }

            snapshot?.let {
                val cartProducts =
                    snapshot.documents.mapNotNull { it.toObject(CartProduct::class.java) }

                sycJob = launch {
                    updateCartItemsWithCurrentData(cartProducts)
                }
                trySend(NetworkResult.Success(cartProducts))

            }


        }
        awaitClose {
            sycJob?.cancel()
            listener.remove()
        }
    }


    suspend fun updateCartItemsWithCurrentData(cartItems: List<CartProduct>) = coroutineScope {
        if (cartItems.isEmpty()) return@coroutineScope

        val grouped = cartItems
            .groupBy { it.product.partnerId }
            .flatMap { (partnerId, items) ->
                items.chunked(10).map { chunk ->
                    async {
                        try {
                            val productSnapshot = db.collection(PARTNERS)
                                .document(partnerId)
                                .collection(PRODUCTS)
                                .whereIn("productId", chunk.map { it.product.productId })
                                .get()
                                .await()

                            productSnapshot.toObjects(Product::class.java)
                        } catch (e: Exception) {
                            emptyList<Product>()
                        }
                    }
                }

            }.awaitAll().flatten()


        val productMap = grouped.associateBy { it.productId }
        Log.d("productmap", productMap.toString())
        cartItems.map { cartItem ->
            async {
                try {

                    val updatedProduct = productMap[cartItem.product.productId]

                    if (updatedProduct == null) {
                        val itemToDelete =
                            cartRef.document(cartItem.product.productId).get().await()

                        if (itemToDelete.exists()) {
                            cartRef.document(cartItem.product.productId).delete().await()
                        }

                    } else {
                        val updatedCartItem = cartItem.copy(
                            deliveryDate = getFutureDateString(
                                updatedProduct.packUpTime,
                                "dd MMMM, E"
                            ),
                            product = updatedProduct.copy(
                                productId = cartItem.product.productId,
                                image = updatedProduct.image,
                                itemName = updatedProduct.itemName,
                                itemPrice = updatedProduct.itemPrice,
                                itemOriginalPrice = updatedProduct.itemOriginalPrice
                            )
                        )

                        if (updatedCartItem != cartItem) {
                            cartRef.document(cartItem.product.productId)
                                .set(updatedCartItem, SetOptions.merge())
                                .await()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "CartUpdateError",
                        "Failed to update cart item ${cartItem.product.productId}",
                        e
                    )
                }
            }
        }.awaitAll()

    }


    suspend fun deleteItemFromCart(cartProduct: CartProduct): NetworkResult<Unit> {
        return try {

            cartRef
                .document(cartProduct.product.productId)
                .delete()
                .await()

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message)
        }
    }

    fun fetchTotalAmountFromCart(): Flow<NetworkResult<Float>> = callbackFlow {

        val listener = cartRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NetworkResult.Error(error.message ?: "Unable to fetch amount"))
                return@addSnapshotListener
            }

            snapshot?.let {
                val amount =
                    it.documents.mapNotNull { doc -> doc.toObject(CartProduct::class.java) }
                        .sumOf { cartProduct ->
                            (cartProduct.product.itemOriginalPrice ?: 0) * (cartProduct.quantity
                                ?: 1)
                        }
                        .toFloat()

                trySend(NetworkResult.Success(amount))

            }

        }

        awaitClose {
            listener.remove()
        }
    }


    fun fetchTotalPriceFromDb(
        cartItems: List<CartProduct>,
        couponAmount: Int = 0
    ): Flow<NetworkResult<Map<String, Int>>> = callbackFlow {

        val listener = cartRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(
                    NetworkResult.Error(
                        error.message ?: "Unable to fetch Total Amount\n Please try later..."
                    )
                )
                return@addSnapshotListener
            }

            snapshot?.let {
                val amount =
                    it.documents.mapNotNull { doc -> doc.toObject(CartProduct::class.java) }
                        .sumOf { cartProduct ->
                            (cartProduct.product.itemOriginalPrice ?: 0) * (cartProduct.quantity
                                ?: 1)
                        }

                val discountAmount =
                    cartItems.sumOf { (it.product.itemPrice ?: 0) * (it.quantity ?: 1) } - amount
                Log.d("productdiscount", discountAmount.toString())

                val platformFee = (amount * 0.01f).roundToInt()
                val deliveryFee = calculateDeliveryCharge(amount)
                val coupon = couponAmount
                val finalAmount = (amount + platformFee + deliveryFee.totalCharge) - coupon


                val priceMap: Map<String, Int> = linkedMapOf(
                    "Price (${cartItems.size} Items)" to amount,
                    "Product Discount" to discountAmount,
                    "Platform Fee" to platformFee,
                    deliveryFee.chargeType to deliveryFee.totalCharge,
                    "Applied Coupons" to coupon,
                    "Total Amount" to finalAmount
                )


                trySend(NetworkResult.Success(priceMap))

            }

        }

        awaitClose {
            listener.remove()
        }

    }

    fun getFutureDateString(packUp: PackUp, format: String = "dd MMMM, E"): String {

        val zoneId = ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.now(zoneId)
        val noonToday = now.withHour(12).withMinute(0).withSecond(0).withNano(0)


        val daysToAdd = when (packUp) {
            PackUp.selectTime -> 0
            PackUp.oneday -> 1
            PackUp.twoday -> 2
            PackUp.threeday -> 3
        }

        val adjustedDaysToAdd = if (now.isAfter(noonToday)) {
            daysToAdd + 1
        } else {
            daysToAdd
        }

        val futureDate = now.plusDays(adjustedDaysToAdd.toLong())

        val formatter = DateTimeFormatter.ofPattern(format, Locale.getDefault())
        return futureDate.format(formatter)

    }


    fun calculateDeliveryCharge(price: Int): DeliveryCharge {
        return when {
            price < 500 -> DeliveryCharge(40, "Standard delivery charge")
            price >= 500 && price < 800 -> DeliveryCharge(0, "Delivery charge")
            price >= 800 && price < 1000 -> DeliveryCharge(69, "Secured packaging fee")
            price >= 1000 -> DeliveryCharge(89, "Promise protection fee")
            else -> DeliveryCharge()
        }
    }

}