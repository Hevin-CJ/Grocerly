package com.example.grocerly.model.uistate

import android.os.Bundle
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Category
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.WishItem
import com.example.grocerly.room.entity.CategoryEntity
import com.example.grocerly.room.entity.OfferEntity

data class HomeUiState (
    val isLoading: Boolean = false,
    val products: List<ParentCategoryItem> = emptyList(),
    val categoryItems: List<Category> = emptyList(),
    val cartItems: List<CartProduct> = emptyList(),
    val homeAddress: String = "",
    val localCategories: List<CategoryEntity> = emptyList(),
    val localOffers: List<OfferItem> = emptyList(),
    val favouriteItems: List<FavouriteItem> = emptyList(),
    val wishListItems: List<WishItem> = emptyList(),
)

