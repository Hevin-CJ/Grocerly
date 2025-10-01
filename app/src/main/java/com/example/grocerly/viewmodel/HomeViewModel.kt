package com.example.grocerly.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.local.CategoryLocalRepoImpl
import com.example.grocerly.Repository.local.OfferLocalRepoImpl
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.HomeRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Category
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val homeRepoImpl: HomeRepoImpl,
    private val cartRepoImpl: CartRepoImpl,
    private val categoryLocalRepoImpl: CategoryLocalRepoImpl,
    private val offerLocalRepoImpl: OfferLocalRepoImpl
) : AndroidViewModel(application) {

    val getOffers = offerLocalRepoImpl.getOffers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _products = MutableStateFlow<NetworkResult<List<ParentCategoryItem>>>(NetworkResult.UnSpecified())
    val products: StateFlow<NetworkResult<List<ParentCategoryItem>>> get() = _products.asStateFlow()

    private val _categoryItems = MutableStateFlow<NetworkResult<List<Category>>>(NetworkResult.UnSpecified())
    val categoryItems: StateFlow<NetworkResult<List<Category>>> get() = _categoryItems.asStateFlow()

    private val _offers = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.UnSpecified())
    val offers: StateFlow<NetworkResult<Unit>>get() = _offers.asStateFlow()

    private val _cartItems =
        MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    val cartItems: StateFlow<NetworkResult<List<CartProduct>>> get() = _cartItems.asStateFlow()

    private val _homeAddress = MutableStateFlow<NetworkResult<String>>(NetworkResult.UnSpecified())
    val homeAddress: StateFlow<NetworkResult<String>> get() = _homeAddress.asStateFlow()

    val categories  = categoryLocalRepoImpl.getCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        fetchProductFromFirebase()
        fetchOffersFromFirebase()
        fetchCartItems()
        fetchHomeAddress()
        fetchCategories()
    }

    fun fetchProductFromFirebase() {
        viewModelScope.launch {
            homeRepoImpl.fetchProductFromFirebase().collectLatest {
                _products.value = it

            }
        }
    }


    fun fetchOffersFromFirebase() {
        viewModelScope.launch { handleNetworkResultOffersFetched() }
    }

    fun fetchCartItems() {
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest {
                _cartItems.emit(it)
            }
        }
    }

    fun fetchHomeAddress() {
        viewModelScope.launch {
            getHomeAddress()
        }
    }

    fun fetchCategories(){
        viewModelScope.launch {
            getCategoriesFrom()
        }
    }

    private suspend fun handleNetworkResultOffersFetched() {
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            _offers.emit(NetworkResult.Loading())
            val fetchedOffers = homeRepoImpl.getOffersFromFirebase()
            _offers.emit(fetchedOffers)
        } else {
            _offers.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }

    private suspend fun getHomeAddress() {
        val fetchedAddress = homeRepoImpl.getCityAndState()
        _homeAddress.emit(fetchedAddress)
    }

    private suspend fun getCategoriesFrom(){
        val fetchedCategories = homeRepoImpl.getCategoriesFromFirebase()
        _categoryItems.emit(fetchedCategories)
        Log.d("fetchedCategories",fetchedCategories.toString())
    }

}