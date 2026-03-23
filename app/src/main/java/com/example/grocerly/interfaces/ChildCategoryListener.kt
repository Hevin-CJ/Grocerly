package com.example.grocerly.interfaces

import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.WishItem

interface ChildCategoryListener {
    fun addProductToCart(cartProduct: CartProduct)
    fun addProductToFavourites(favouriteItem: FavouriteItem)

    fun addProductToWishList(wishItem: WishItem)
}