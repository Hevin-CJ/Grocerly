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
import com.example.grocerly.utils.Mappers.getFutureDateString
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
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class CartRepoImpl @Inject constructor(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    val userId = auth.currentUser?.uid.toString()

    private val cartRef = db.collection(USERS).document(userId).collection(CART)


    suspend fun addProductToCart(cartProduct: CartProduct): NetworkResult<Unit> {
        return try {

            val productId = cartProduct.product.productId
            val documentRef = cartRef.document(productId)

            val snapshot = documentRef.get().await()

            if (snapshot.exists()) {

                documentRef.delete().await()
                NetworkResult.Success(Unit)
            } else {
                val updatedProduct = cartProduct.copy(
                    deliveryDate = getFutureDateString(
                        cartProduct.product.packUpTime,
                        "dd MMMM, E"
                    )
                )
                documentRef.set(updatedProduct).await()
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

        if (cartProduct.quantity > maxQuantity) {
            return NetworkResult.Error("Maximum quantity allowed for \n${cartProduct.product.itemName} is $maxQuantity.")
        }

        return try {
            cartRef.document(cartProduct.product.productId)
                .update(QUANTITY, cartProduct.quantity)
                .await()
            NetworkResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to update quantity")
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
                return@addSnapshotListener
            }

            val cartProducts = snapshot.documents.mapNotNull { it.toObject(CartProduct::class.java) }

            trySend(NetworkResult.Success(cartProducts))

            sycJob?.cancel()
            sycJob = launch {
                updateCartItemsWithCurrentData(cartProducts)
            }

        }
        awaitClose {
            sycJob?.cancel()
            listener.remove()
        }
    }


    suspend fun updateCartItemsWithCurrentData(cartItems: List<CartProduct>) {
        if (cartItems.isEmpty()) return

        val productMap = coroutineScope {
            cartItems.groupBy { it.product.partnerId }
                .flatMap { (partnerId, items) ->

                    items.chunked(10).map { chunk ->
                        async {
                            try {
                                val productIds = chunk.map { it.product.productId }
                                val snapshot = db.collection(PARTNERS)
                                    .document(partnerId)
                                    .collection(PRODUCTS)
                                    .whereIn("productId", productIds)
                                    .get()
                                    .await()
                                snapshot.toObjects(Product::class.java)
                            } catch (e: Exception) {
                                emptyList<Product>()
                            }
                        }
                    }
                }
                .awaitAll()
                .flatten()
                .associateBy { it.productId }
        }


        val batch = db.batch()
        var requiresNetworkWrite = false


        cartItems.forEach { cartItem ->
            val productId = cartItem.product.productId
            val updatedProduct = productMap[productId]
            val docRef = cartRef.document(productId)

            if (updatedProduct == null) {
                batch.delete(docRef)
                requiresNetworkWrite = true
            } else {

                val newDeliveryDate = getFutureDateString(updatedProduct.packUpTime, "dd MMMM, E")

                val updatedCartItem = cartItem.copy(
                    deliveryDate = newDeliveryDate,
                    product = updatedProduct.copy(
                        productId = productId,
                        image = updatedProduct.image,
                        itemName = updatedProduct.itemName,
                        itemPrice = updatedProduct.itemPrice,
                        itemOriginalPrice = updatedProduct.itemOriginalPrice
                    )
                )

                if (updatedCartItem != cartItem) {

                    batch.set(docRef, updatedCartItem, SetOptions.merge())
                    requiresNetworkWrite = true
                }
            }
        }


        if (requiresNetworkWrite) {
            try {
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("CartUpdateError", "Failed to commit cart batch updates", e)
            }
        }
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

            if (snapshot==null || snapshot.isEmpty){
                trySend(NetworkResult.Success(0f))
                return@addSnapshotListener
            }

            snapshot.let {
                val amount = it.documents.mapNotNull { doc -> doc.toObject(CartProduct::class.java) }
                        .sumOf { cartProduct ->
                            (cartProduct.product.itemOriginalPrice ?: 0) * (cartProduct.quantity ?: 1)
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


            if (snapshot == null || snapshot.isEmpty) {
                val emptyMap: Map<String, Int> = linkedMapOf(
                    "Price (0 Items)" to 0,
                    "Product Discount" to 0,
                    "Platform Fee" to 0,
                    "Delivery Charge" to 0,
                    "Applied Coupons" to 0,
                    "Total Amount" to 0
                )
                trySend(NetworkResult.Success(emptyMap))
                return@addSnapshotListener
            }

            snapshot.let {
                val amount = it.documents.mapNotNull { doc -> doc.toObject(CartProduct::class.java) }
                        .sumOf { cartProduct ->
                            (cartProduct.product.itemPrice ?: 0) * (cartProduct.quantity ?: 1)
                        }

                val discountAmount =  amount - cartItems.sumOf { (it.product.itemOriginalPrice ?: 0) * (it.quantity ?: 1) }

                val platformFee = (amount * 0.01f).roundToInt()
                val deliveryFee = calculateDeliveryCharge(amount)
                val coupon = couponAmount
                val finalAmount = (amount + platformFee + deliveryFee.totalCharge) - (coupon + discountAmount)


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