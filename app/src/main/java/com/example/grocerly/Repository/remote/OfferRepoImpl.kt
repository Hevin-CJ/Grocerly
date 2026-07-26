package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.Repository.local.OfferLocalRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Constants.CART
import com.example.grocerly.utils.Constants.OFFERS
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.Mappers.getFutureDateString
import com.example.grocerly.utils.Mappers.toOfferEntityList
import com.example.grocerly.utils.Mappers.toOfferItem
import com.example.grocerly.utils.Mappers.toOfferItemList
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ActivityRetainedScoped
class OfferRepoImpl @Inject constructor(private val offerLocalRepoImpl: OfferLocalRepoImpl,private val db: FirebaseFirestore,private val auth: FirebaseAuth) {


    fun getOffers(): Flow<NetworkResult<List<OfferItem>>> {
        return offerLocalRepoImpl.getOffers()
            .map { localEntities ->
                val localOffers = localEntities.map { it.toOfferItem() }
                if (localOffers.isEmpty()) {
                    NetworkResult.Loading()
                } else {
                    NetworkResult.Success(localOffers)
                }
            }
    }


     suspend fun syncOffersFromNetwork() {
        try {
            val querySnapshot = db.collectionGroup(OFFERS).get().await()
            val remoteOffers =   querySnapshot.documents.mapNotNull { it.toObject(OfferItem::class.java) }
            offerLocalRepoImpl.upsertOffer(remoteOffers.toOfferEntityList())
        } catch (e: Exception) {
            Log.e("OfferSync", "Failed to sync latest offers: ${e.message}")
        }
    }

    suspend fun addOfferFromFirebaseToCart(productId: String, partnerId: String): NetworkResult<Unit> = coroutineScope {

        if (partnerId.isBlank() || productId.isBlank()) {
            return@coroutineScope NetworkResult.Error("Invalid offer")
        }

        val userId = auth.currentUser?.uid ?: return@coroutineScope NetworkResult.Error("User not authenticated")

        return@coroutineScope try {
            val partnerRef = db.collection(PARTNERS).document(partnerId).collection(PRODUCTS).document(productId)
            val cartRef = db.collection(USERS).document(userId).collection(CART).document(productId)


            val cartDeferred = async { cartRef.get().await() }
            val productDeferred = async { partnerRef.get().await() }

            if (cartDeferred.await().exists()) {
                return@coroutineScope NetworkResult.Error("Already in cart")
            }


            val productSnapshot = productDeferred.await()
            if (!productSnapshot.exists()) {
                return@coroutineScope NetworkResult.Error("This offer is no longer available.")
            }

            val product = productSnapshot.toObject(Product::class.java)
                ?: return@coroutineScope NetworkResult.Error("Failed to parse product data.")

            val deliveryDate = getFutureDateString(product.packUpTime, "dd MMMM, E")
            val newCartItem = CartProduct(
                product = product,
                quantity = 1,
                deliveryDate = deliveryDate
            )

            cartRef.set(newCartItem).await()

            NetworkResult.Success(Unit)

        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to add offer to cart")
        }
    }


    fun createDummyOffer(): OfferItem {

        return OfferItem(
            offerId = "offer_123",
            offerImage = "",
            offerBgColor = "#E3F2FD",
            buttonText = "",
            buttonBgColor = "#FFFFFF",
            buttonTxtColor = "#FFFFFF",
            descriptionText = "No Offers Found",
            descriptionTextColor = "#000000",
            productId = ""
        )
    }
}