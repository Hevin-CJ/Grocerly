package com.example.grocerly.utils

import com.example.grocerly.fragments.Menu
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product

sealed class MenuAction {
    data object SIGN_OUT: MenuAction()
    data object ORDERS: MenuAction()
    data object ASSISTANT: MenuAction()

    data class addToCart(val product: CartProduct): MenuAction()
    data class addToFavourites(val favouriteItem: FavouriteItem): MenuAction()
}