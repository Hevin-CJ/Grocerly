package com.example.grocerly.model

import android.os.Parcelable
import com.example.grocerly.utils.OrderStatus
import com.example.grocerly.utils.QuantityType
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartProduct(
    val product: Product = Product(),
    var quantity:Int = 1,
    val orderedTime: Long = 0L,
    val deliveryDate: String = "",
    var deliveredDate: Long = 0L,
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val cancellationInfo: CancellationInfo = CancellationInfo()
): Parcelable