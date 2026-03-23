package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.uievents.CartUiEvents
import com.example.grocerly.model.uistate.CartUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CartViewModel @Inject constructor (application: Application,private val cartRepoImpl: CartRepoImpl):  AndroidViewModel(application) {


    private val _cartUiState = MutableStateFlow<NetworkResult<CartUiState>>(NetworkResult.UnSpecified())
    val cartUiState get() = _cartUiState.asStateFlow()

    private val _cartUiEvents = Channel<CartUiEvents>()
    val cartUiEvents = _cartUiEvents.receiveAsFlow()


    private val quantityUpdateJobs = mutableMapOf<String,Job>()

    init {
        fetchCartData()
    }

    private fun fetchCartData() {
       viewModelScope.launch {
           combine(
               cartRepoImpl.fetchAllCartItems(),
               cartRepoImpl.fetchTotalAmountFromCart()
           ) { cartResult, totalResult ->
               when {
                   cartResult is NetworkResult.Error -> NetworkResult.Error(cartResult.message ?: "Failed to load cart")
                   totalResult is NetworkResult.Error -> NetworkResult.Error(totalResult.message ?: "Failed to load total")
                   cartResult is NetworkResult.Loading || totalResult is NetworkResult.Loading -> NetworkResult.Loading()
                   cartResult is NetworkResult.Success && totalResult is NetworkResult.Success -> {
                       NetworkResult.Success(CartUiState(cartResult.data, totalResult.data))
                   }
                   else -> NetworkResult.UnSpecified()
               }
           }.collectLatest { combinedState ->
               _cartUiState.value = combinedState
           }
       }
    }


    fun addProductIntoCartFirebase(cartProduct: CartProduct) = executeNetworkAction {
        cartRepoImpl.addProductToCart(cartProduct)
    }



    fun deleteCartItem(cartProduct: CartProduct) = executeNetworkAction {
        cartRepoImpl.deleteItemFromCart(cartProduct)
    }



    fun updateQuantity(cartProduct: CartProduct){
        val productId = cartProduct.product.productId

        quantityUpdateJobs[productId]?.cancel()

        quantityUpdateJobs[productId] = executeNetworkAction {
            cartRepoImpl.updateQuantity(cartProduct)
        }
    }


    private fun executeNetworkAction(action: suspend () -> NetworkResult<Unit>): Job {
        return viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val result = action()
                if (result is NetworkResult.Error) {
                    _cartUiEvents.send(CartUiEvents.ShowMessage(result.message ?: "Unknown Error Occurred"))
                }
            } else {
                _cartUiEvents.send(CartUiEvents.ShowMessage("Enable Wifi or Mobile Data"))
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        quantityUpdateJobs.clear()
    }


}