package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.cart.data.mapper.toDataModel
import com.example.grocerly.cart.data.mapper.toDomainModel
import com.example.grocerly.cart.domain.usecase.AddProductToCartUseCase
import com.example.grocerly.cart.domain.usecase.DeleteProductFromCartUseCase
import com.example.grocerly.cart.domain.usecase.FetchCartItemsUseCase
import com.example.grocerly.cart.domain.usecase.FetchTotalCartAmountUseCase
import com.example.grocerly.cart.domain.usecase.UpdateCartQuantityUseCase
import com.example.grocerly.model.CartProduct
import com.example.grocerly.ui.uievents.CartUiEvents
import com.example.grocerly.ui.uistate.CartUiState
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    application: Application,
    private val fetchCartItemsUseCase: FetchCartItemsUseCase,
    private val fetchTotalCartAmountUseCase: FetchTotalCartAmountUseCase,
    private val addProductToCartUseCase: AddProductToCartUseCase,
    private val deleteProductFromCartUseCase: DeleteProductFromCartUseCase,
    private val updateCartQuantityUseCase: UpdateCartQuantityUseCase
) : AndroidViewModel(application) {

    private val _cartUiState = MutableStateFlow<CartUiState>(CartUiState())
    val cartUiState get() = _cartUiState.asStateFlow()

    private val _cartUiEvents = Channel<CartUiEvents>()
    val cartUiEvents = _cartUiEvents.receiveAsFlow()

    private val quantityUpdateJobs = mutableMapOf<String, Job>()

    init {
        observeCartData()
    }

    private fun observeCartData() {
        viewModelScope.launch {
            combine(
                fetchCartItemsUseCase(),
                fetchTotalCartAmountUseCase.fetchTotalAmount()
            ) { cartResult, totalResult ->
                when {
                    cartResult is NetworkResult.Error<*> -> {
                        _cartUiEvents.send(CartUiEvents.ShowMessage(cartResult.message ?: "Failed to load cart"))
                        _cartUiState.update {item-> item.copy(isLoading = false) }
                    }
                    totalResult is NetworkResult.Error<*> -> {
                        _cartUiEvents.send(CartUiEvents.ShowMessage(totalResult.message ?: "Failed to load total"))
                        _cartUiState.update { it.copy(isLoading = false) }
                    }
                    cartResult is NetworkResult.Loading<*> || totalResult is NetworkResult.Loading<*> -> {
                        _cartUiState.update { it.copy(isLoading = true) }
                    }
                    cartResult is NetworkResult.Success<*> && totalResult is NetworkResult.Success<*> -> {
                        val cartProducts = cartResult.data?.map { it.toDataModel() } ?: emptyList()
                        _cartUiState.update {
                            it.copy(
                                isLoading = false,
                                cartItems = cartProducts,
                                totalAmount = totalResult.data ?: 0f
                            )
                        }
                    }
                }
            }.collectLatest {}
        }
    }

    fun addProductIntoCartFirebase(cartProduct: CartProduct) = executeNetworkAction {
        addProductToCartUseCase(cartProduct.toDomainModel())
    }

    fun deleteCartItem(cartProduct: CartProduct) = executeNetworkAction {
        deleteProductFromCartUseCase(cartProduct.toDomainModel())
    }

    fun updateQuantity(cartProduct: CartProduct) {
        val productId = cartProduct.product.productId

        quantityUpdateJobs[productId]?.cancel()

        quantityUpdateJobs[productId] = executeNetworkAction {
            updateCartQuantityUseCase(cartProduct.toDomainModel())
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