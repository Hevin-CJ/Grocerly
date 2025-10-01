package com.example.grocerly.model

import kotlin.hashCode

data class ParentCategoryItem(
    val categoryName: String,
    val childCategoryItems: List<Product>

)