package com.example.grocerly.model

import android.os.Parcelable

import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize


@Parcelize
data class OfferItem(
    val offerId: String = "",
    val offerImage: String = "",
    val offerBgColor: String = "",
    val buttonText: String = "",
    val buttonBgColor: String = "",
    val buttonTxtColor: String = "",
    val descriptionText: String = "",
    val descriptionTextColor: String = "",
    @set:PropertyName("productId")
    @get:PropertyName("productId")
    var productId: String = "",
    val partnerId: String = ""
) : Parcelable