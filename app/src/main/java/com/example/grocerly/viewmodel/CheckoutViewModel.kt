package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.CouponRepoImpl // ADDED
import com.example.grocerly.Repository.remote.SavedAddressRepoImpl
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.EarnedCoupon
import com.example.grocerly.ui.uievents.CheckoutUiEvent
import com.example.grocerly.ui.uistate.CheckoutUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepoImpl: CartRepoImpl,
    private val addressRepoImpl: SavedAddressRepoImpl,
    private val couponRepoImpl: CouponRepoImpl,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<CheckoutUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val quantityUpdateJobs = mutableMapOf<String, Job>()

    private var priceCalculationJob: Job? = null

    init {
        getDefaultAddress()
        fetchCartItems()
    }

    private fun calculateDiscountInRupees(cartItems: List<CartProduct>, coupon: EarnedCoupon?): Int {
        if (coupon == null) return 0

        val partnerSpend = cartItems
            .filter { it.product.partnerId == coupon.partnerId }
            .sumOf { (it.product.itemPrice ?: 0) * it.quantity }

        if (partnerSpend < coupon.minOrderValue) {
            return 0
        }

        val cartTotal = cartItems.sumOf { (it.product.itemPrice ?: 0) * it.quantity }
        return ((cartTotal * coupon.discountAmount) / 100.0).toInt()
    }

    private fun getDefaultAddress() {
        viewModelScope.launch {
            addressRepoImpl.getDefaultAddressFromDb().collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, defaultAddress = result.data) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    else -> Unit
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchCartItems() {
        viewModelScope.launch {

            cartRepoImpl.fetchAllCartItems()
                .combine(_uiState.map { it.appliedCoupon }.distinctUntilChanged()) { cartResult, coupon ->
                    Pair(cartResult, coupon)
                }
                .flatMapLatest { (result, coupon) ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            if (_uiState.value.cartItems.isEmpty()) {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                            flowOf(NetworkResult.Loading())
                        }
                        is NetworkResult.Success -> {
                            val items = result.data ?: emptyList()
                            _uiState.update { it.copy(isLoading = false, cartItems = items) }


                            val calculatedDiscount = calculateDiscountInRupees(items, coupon)

                            if (coupon != null && calculatedDiscount == 0) {
                                removeCoupon()
                            }

                            cartRepoImpl.fetchTotalPriceFromDb(items, calculatedDiscount)
                        }
                        is NetworkResult.Error -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                            flowOf(NetworkResult.Error(result.message))
                        }
                        else -> flowOf(NetworkResult.UnSpecified())
                    }
                }
                .collectLatest { priceResult ->
                    when (priceResult) {
                        is NetworkResult.Success -> _uiState.update { it.copy(priceBreakdown = priceResult.data ?: emptyMap()) }
                        is NetworkResult.Error -> _uiState.update { it.copy(errorMessage = priceResult.message) }
                        else -> Unit
                    }
                }
        }
    }

    fun fetchAddress() {
        viewModelScope.launch {
            addressRepoImpl.getAllAddressFromDb().collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, savedAddresses = result.data ?: emptyList()) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    else -> Unit
                }
            }
        }
    }


    fun applyCoupon(couponCode: String) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiState.update {
                    it.copy(errorMessage = "Enable Wifi or Mobile Data", isCouponError = false)
                }
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = true, couponMessage = null, isCouponError = false)
            }

            val cartItems = _uiState.value.cartItems
            val partnerSpendMap = cartItems
                .groupBy { it.product.partnerId }
                .mapValues { (_, list) -> list.sumOf { (it.product.itemPrice ?: 0) * it.quantity } }

            val result = couponRepoImpl.validateCoupon(couponCode, partnerSpendMap)

            when (result) {
                is NetworkResult.Success -> {

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appliedCoupon = result.data,
                            couponMessage = "${result.data?.code} Applied",
                            isCouponError = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appliedCoupon = null,
                            couponMessage = result.message ?: "Invalid Coupon",
                            isCouponError = true
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun removeCoupon() {
        _uiState.update {
            it.copy(
                appliedCoupon = null,
                couponMessage = null,
                isCouponError = false
            )
        }
        calculateTotalPrice(_uiState.value.cartItems, 0)
    }

    private fun calculateTotalPrice(cartItems: List<CartProduct>, couponAmount: Int = 0) {

        priceCalculationJob?.cancel()

        priceCalculationJob = viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage("Enable Wifi or Mobile Data"))
                return@launch
            }

            cartRepoImpl.fetchTotalPriceFromDb(cartItems, couponAmount).collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {  }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, priceBreakdown = result.data ?: emptyMap()) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    else -> Unit
                }
            }
        }
    }

    fun updateQuantity(cartProduct: CartProduct) {
        val productId = cartProduct.product.productId
        quantityUpdateJobs[productId]?.cancel()

        quantityUpdateJobs[productId] = viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage("Enable Wifi or Mobile Data"))
                return@launch
            }

            val result = cartRepoImpl.updateQuantity(cartProduct)
            if (result is NetworkResult.Error) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage(result.message ?: "Failed to update quantity"))
            }
        }
    }

    fun deleteCartItem(cartProduct: CartProduct) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage("Enable Wifi or Mobile data"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = cartRepoImpl.deleteItemFromCart(cartProduct)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is NetworkResult.Success -> _uiEvent.send(CheckoutUiEvent.ItemDeletedSuccess)
                is NetworkResult.Error -> _uiEvent.send(CheckoutUiEvent.ShowMessage(result.message ?: "Failed to delete item"))
                else -> Unit
            }
        }
    }

    fun deleteAddress(address: Address) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage("Enable Wifi or Mobile data"))
                return@launch
            }

            val result = addressRepoImpl.deleteAddressFromFirebase(address)
            if (result is NetworkResult.Error) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage("Failed to delete address"))
            }
        }
    }

    fun setAsDefault(address: Address) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvent.send(CheckoutUiEvent.ShowMessage("Enable Wifi or Mobile data"))
                return@launch
            }

            val result = addressRepoImpl.setAsDefaultAddressInDb(address)
            when (result) {
                is NetworkResult.Success -> _uiEvent.send(CheckoutUiEvent.AddressUpdatedSuccess)
                is NetworkResult.Error -> _uiEvent.send(CheckoutUiEvent.ShowMessage("Failed to set default address"))
                else -> Unit
            }
        }
    }
}