package com.example.grocerly.cart.domain.model

import com.example.grocerly.model.CancellationInfo
import com.example.grocerly.utils.OrderStatus
import com.example.grocerly.utils.PackUp
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.utils.QuantityType

data class DomainProduct(
    val productId: String = "",
    val partnerId: String = "",
    val image: String? = "",
    val itemName: String = "",
    val itemPrice: Int? = null,
    val itemOriginalPrice: Int? = 0,
    val category: ProductCategory = ProductCategory.selectcatgory,
    val itemRating: Double? = 5.0,
    val totalRating: Int? = 0,
    val searchKeywords: List<String> = listOf(),
    val isFavourite: Boolean = false,
    val isInCart: Boolean = false,
    val isEnabled: Boolean? = true,
    val maxQuantity: Int? = 1,
    val quantityType: QuantityType = QuantityType.selectQuantity,
    val packUpTime: PackUp = PackUp.selectTime
)

data class CartItem(
    val product: DomainProduct = DomainProduct(),
    var quantity: Int = 1,
    val orderedTime: Long = 0L,
    val deliveryDate: String = "",
    var deliveredDate: Long = 0L,
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val cancellationInfo: CancellationInfo = CancellationInfo(),
    val isRewardClaimed: Boolean = false
)
