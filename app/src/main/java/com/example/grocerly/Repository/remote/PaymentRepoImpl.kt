package com.example.grocerly.Repository.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.example.grocerly.model.Order
import com.example.grocerly.room.dao.PendingOrderDao
import com.example.grocerly.room.entity.PendingOrderEntity
import com.example.grocerly.utils.Constants.EARNED_COUPONS
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.PAYMENTS
import com.example.grocerly.utils.Constants.SAVED_CARDS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.PaymentMethodItem
import com.example.grocerly.worker.PlaceOrderWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.emptyList


@ActivityRetainedScoped
class PaymentRepoImpl @Inject constructor( private val auth: FirebaseAuth,private val db: FirebaseFirestore,private val cartRepoImpl: CartRepoImpl,private val pendingOrderDao: PendingOrderDao,@ApplicationContext private val context: Context) {

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





    suspend fun sendOrderToUserAndSeller(paymentType:String,order: Order,appliedCouponId: String?=null): NetworkResult<Unit>{
        return try {

            val entity = PendingOrderEntity(
                orderJson = Gson().toJson(order),
                paymentType = paymentType,
                appliedCouponId = appliedCouponId
            )
            val roomId = pendingOrderDao.insertPendingOrder(entity).toInt()


            val workData = workDataOf("room_order_id" to roomId)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val orderWorkRequest = OneTimeWorkRequestBuilder<PlaceOrderWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "place_order_${order.orderId}",
                ExistingWorkPolicy.REPLACE,
                orderWorkRequest
            )


            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to queue order")
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