package com.example.grocerly.model

import java.util.Locale

data class Account(
    val userId:String="",
    val firstName:String="",
    val lastName:String="",
    val email:String="",
    val imageUrl:String = " ",
    val countryCode: String="",
    val phoneNumber: String = " "
)
