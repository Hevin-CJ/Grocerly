package com.example.grocerly.room.convertors

import androidx.room.TypeConverter
import com.example.grocerly.utils.ProductCategory

class CategoryConvertor {

    @TypeConverter
    fun fromProductCategory(category: ProductCategory): String {
        return category.displayName
    }

    @TypeConverter
    fun toProductCategory(name: String): ProductCategory {
        return ProductCategory.fromString(name)
    }
}