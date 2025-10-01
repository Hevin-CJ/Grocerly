package com.example.grocerly.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.CustomSearchRepoImpl
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.Repository.remote.SearchRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomSearchViewModel @Inject constructor(private val customSearchRepoImpl: CustomSearchRepoImpl,private val cartRepoImpl: CartRepoImpl,private val favouritesRepoImpl: FavouritesRepoImpl,private val searchRepoImpl: SearchRepoImpl,application: Application): AndroidViewModel(application) {


    private val _searchedCategories = MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.UnSpecified())
    val searchedCategories = _searchedCategories.asStateFlow()

    private val _cartItems = MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    val cartItems = _cartItems.asStateFlow()

    private val _favouriteItems = MutableStateFlow<NetworkResult<List<FavouriteItem>>>(NetworkResult.UnSpecified())
    val favouriteItems = _favouriteItems.asStateFlow()

    private val _searchItem = MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.UnSpecified())
    val searchItem = _searchItem.asStateFlow()



    init {
        getCartItems()
        getFavouriteItems()
    }

    fun searchCategory(categoryName: ProductCategory){
        viewModelScope.launch {
            searchByCategory(categoryName)
        }
    }

    fun addProductIntoCartFirebase(cartProduct: CartProduct){
        viewModelScope.launch {
            cartRepoImpl.addProductToCart(cartProduct)
        }
    }

    fun addFavouriteIntoCartFirebase(favouriteItem: FavouriteItem){
        viewModelScope.launch {
            favouritesRepoImpl.addToFavouritesFirebase(favouriteItem)
        }
    }

    fun getCartItems(){
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest {
                _cartItems.emit(it)
            }
        }
    }

    fun getFavouriteItems(){
        viewModelScope.launch {
            favouritesRepoImpl.fetchAllFavourites().collectLatest {
                _favouriteItems.emit(it)
            }
        }
    }


    fun searchItemsInFirebase(query: String) {
        viewModelScope.launch {
            searchRepoImpl.searchProduct(query).debounce(400L).distinctUntilChanged().collectLatest { result ->
                _searchItem.emit(result)
                Log.d("searchItem", result.data.toString())
            }
        }
    }

    private suspend fun searchByCategory(categoryName: ProductCategory) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            customSearchRepoImpl.searchByCategory(categoryName).collectLatest {
                _searchedCategories.emit(it)
            }
        }else{
            _searchedCategories.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }

}