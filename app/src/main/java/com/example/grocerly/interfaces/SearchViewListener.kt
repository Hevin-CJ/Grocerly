package com.example.grocerly.interfaces

import com.example.grocerly.utils.ProductCategory

interface SearchViewListener {
    fun onItemClicked(category: ProductCategory)
}