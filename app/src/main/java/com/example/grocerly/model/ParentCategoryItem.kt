package com.example.grocerly.model

import com.example.grocerly.utils.ProductCategory

data class ParentCategoryItem(
    val category: ProductCategory = ProductCategory.selectcatgory,
    val categoryName: String,
    val childCategoryItems: List<Product>
)
