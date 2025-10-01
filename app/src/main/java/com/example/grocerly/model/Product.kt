package com.example.grocerly.model

import android.os.Parcelable
import com.example.grocerly.utils.PackUp
import com.example.grocerly.utils.ProductCategory
import com.example.grocerly.utils.QuantityType
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Product(
    val productId:String="",
    val partnerId:String="",
    val image:String?="",
    val itemName:String="",
    val itemPrice:Int?=null,
    val itemOriginalPrice: Int?=0,
    val category: ProductCategory= ProductCategory.selectcatgory,
    val itemRating:Double ?= 5.0,
    val totalRating:Int ?= 0,

    val isFavourite: Boolean=false,
    val isInCart: Boolean=false,

    val isEnabled: Boolean?=true,
    val maxQuantity: Int?=1,
    val quantityType: QuantityType = QuantityType.selectQuantity,
    val packUpTime: PackUp = PackUp.selectTime
): Parcelable
