package com.example.grocerly.model

import android.os.Parcelable

data class OfferItem(
    val offerId:String,
    val offerImage: String,
    val offerBgColor: String,
    val buttonText: String,
    val buttonBgColor: String,
    val buttonTxtColor:String,
    val descriptionText: String,
    val descriptionTextColor: String
){
    constructor():this("","","","","","","","")
}