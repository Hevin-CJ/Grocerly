package com.example.grocerly.model

import android.os.Parcelable
import com.example.grocerly.utils.OrderStatus
import com.example.grocerly.utils.QuantityType
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class CartProduct(
    val product: Product = Product(),
    var quantity:Int = 1,
    val orderedTime: Long = 0L,
    val deliveryDate: String = "",
    var deliveredDate: Long = 0L,
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val cancellationInfo: CancellationInfo = CancellationInfo(),
    @get:PropertyName("rewardClaimed")
    @set:PropertyName("rewardClaimed")
    var isRewardClaimed: Boolean = false
): Parcelable