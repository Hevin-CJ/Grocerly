package com.example.grocerly.utils

import com.example.grocerly.model.WishItem

sealed interface WishListAction {
    data class AddItemToCart(val wishItem: WishItem) : WishListAction
    data class DeleteItemFromWishList(val wishItem: WishItem) : WishListAction
}
