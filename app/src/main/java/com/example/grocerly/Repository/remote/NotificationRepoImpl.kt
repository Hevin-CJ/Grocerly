package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.Constants.FCM_TOKEN
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepoImpl @Inject constructor(private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging,
    private val grocerlyDataStore: GrocerlyDataStore
) {

    suspend fun fetchOrderForNotification(orderId: String, productId: String): NetworkResult<Pair<Order, CartProduct>> {
        return try {
            val userId = auth.currentUser?.uid ?: return NetworkResult.Error("User not authenticated")

            val document = db.collection(USERS).document(userId)
                .collection(ORDERS).document(orderId)
                .get()
                .await()

            val order = document.toObject(Order::class.java)
            val cartProduct = order?.items?.find { it.product.productId == productId }

            if (order != null && cartProduct != null) {
                NetworkResult.Success(Pair(order, cartProduct))
            } else {
                NetworkResult.Error("Order or Product not found.")
            }

        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown error occurred")
        }
    }


    suspend fun claimActiveDeviceToken() {
        try {
            val userId = auth.currentUser?.uid ?: return

            val currentToken = messaging.token.await()

            val savedToken = grocerlyDataStore.getSavedFcmToken().first()

            if (currentToken != savedToken) {
                val data = mapOf(FCM_TOKEN to currentToken)

                db.collection(USERS).document(userId)
                    .set(data, SetOptions.merge())
                    .await()

                grocerlyDataStore.saveFcmTokenLocally(currentToken)
                Log.d("FCM_SYNC", "New token synced to Firestore: $currentToken")
            } else {
                Log.d("FCM_SYNC", "Token unchanged. API call skipped.")
            }

        } catch (e: Exception) {
            Log.e("FCM_SYNC", "Failed to claim active token", e)
        }
    }
}