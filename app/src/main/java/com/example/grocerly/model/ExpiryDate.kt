package com.example.grocerly.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpiryDate (
    val month:Int,
    val year: Int
): Parcelable{
    constructor():this(0,0)
}