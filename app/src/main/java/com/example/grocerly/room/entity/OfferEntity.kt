package com.example.grocerly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.grocerly.utils.Constants.OFFER_TABLE

@Entity(tableName = OFFER_TABLE)
data class OfferEntity(
    @PrimaryKey
    val offerId:String,
    val offerImage: String,
    val offerBgColor: String,
    val buttonText: String,
    val buttonBgColor: String,
    val buttonTxtColor:String,
    val descriptionText: String,
    val descriptionTextColor: String
)