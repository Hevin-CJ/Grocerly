package com.example.grocerly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.grocerly.utils.Constants.PROFILE_TABLE

@Entity(tableName = PROFILE_TABLE)
data class ProfileEntity(
    @PrimaryKey
    val userId:String,
    val firstName:String,
    val lastName:String,
    val email:String,
    val imageUrl:String,
    val countryCode: String,
    val phoneNumber: String
)
