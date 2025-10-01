package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.PaymentRepoImpl
import com.example.grocerly.Repository.remote.SavedCardsRepoImpl
import com.example.grocerly.model.Card
import com.example.grocerly.model.Order
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.PaymentMethodItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor (application: Application,private val savedCardsRepoImpl: SavedCardsRepoImpl,private val paymentRepoImpl: PaymentRepoImpl,private val cartRepoImpl: CartRepoImpl): AndroidViewModel(application) {

    private val _savedCards = MutableStateFlow<NetworkResult<List<Card>>>(NetworkResult.UnSpecified())
    val savedCards: StateFlow<NetworkResult<List<Card>>> = _savedCards.asStateFlow()

    private val _savedPaymentHeader = MutableStateFlow<NetworkResult<List<PaymentMethodItem.Header>>>(NetworkResult.UnSpecified())
    val savedPaymentHeader: StateFlow<NetworkResult<List<PaymentMethodItem.Header>>> = _savedPaymentHeader.asStateFlow()

    private val _cvvState = MutableStateFlow<NetworkResult<String>>(NetworkResult.UnSpecified())
    val cvvState: StateFlow<NetworkResult<String>> = _cvvState.asStateFlow()

    private val _confirmOrderState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.UnSpecified())
    val confirmOrderState: StateFlow<NetworkResult<Unit>> = _confirmOrderState.asStateFlow()

    init {
        fetchSavedCards()
        fetchHeaders()
    }


    fun fetchSavedCards(){
       viewModelScope.launch {
           savedCardsRepoImpl.getAllSavedCardsFromFirebase().collectLatest {
               _savedCards.emit(NetworkResult.Loading())
               _savedCards.emit(it)
           }
       }
    }


    fun fetchHeaders(){
        viewModelScope.launch {
            fetchPaymentHeader()
        }
    }

    fun checkCvvForPaymentDb(cardId: String,cvv: String){
        viewModelScope.launch {
            checkCvvAndDoPayment(cardId,cvv)
        }
    }

   fun setOrderInDb(orderType: String, order: Order){
       viewModelScope.launch {
           setAllOrderInFirebase(orderType,order)
       }
   }

    private suspend fun setAllOrderInFirebase(paymentType: String, order: Order) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            _confirmOrderState.emit(NetworkResult.Loading())
            val setOrderState = paymentRepoImpl.sendOrderToUserAndSeller(paymentType,order)
            _confirmOrderState.emit(setOrderState)
        }else{
            _confirmOrderState.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }


    private suspend fun checkCvvAndDoPayment(cardId: String,cvv: String){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val cvvPaymentState =  paymentRepoImpl.checkCvvForPayment(cardId,cvv)
            _cvvState.emit(cvvPaymentState)
        }else{
            _cvvState.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }


    private suspend fun fetchPaymentHeader() {
        val headers = paymentRepoImpl.fetchPaymentHeader()
        _savedPaymentHeader.emit(headers)
    }

}