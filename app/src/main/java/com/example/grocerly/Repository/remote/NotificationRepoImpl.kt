package com.example.grocerly.Repository.remote

import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepoImpl @Inject constructor(private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
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
}