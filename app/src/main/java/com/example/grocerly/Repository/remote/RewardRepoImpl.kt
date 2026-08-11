package com.example.grocerly.Repository.remote

import com.example.grocerly.model.CancellationInfo
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.EarnedCoupon
import com.example.grocerly.model.Order
import com.example.grocerly.model.PartnerCouponRule
import com.example.grocerly.utils.CancellationStatus
import com.example.grocerly.utils.Constants.COUPONS
import com.example.grocerly.utils.Constants.EARNED_COUPONS
import com.example.grocerly.utils.Constants.ORDERS
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.OrderStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardRepoImpl @Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {

    suspend fun generateCouponsForOrder(
        orderId: String,
        items: List<CartProduct>
    ): NetworkResult<Unit> {
        val userId = auth.currentUser?.uid ?: return NetworkResult.Error("Unauthorized Activity")

        val validItems = items.filter { it.cancellationInfo.cancellationStatus != CancellationStatus.Cancelled }
        if (validItems.isEmpty() || !validItems.all { it.orderStatus == OrderStatus.DELIVERED }) {
            return NetworkResult.Success(Unit)
        }

        val unclaimedDeliveredItems = validItems.filter { !it.isRewardClaimed }
        if (unclaimedDeliveredItems.isEmpty()) {
            return NetworkResult.Success(Unit)
        }

        val partnerSpendMap = unclaimedDeliveredItems
            .groupBy { it.product.partnerId }
            .mapValues { (_, list) -> list.sumOf { (it.product.itemPrice ?: 0) * it.quantity } }

        val orderRef = db.collection(USERS).document(userId).collection(ORDERS).document(orderId)
        val globalOrderRef = db.collection(ORDERS).document(orderId)
        val userCouponsRef = db.collection(USERS).document(userId).collection(EARNED_COUPONS)

        return try {
            val partnerRulesMap = mutableMapOf<String, List<PartnerCouponRule>>()

            for (partnerId in partnerSpendMap.keys) {
                val rulesSnapshot = db.collection(PARTNERS).document(partnerId)
                    .collection(COUPONS)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                partnerRulesMap[partnerId] = rulesSnapshot.documents.mapNotNull {
                    it.toObject(PartnerCouponRule::class.java)
                }
            }

            db.runTransaction { transaction ->
                val orderSnapshot = transaction.get(orderRef)
                val currentOrder = orderSnapshot.toObject(Order::class.java)
                    ?: return@runTransaction

                val txUnclaimedItems = currentOrder.items.filter {
                    it.orderStatus == OrderStatus.DELIVERED &&
                            it.cancellationInfo.cancellationStatus != CancellationStatus.Cancelled &&
                            !it.isRewardClaimed
                }

                if (txUnclaimedItems.isEmpty()) return@runTransaction

                val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

                for ((partnerId, spend) in partnerSpendMap) {
                    val applicableRule = partnerRulesMap[partnerId]
                        ?.filter { spend >= it.minimumSpendToEarn }
                        ?.maxByOrNull { it.minimumSpendToEarn }

                    if (applicableRule != null) {
                        val couponRef = userCouponsRef.document()
                        val expiry = System.currentTimeMillis() + (applicableRule.validityInDays * 24L * 60 * 60 * 1000)
                        val randomString = (1..7).map { charset.random() }.joinToString("")
                        val uniqueCode = randomString.uppercase()

                        val newCoupon = EarnedCoupon(
                            couponId = couponRef.id,
                            code = uniqueCode,
                            partnerId = partnerId,
                            discountAmount = applicableRule.discountAmountToGive,
                            minOrderValue = applicableRule.minOrderValueForNextPurchase,
                            expiryTimestamp = expiry,
                            isCouponUsed = false
                        )

                        transaction.set(couponRef, newCoupon)
                    }
                }

                val updatedItems = currentOrder.items.map { item ->
                    if (txUnclaimedItems.any { it.product.productId == item.product.productId }) {
                        item.copy(isRewardClaimed = true)
                    } else {
                        item
                    }
                }

                val updatedOrder = currentOrder.copy(items = updatedItems)

                transaction.set(orderRef, updatedOrder.toMap(), SetOptions.merge())
                transaction.set(globalOrderRef, updatedOrder.toMap(), SetOptions.merge())
            }.await()

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Reward generation failed")
        }
    }





    private fun Order.toMap(): Map<String, Any> {
        return mapOf(
            "orderId" to orderId,
            "items" to items.map { it.toMap() }
        )
    }

    private fun CartProduct.toMap(): Map<String, Any?> {
        return mapOf(
            "product" to product,
            "quantity" to quantity,
            "orderedTime" to orderedTime,
            "deliveryDate" to deliveryDate,
            "deliveredDate" to deliveredDate,
            "orderStatus" to  orderStatus,
            "cancellationInfo" to cancellationInfo.toMap(),
            "rewardClaimed" to isRewardClaimed // Crucial step
        )
    }

    private fun CancellationInfo.toMap(): Map<String, Any?> {
        return mapOf(
            "cancellationStatus" to cancellationStatus.name,
            "cancelledBy" to cancelledBy.name,
            "cancelledAt" to cancelledAt,
            "reason" to reason
        )
    }
}