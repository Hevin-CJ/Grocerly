package com.example.grocerly.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.grocerly.room.entity.PendingOrderEntity

@Dao
interface PendingOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOrder(order: PendingOrderEntity): Long

    @Query("SELECT * FROM pending_orders WHERE id = :orderId")
    suspend fun getPendingOrderById(orderId: Int): PendingOrderEntity?

    @Query("DELETE FROM pending_orders WHERE id = :orderId")
    suspend fun deletePendingOrder(orderId: Int)
}