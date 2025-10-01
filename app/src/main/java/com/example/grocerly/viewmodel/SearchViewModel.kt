package com.example.grocerly.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.Repository.remote.SearchRepoImpl
import com.example.grocerly.fragments.Favourites
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val searchRepoImpl: SearchRepoImpl,private val cartRepoImpl: CartRepoImpl,private val favouritesRepoImpl: FavouritesRepoImpl,application: Application): AndroidViewModel(application) {

    private val _searchItem = MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.UnSpecified())
    val searchItem: StateFlow<NetworkResult<List<Product>>> get() = _searchItem.asStateFlow()

    private val _cartItems = MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    val cartItems get() = _cartItems.asStateFlow()

    private val _favouriteItems = MutableStateFlow<NetworkResult<List<FavouriteItem>>>(NetworkResult.UnSpecified())
    val favouriteItems get() = _favouriteItems.asStateFlow()

    init {
        getCartItems()
        getFavouriteItems()
    }

    fun addFavouriteToFirebase(favourites: FavouriteItem){
        viewModelScope.launch {
            favouritesRepoImpl.addToFavouritesFirebase(favourites)
        }
    }




    fun searchItemsInFirebase(query: String){
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                searchRepoImpl.searchProduct(query).collectLatest {
                    _searchItem.emit(it)
                    Log.d("searchItem",it.data.toString())
                }
            } else {
                _searchItem.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
            }
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

}