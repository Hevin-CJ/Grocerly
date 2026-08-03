package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.PaymentRepoImpl
import com.example.grocerly.Repository.remote.SavedCardsRepoImpl
import com.example.grocerly.model.Card
import com.example.grocerly.model.Order
import com.example.grocerly.model.uievents.PaymentUiEvent
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.PaymentMethodItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject



@HiltViewModel
class PaymentViewModel @Inject constructor(
    application: Application,
    private val savedCardsRepoImpl: SavedCardsRepoImpl,
    private val paymentRepoImpl: PaymentRepoImpl
) : AndroidViewModel(application) {

    private val _savedCards = MutableStateFlow<NetworkResult<List<Card>>>(NetworkResult.UnSpecified())
    val savedCards: StateFlow<NetworkResult<List<Card>>> = _savedCards.asStateFlow()

    private val _savedPaymentHeader = MutableStateFlow<NetworkResult<List<PaymentMethodItem.Header>>>(NetworkResult.UnSpecified())
    val savedPaymentHeader: StateFlow<NetworkResult<List<PaymentMethodItem.Header>>> = _savedPaymentHeader.asStateFlow()

    private val _confirmOrderState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.UnSpecified())
    val confirmOrderState: StateFlow<NetworkResult<Unit>> = _confirmOrderState.asStateFlow()

    private val _paymentEvent = Channel<PaymentUiEvent>()
    val paymentEvent = _paymentEvent.receiveAsFlow()

    init {
        fetchSavedCards()
        fetchHeaders()
    }

    fun fetchSavedCards() {
        viewModelScope.launch {
            _savedCards.emit(NetworkResult.Loading())
            savedCardsRepoImpl.getAllSavedCardsFromFirebase().collectLatest {
                _savedCards.emit(it)
            }
        }
    }

    fun fetchHeaders() {
        viewModelScope.launch {
            val headers = paymentRepoImpl.fetchPaymentHeader()
            _savedPaymentHeader.emit(headers)
        }
    }


    fun processCardPayment(cardId: String, cvv: String, order: Order, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                onResult("Enable Wifi or Mobile Data")
                return@launch
            }

            val cvvResult = paymentRepoImpl.checkCvvForPayment(cardId, cvv)

            if (cvvResult is NetworkResult.Success) {
                onResult("")
                confirmOrder("CARD", order)
            } else {
                onResult(cvvResult.message ?: "Invalid CVV")
            }
        }
    }


    fun prepareUpiPayment(order: Order, upi: String) {
        if (upi.isBlank()) return

        try {
            val options = JSONObject().apply {
                put("name", "Grocerly")
                put("description", "Delivering The Best Groceries")
                put("image", "http://example.com/image/rzp.jpg")
                put("theme.color", "#0CA201")
                put("currency", "INR")
                put("amount", order.totalOrderPrice * 100)
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 4)
                })
                put("prefill", JSONObject().apply {
                    put("contact", order.address.phoneNumber)
                })
            }

            viewModelScope.launch {
                _paymentEvent.send(PaymentUiEvent.LaunchRazorpay(options))
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _paymentEvent.send(PaymentUiEvent.ShowMessage("Error preparing payment: ${e.message}"))
            }
        }
    }

    fun confirmOrder(paymentType: String, order: Order) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _paymentEvent.send(PaymentUiEvent.ShowMessage("Enable Wifi or Mobile Data"))
                return@launch
            }

            _confirmOrderState.emit(NetworkResult.Loading())

            val result = paymentRepoImpl.sendOrderToUserAndSeller(paymentType, order)
            _confirmOrderState.emit(result)

            if (result is NetworkResult.Success) {
                _paymentEvent.send(PaymentUiEvent.NavigateToOrderPlaced)
            } else if (result is NetworkResult.Error) {
                _paymentEvent.send(PaymentUiEvent.ShowMessage(result.message ?: "Failed to place order"))
            }
        }
    }
}