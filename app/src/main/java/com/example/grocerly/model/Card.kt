package com.example.grocerly.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Card(
    val cardId: String="",
    val holderName: String,
    val cardNumber: String,
    val expiryDate: ExpiryDate,
    val cvv: String,
    val cardType: String
):Parcelable{
    constructor():this("","","", ExpiryDate(0,0),"","")
}
