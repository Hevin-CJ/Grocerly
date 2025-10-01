package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.SavedAddressRepoImpl
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CheckoutViewModel @Inject constructor(private val cartRepoImpl: CartRepoImpl,private val addressRepoImpl: SavedAddressRepoImpl,application: Application): AndroidViewModel(application) {

    private val _defaultAddress = MutableStateFlow<NetworkResult<Address?>>(NetworkResult.UnSpecified())
    val  defaultAddress: StateFlow<NetworkResult<Address?>> get() = _defaultAddress.asStateFlow()

    private val _cartItems = MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    val  cartItems: StateFlow<NetworkResult<List<CartProduct>>> get() = _cartItems.asStateFlow()

    private val _updateQuantityState = Channel<NetworkResult<Unit>>()
    val  updateQuantityState: Flow<NetworkResult<Unit>> get() = _updateQuantityState.receiveAsFlow()

    private val _deletedItems = MutableSharedFlow<NetworkResult<Unit>>()
    val deletedItems: Flow<NetworkResult<Unit>> get() = _deletedItems.asSharedFlow()

    private val _priceBreakdown = MutableStateFlow< NetworkResult<Map<String, Int>>>(NetworkResult.UnSpecified())
    val priceBreakdown: StateFlow<NetworkResult<Map<String,Int>>> = _priceBreakdown.asStateFlow()

    private val _savedAddresses = MutableStateFlow<NetworkResult<List<Address>>>(NetworkResult.UnSpecified())
    val  savedAddresses: StateFlow<NetworkResult<List<Address>>> get() = _savedAddresses.asStateFlow()

    private val quantityUpdateJobs = mutableMapOf<String,Job>()

     val emptyDefaultAddress: Flow<Boolean> = defaultAddress.map { it ->
        when(it){
            is NetworkResult.Error<*> -> false
            is NetworkResult.Loading<*> -> false
            is NetworkResult.Success<*> -> {
                val deliveryAddress = it.data?.deliveryAddress
                deliveryAddress?.isEmpty() ?: true
            }
            is NetworkResult.UnSpecified<*> -> false
        }
    }


    init {
        getDefaultAddress()
        fetchCartItems()
    }

    fun getDefaultAddress(){
        viewModelScope.launch {
            addressRepoImpl.getDefaultAddressFromDb().collectLatest {
                _defaultAddress.emit(it)
            }
        }
    }

    fun deleteCartItem(cartProduct: CartProduct){
        viewModelScope.launch { handleNetworkResultDeleteItem(cartProduct) }
    }

    fun fetchCartItems(){
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest {
                _cartItems.emit(it)
            }
        }
    }

    fun fetchAddress(){
        viewModelScope.launch {
           addressRepoImpl.getAllAddressFromDb().collectLatest {
               _savedAddresses.emit(it)
           }
        }
    }

    fun updateQuantity(cartProduct: CartProduct){
        val productId = cartProduct.product.productId
        quantityUpdateJobs[productId]?.cancel()

       val job =  viewModelScope.launch {
            updateItemQuantity(cartProduct)
        }
        quantityUpdateJobs[productId] = job
    }



    fun calculateTotalPrice(cartItems: List<CartProduct>){
        viewModelScope.launch {
            calculateTotalPriceFromDb(cartItems)
        }
    }

    fun deleteAddress(address: Address){
        viewModelScope.launch {
            deleteSavedAddress(address)
        }
    }

    fun setAsDefault(address: Address){
        viewModelScope.launch {
            setAsDefaultAddress(address)
        }
    }

    private suspend fun deleteSavedAddress(address: Address){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val deletedAddress = addressRepoImpl.deleteAddressFromFirebase(address)
        }else{

        }
    }

    private suspend fun setAsDefaultAddress(address: Address){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val defaultAddress = addressRepoImpl.setAsDefaultAddressInDb(address)
        }else{

        }
    }

    private suspend fun updateItemQuantity(cartProduct: CartProduct){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val updatedCart = cartRepoImpl.updateQuantity(cartProduct)
           _updateQuantityState.send(updatedCart)
        }else{
            _updateQuantityState.send(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }

    private suspend fun handleNetworkResultDeleteItem(cartProduct: CartProduct){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            _deletedItems.emit(NetworkResult.Loading())
            val deletedItem = cartRepoImpl.deleteItemFromCart(cartProduct)
            _deletedItems.emit(deletedItem)
        }else{
            _deletedItems.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }





    private suspend fun calculateTotalPriceFromDb(cartItems: List<CartProduct>,couponAmount: Int = 0) {

       if (NetworkUtils.isNetworkAvailable(getApplication())){
          cartRepoImpl.fetchTotalPriceFromDb(cartItems,couponAmount).collectLatest {
              _priceBreakdown.emit(it)
          }
       }else{
           _priceBreakdown.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
       }

    }

}