package com.example.grocerly.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.grocerly.model.Category
import com.example.grocerly.room.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM CATEGORY_ENTITY_TABLE ORDER BY id")
     fun getCategories(): Flow<List<CategoryEntity>>

}