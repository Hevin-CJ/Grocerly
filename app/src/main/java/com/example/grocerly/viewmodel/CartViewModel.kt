package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.uistate.CartUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CartViewModel @Inject constructor (application: Application,private val cartRepoImpl: CartRepoImpl):  AndroidViewModel(application) {

    private val _addedCartItems = MutableSharedFlow<NetworkResult<Unit>>()
    val addedCartItems: Flow<NetworkResult<Unit>> get() = _addedCartItems.asSharedFlow()

    private val _deletedItems = MutableSharedFlow<NetworkResult<Unit>>()
    val deletedItems: Flow<NetworkResult<Unit>> get() = _deletedItems.asSharedFlow()

    private val _cartItems = MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    private val _totalAmount = MutableStateFlow<NetworkResult<Float>>(NetworkResult.UnSpecified())

    private val _updateQuantityState = Channel<NetworkResult<Unit>>()
    val  updateQuantityState: Flow<NetworkResult<Unit>> get() = _updateQuantityState.receiveAsFlow()

    private val _cartUiState = MutableStateFlow<NetworkResult<CartUiState>>(NetworkResult.UnSpecified())
    val cartUiState get() = _cartUiState.asStateFlow()

    private val quantityUpdateJobs = mutableMapOf<String,Job>()

    init {


        _cartItems.combine(_totalAmount){ cartItems,totalamount ->
            when{
                cartItems is NetworkResult.Success && totalamount is NetworkResult.Success ->{
                    _cartUiState.emit(   NetworkResult.Success(
                        CartUiState(
                            cartItems.data,
                            totalamount.data
                        )
                    ))
                }

                cartItems is NetworkResult.Error ->{
                   _cartItems.emit( NetworkResult.Error(cartItems.message ?: "Something Unusual happened \n Please try later...."))
                }

                totalamount is NetworkResult.Error -> {
                    _cartUiState.emit( NetworkResult.Error(totalamount.message ?: "Unknown Error"))
                }

                cartItems is NetworkResult.Loading || totalamount is NetworkResult.Loading -> {
                    _cartUiState.emit(NetworkResult.Loading())
                }

                else ->  _cartUiState.emit(NetworkResult.UnSpecified())
            }
        }.launchIn(viewModelScope)


        fetchCartItems()
        fetchTotalAmountFromCart()
    }


    fun addProductIntoCartFirebase(cartProduct: CartProduct){
        viewModelScope.launch { handleNetworkResultProductIntoCart(cartProduct) }
    }


    fun fetchCartItems(){
        viewModelScope.launch { handleNetworkResultFetchAllCartItems() }
    }

    fun deleteCartItem(cartProduct: CartProduct){
        viewModelScope.launch { handleNetworkResultDeleteItem(cartProduct) }
    }

    fun fetchTotalAmountFromCart(){
        viewModelScope.launch { handleNetworkResultFetchTotalAmount() }
    }



    private suspend fun handleNetworkResultFetchTotalAmount() {
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            _totalAmount.value = NetworkResult.Loading()
            cartRepoImpl.fetchTotalAmountFromCart().collectLatest { result ->
                _totalAmount.emit(result)
            }
        } else {
            _totalAmount.value = NetworkResult.Error("Enable Wifi or Mobile data")
        }
    }


    private suspend fun handleNetworkResultProductIntoCart(cartProduct: CartProduct) {
       if (NetworkUtils.isNetworkAvailable(getApplication())){
           _addedCartItems.emit(NetworkResult.Loading())

           val isAdded = cartRepoImpl.addProductToCart(cartProduct)

           _addedCartItems.emit(isAdded)
       }else{
           _addedCartItems.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
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

    private suspend fun updateItemQuantity(cartProduct: CartProduct){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val updatedCart = cartRepoImpl.updateQuantity(cartProduct)
            _updateQuantityState.send(updatedCart)
        }else{
            _updateQuantityState.send(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }




    private suspend fun handleNetworkResultFetchAllCartItems() {
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            _cartItems.emit(NetworkResult.Loading())
           cartRepoImpl.fetchAllCartItems().collectLatest {
                _cartItems.emit(it)
            }

        } else {
            _cartItems.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
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


    override fun onCleared() {
        super.onCleared()
        _updateQuantityState.close()
        quantityUpdateJobs.clear()
    }


}