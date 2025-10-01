package com.example.grocerly.Repository.remote

import com.example.grocerly.model.Order
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.PAYMENTS
import com.example.grocerly.utils.Constants.SAVED_CARDS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.PaymentMethodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.collections.emptyList


@ActivityRetainedScoped
class PaymentRepoImpl @Inject constructor( private val auth: FirebaseAuth,private val db: FirebaseFirestore,private val cartRepoImpl: CartRepoImpl) {

    private val userId = auth.currentUser?.uid.toString()
    private val cardRef = db.collection(USERS).document(userId).collection(SAVED_CARDS)


    suspend fun checkCvvForPayment(cardId: String, cvv: String): NetworkResult<String> {
        return try {

            if (userId.isEmpty()){
                return NetworkResult.Error("Authentication Required, Please Login for payment")
            }

            val cleanCvv = cvv.replace("\\s".toRegex(),"")

           if (cleanCvv.isEmpty()){
               return NetworkResult.Error("CVV cannot be empty")
           }

            if (!cleanCvv.matches(Regex("^\\d{3,4}$"))){
                return NetworkResult.Error("Invalid CVV format")
            }

            val snapshot = cardRef.document(cardId).get().await()

            if (!snapshot.exists()) {
                return NetworkResult.Error("Card not found")
            }

            val storedCvv = snapshot.getString("cvv")
            return if (storedCvv == cleanCvv) {
                NetworkResult.Success("")
            } else {
                NetworkResult.Error("Invalid CVV")
            }


        } catch (e: Exception) {
            NetworkResult.Error(e.message)
        }
    }





    suspend fun sendOrderToUserAndSeller(paymentType:String,order: Order): NetworkResult<Unit>{
        return try {

            val updatedItems = order.items.map { it.copy(orderedTime = System.currentTimeMillis()) }
            val updatedOrder = order.copy(paymentType = paymentType, userId = userId, items = updatedItems, timestamp = System.currentTimeMillis())

            val batch = db.batch()

           val globalOrderRef =  db.collection(ORDERS).document(order.orderId)
            batch.set(globalOrderRef, updatedOrder)

            val userOrderRef = db.collection(USERS)
                .document(userId)
                .collection(ORDERS)
                .document(order.orderId)
            batch.set(userOrderRef, updatedOrder)


            val itemsGroupedBySeller = updatedOrder.items.groupBy { it.product.partnerId }

            itemsGroupedBySeller.forEach {(sellerId,sellerItems) ->
               val sellerOrder = updatedOrder.copy(items = sellerItems)
                val sellerOrderRef = db.collection(PARTNERS)
                    .document(sellerId)
                    .collection(ORDERS)
                    .document(order.orderId)
                batch.set(sellerOrderRef, sellerOrder)

            }

            order.items.forEach { item ->
                cartRepoImpl.deleteItemFromCart(item)
            }

            batch.commit().await()

            NetworkResult.Success(Unit)
        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }


    suspend fun fetchPaymentHeader(): NetworkResult<List<PaymentMethodItem.Header>>{
        return try {

            val headerSnapshot = db.collectionGroup(PAYMENTS).get().await()

            if (headerSnapshot.isEmpty){
                return NetworkResult.Success(emptyList())
            }

            val headers = headerSnapshot.toObjects(PaymentMethodItem.Header::class.java).sortedBy { it.id }
            NetworkResult.Success(headers)

        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }

    }


}