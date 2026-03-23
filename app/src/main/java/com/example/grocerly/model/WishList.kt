package com.example.grocerly.model

data class WishList (
    val id:String,
    val wishListName: String,
    val items: List<WishItem>
)