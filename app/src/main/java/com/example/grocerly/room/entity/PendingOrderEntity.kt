package com.example.grocerly.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val orderJson: String,
    val paymentType: String,
    val appliedCouponId: String?,
    @ColumnInfo(defaultValue = "0") val retryCount: Int = 0
)