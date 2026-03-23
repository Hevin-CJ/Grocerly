package com.example.grocerly.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.grocerly.Repository.remote.RewardRepoImpl
import com.example.grocerly.model.Order
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class CouponWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workParams: WorkerParameters,
    private val rewardRepoImpl: RewardRepoImpl,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
): CoroutineWorker(appContext, workParams) {
    override suspend fun doWork(): Result {

        val orderId = inputData.getString("orderId") ?: return Result.failure()
        val userId = auth.currentUser?.uid ?: return Result.failure()

        return try {
            val orderSnapshot = db.collection(USERS).document(userId)
                .collection(ORDERS).document(orderId)
                .get().await()


            val order = orderSnapshot.toObject(Order::class.java) ?: return Result.failure()

            val result = rewardRepoImpl.generateCouponsForOrder(orderId, order.items)

            when (result) {
                is NetworkResult.Success -> Result.success()
                is NetworkResult.Error -> Result.retry()
                else -> Result.failure()
            }

        }catch (e: Exception){
            Result.retry()
        }

    }

}