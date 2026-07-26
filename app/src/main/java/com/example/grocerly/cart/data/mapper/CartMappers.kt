package com.example.grocerly.cart.data.mapper

import com.example.grocerly.cart.domain.model.CartItem
import com.example.grocerly.cart.domain.model.DomainProduct
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Product

fun Product.toDomainModel(): DomainProduct {
    return DomainProduct(
        productId = this.productId,
        partnerId = this.partnerId,
        image = this.image,
        itemName = this.itemName,
        itemPrice = this.itemPrice,
        itemOriginalPrice = this.itemOriginalPrice,
        category = this.category,
        itemRating = this.itemRating,
        totalRating = this.totalRating,
        searchKeywords = this.searchKeywords,
        isFavourite = this.isFavourite,
        isInCart = this.isInCart,
        isEnabled = this.isEnabled,
        maxQuantity = this.maxQuantity,
        quantityType = this.quantityType,
        packUpTime = this.packUpTime
    )
}

fun DomainProduct.toDataModel(): Product {
    return Product(
        productId = this.productId,
        partnerId = this.partnerId,
        image = this.image,
        itemName = this.itemName,
        itemPrice = this.itemPrice,
        itemOriginalPrice = this.itemOriginalPrice,
        category = this.category,
        itemRating = this.itemRating,
        totalRating = this.totalRating,
        searchKeywords = this.searchKeywords,
        isFavourite = this.isFavourite,
        isInCart = this.isInCart,
        isEnabled = this.isEnabled,
        maxQuantity = this.maxQuantity,
        quantityType = this.quantityType,
        packUpTime = this.packUpTime
    )
}

fun CartProduct.toDomainModel(): CartItem {
    return CartItem(
        product = this.product.toDomainModel(),
        quantity = this.quantity,
        orderedTime = this.orderedTime,
        deliveryDate = this.deliveryDate,
        deliveredDate = this.deliveredDate,
        orderStatus = this.orderStatus,
        cancellationInfo = this.cancellationInfo,
        isRewardClaimed = this.isRewardClaimed
    )
}

fun CartItem.toDataModel(): CartProduct {
    return CartProduct(
        product = this.product.toDataModel(),
        quantity = this.quantity,
        orderedTime = this.orderedTime,
        deliveryDate = this.deliveryDate,
        deliveredDate = this.deliveredDate,
        orderStatus = this.orderStatus,
        cancellationInfo = this.cancellationInfo,
        isRewardClaimed = this.isRewardClaimed
    )
}
