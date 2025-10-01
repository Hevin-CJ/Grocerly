package com.example.grocerly.model

data class FavouriteItem(
    val favouriteId: String= "",
    val product: Product
){
    constructor():this("", Product())
}
