package com.example.grocerly.room.dao

import android.icu.text.MessagePattern
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.grocerly.room.entity.OfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffer(offerEntity: List<OfferEntity>)

    @Query( "SELECT * FROM offer_entity_table")
    fun getOffers(): Flow<List<OfferEntity>>


}