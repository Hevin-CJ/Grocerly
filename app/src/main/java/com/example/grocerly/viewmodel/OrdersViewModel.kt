package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.Repository.remote.HomeRepoImpl
import com.example.grocerly.Repository.remote.OrderRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Order
import com.example.grocerly.model.Product
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.OrderStatus
import com.example.grocerly.utils.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OrdersViewModel @Inject constructor(application: Application,private val orderRepoImpl: OrderRepoImpl,private val cartRepoImpl: CartRepoImpl,private val favouritesRepoImpl: FavouritesRepoImpl,private val homeRepoImpl: HomeRepoImpl): AndroidViewModel(application) {

    private val _orders =  MutableStateFlow<NetworkResult<List<Order>>>(NetworkResult.UnSpecified())
    val orders = _orders.asStateFlow()

    private val _Products =  MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.UnSpecified())
    val Products = _Products.asStateFlow()

    private val _favourites =  MutableStateFlow<NetworkResult<List<FavouriteItem>>>(NetworkResult.UnSpecified())
    val favourites = _favourites.asStateFlow()

    private val _cartItems =  MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    val cartItems = _cartItems.asStateFlow()

    private val _orderStatus =  MutableStateFlow<NetworkResult<CartProduct>>(NetworkResult.UnSpecified())
    val orderStatus = _orderStatus.asStateFlow()

    private val _cancelState =  Channel<NetworkResult<Unit>>()
    val cancelState = _cancelState.receiveAsFlow()

    init {
        fetchFavourites()
        fetchCartItems()
    }


    fun fetchAllOrders(){
        viewModelScope.launch {
            fetchAllOrderFromFirebase()
        }
    }

    fun insertCartProduct(cartProduct: CartProduct){
        viewModelScope.launch {
            cartRepoImpl.addProductToCart(cartProduct)
        }
    }

    fun insertProductIntoFavourites(favouriteItem: FavouriteItem){
        viewModelScope.launch {
            favouritesRepoImpl.addToFavouritesFirebase(FavouriteItem(favouriteItem.favouriteId,favouriteItem.product))

        }
    }

    fun fetchOrderStatus(cartProduct: CartProduct,order: Order){
        viewModelScope.launch {
            orderRepoImpl.getOrderStatus(cartProduct,order).collectLatest {
                _orderStatus.emit(it)
            }
        }
    }

    fun fetchCartItems(){
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest {
                _cartItems.emit(it)
            }
        }
    }


    fun fetchProductByCategory(category: ProductCategory){
        viewModelScope.launch {
            homeRepoImpl.fetchByCategoryFromFirebase(category).collectLatest {
                _Products.emit(it)
            }
        }
    }

    fun fetchFavourites(){
        viewModelScope.launch {
            favouritesRepoImpl.fetchAllFavourites().collectLatest {
                _favourites.emit(it)
            }
        }
    }

    fun setCancelOrder(cartProduct: CartProduct,order: Order,reason: String){
        viewModelScope.launch {
            cancelOrder(cartProduct,order,reason)
        }
    }

    fun setOrderStatus(cartProduct: CartProduct,order: Order,orderStatus: OrderStatus){
        viewModelScope.launch {
            setOrderStatusInFb(cartProduct,order,orderStatus)
        }
    }



    fun clearAllData() {
        _orders.value = NetworkResult.UnSpecified()
        _favourites.value = NetworkResult.UnSpecified()
        _cartItems.value = NetworkResult.UnSpecified()
        _Products.value = NetworkResult.UnSpecified()
    }

    private suspend fun cancelOrder(cartProduct: CartProduct,order: Order,reason: String){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
           val cancelState =  orderRepoImpl.deleteItemFromOrders(cartProduct,order,reason)
            _cancelState.send(cancelState)
        }else{
            _cancelState.send(NetworkResult.Error("Enable wifi or Mobile data"))
        }
    }

    private suspend fun setOrderStatusInFb(cartProduct: CartProduct,order: Order,orderStatus: OrderStatus){
      if (NetworkUtils.isNetworkAvailable(getApplication())){
         val setStatus=  orderRepoImpl.setOrderStateInOrder(cartProduct,order,orderStatus)
          _cancelState.send(setStatus)
      }else{
          _cancelState.send(NetworkResult.Error("Enable wifi or Mobile data"))
      }
    }


    private suspend fun fetchAllOrderFromFirebase(){
       orderRepoImpl.getOrdersFromFirebase().collectLatest {
           _orders.emit(it)
       }
    }


    override fun onCleared() {
        super.onCleared()
        clearAllData()
    }


}