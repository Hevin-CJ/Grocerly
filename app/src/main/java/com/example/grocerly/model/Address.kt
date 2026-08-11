package com.example.grocerly.model

import android.os.Parcelable
import com.example.grocerly.utils.AddressType
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Address(
    var addressId: String = "",
    var firstName: String = "",
    var phoneNumber: String = "",
    var alternateNumber: String = "",
    var state: String = "",
    var city: String = "",
    var deliveryAddress: String = "",
    var landMark: String = "",
    var pinCode: String = "",
    var addressType: String = "",
    var default: Boolean = false
): Parcelable
