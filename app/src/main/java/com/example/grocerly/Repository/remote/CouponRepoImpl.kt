package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.model.EarnedCoupon
import com.example.grocerly.utils.Constants.EARNED_COUPONS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ActivityRetainedScoped
class CouponRepoImpl @Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {

    suspend fun validateCoupon(inputCode: String, partnerSpendMap: Map<String, Int>): NetworkResult<EarnedCoupon> {
        val userId = auth.currentUser?.uid ?: return NetworkResult.Error("Unauthorized Activity")
        val currentTime = System.currentTimeMillis()

        return try {
            val querySnapshot = db.collection(USERS).document(userId)
                .collection(EARNED_COUPONS)
                .whereEqualTo("code", inputCode.uppercase())
                .limit(1)
                .get().await()

            if (querySnapshot.isEmpty) return NetworkResult.Error("Invalid coupon code")

            val coupon = querySnapshot.documents.first().toObject(EarnedCoupon::class.java)
                ?: return NetworkResult.Error("Data parse error")

            val spendWithPartner = partnerSpendMap[coupon.partnerId] ?: 0

            when {
                coupon.isCouponUsed -> NetworkResult.Error("Coupon has already been used")
                coupon.expiryTimestamp < currentTime -> NetworkResult.Error("Coupon has expired")
                spendWithPartner < coupon.minOrderValue -> NetworkResult.Error("Add ₹${coupon.minOrderValue - spendWithPartner} more from this partner to use this code")
                else -> NetworkResult.Success(coupon)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Validation error")
        }
    }

    suspend fun markCouponAsUsed(couponId: String): NetworkResult<Unit> {
        val userId = auth.currentUser?.uid ?: return NetworkResult.Error("Unauthorized Activity")
        return try {
            db.collection(USERS).document(userId).collection(EARNED_COUPONS)
                .document(couponId).update("isUsed", true).await()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to mark used")
        }
    }


    fun fetchAllAvailableCoupons(): Flow<NetworkResult<List<EarnedCoupon>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(NetworkResult.Error("Unauthorized Activity"))
            close()
            return@callbackFlow
        }

        trySend(NetworkResult.Loading())

        val userCouponsRef = db.collection(USERS).document(userId).collection(EARNED_COUPONS)

        val listener = userCouponsRef
            .whereEqualTo("isCouponUsed",false)
            .addSnapshotListener { snapshot, exception ->

                if (exception != null) {
                    trySend(NetworkResult.Error(exception.message ?: "Failed to fetch coupons"))
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(NetworkResult.Success(emptyList()))
                    return@addSnapshotListener
                }

                val currentTime = System.currentTimeMillis()


                val validCoupons = snapshot.documents
                    .mapNotNull { it.toObject(EarnedCoupon::class.java) }
                    .filter { it.expiryTimestamp > currentTime }


                Log.d("couponlistgotrepo",validCoupons.toString())
                trySend(NetworkResult.Success(validCoupons))


            }

        awaitClose {
            listener.remove()
        }
    }
}