package com.example.grocerly.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.model.Order
import com.example.grocerly.room.dao.PendingOrderDao
import com.example.grocerly.utils.Constants.EARNED_COUPONS
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await


@HiltWorker
class PlaceOrderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingOrderDao: PendingOrderDao,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val cartRepoImpl: CartRepoImpl
): CoroutineWorker(appContext, workerParams) {


    override suspend fun doWork(): Result {

        val pendingOrderId = inputData.getInt("room_order_id", -1)
        if (pendingOrderId == -1) return Result.failure()

        val userId = auth.currentUser?.uid ?: return Result.failure()


        val pendingEntity = pendingOrderDao.getPendingOrderById(pendingOrderId)
            ?: return Result.failure()

        return try {
            val order = Gson().fromJson(pendingEntity.orderJson, Order::class.java)

            val updatedItems = order.items.map { it.copy(orderedTime = System.currentTimeMillis()) }
            val updatedOrder = order.copy(
                paymentType = pendingEntity.paymentType,
                userId = userId,
                items = updatedItems,
                timestamp = System.currentTimeMillis()
            )

            val batch = db.batch()

            val globalOrderRef = db.collection(ORDERS).document(order.orderId)
            batch.set(globalOrderRef, updatedOrder)

            val userOrderRef = db.collection(USERS).document(userId).collection(ORDERS).document(order.orderId)
            batch.set(userOrderRef, updatedOrder)

            val itemsGroupedBySeller = updatedOrder.items.groupBy { it.product.partnerId }
            itemsGroupedBySeller.forEach { (sellerId, sellerItems) ->
                val sellerOrder = updatedOrder.copy(items = sellerItems)
                val sellerOrderRef = db.collection(PARTNERS).document(sellerId).collection(ORDERS).document(order.orderId)
                batch.set(sellerOrderRef, sellerOrder)
            }

            if (pendingEntity.appliedCouponId != null) {
                val couponRef = db.collection(USERS).document(userId).collection(EARNED_COUPONS).document(pendingEntity.appliedCouponId)
                batch.delete(couponRef)
            }


            batch.commit().await()

            order.items.forEach { cartRepoImpl.deleteItemFromCart(it) }

            pendingOrderDao.deletePendingOrder(pendingOrderId)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }

    }


}