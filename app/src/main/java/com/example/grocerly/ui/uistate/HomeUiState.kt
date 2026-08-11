package com.example.grocerly.ui.uistate

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
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<ParentCategoryItem> = emptyList(),
    val categoryItems: List<Category> = emptyList(),
    val homeAddress: String = "",
    val localOffers: List<OfferItem> = emptyList(),
    val cartProductIds: Set<String> = emptySet(),
    val favouriteProductIds: Set<String> = emptySet(),
    val wishListProductIds: Set<String> = emptySet()
)

