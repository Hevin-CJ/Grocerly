package com.example.grocerly.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.grocerly.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profile_entity_table LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?>

}