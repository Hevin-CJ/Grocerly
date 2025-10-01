package com.example.grocerly.interfaces

import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem

interface ChildCategoryListener {
    fun addProductToCart(cartProduct: CartProduct)
    fun addProductToFavourites(favouriteItem: FavouriteItem)
}