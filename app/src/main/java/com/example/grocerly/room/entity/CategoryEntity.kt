package com.example.grocerly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.grocerly.utils.Constants.CATEGORY_TABLE
import com.example.grocerly.utils.ProductCategory

@Entity(tableName = CATEGORY_TABLE)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: ProductCategory,
    val imageUrl: String
)