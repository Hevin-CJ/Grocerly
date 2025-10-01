package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.SavedCardsRepoImpl
import com.example.grocerly.model.Card
import com.example.grocerly.utils.CardValidation
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.savedCardState
import com.example.grocerly.utils.validateCVV
import com.example.grocerly.utils.validateCardNumber
import com.example.grocerly.utils.validateExpiry
import com.example.grocerly.utils.validateHolderName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SavedCardsViewModel @Inject constructor(private val savedCardsRepoImpl: SavedCardsRepoImpl,application: Application): AndroidViewModel(application) {

    private val cardSavedState = Channel<NetworkResult<String>>()
    val _cardSavedState: Flow<NetworkResult<String>> get() = cardSavedState.receiveAsFlow()

    private val cardDeleteState = Channel<NetworkResult<String>>()
    val _cardDeleteState: Flow<NetworkResult<String>> get() = cardDeleteState.receiveAsFlow()

    private val cardValidationState = Channel<savedCardState>()
    val _cardValidationState: Flow<savedCardState> get() = cardValidationState.receiveAsFlow()

    val fetchedCards: Flow<NetworkResult<List<Card>>> = savedCardsRepoImpl.getAllSavedCardsFromFirebase()
    val fetchedLiveData  = savedCardsRepoImpl.getAllSavedCardsFromFirebase().asLiveData()

    val emptyCardState: LiveData<Boolean> = fetchedLiveData.map { result ->
        when (result) {
            is NetworkResult.Success -> result.data.isNullOrEmpty()
            is NetworkResult.Error<*> -> false
            is NetworkResult.Loading<*> -> true
            is NetworkResult.UnSpecified<*> -> false
        }
    }
    
    fun saveCardDetails(card: Card){
        viewModelScope.launch {
            if (isValidatedCard(card)){
                suspendSaveCardDetails(card)
            }else{
                emitCardValidationErrors(card)
            }

        }
    }

    fun updateCardDetails(card: Card){
        viewModelScope.launch {
            if (isValidatedCard(card)){
                suspendUpdateCardDetails(card)
            }else{
                emitCardValidationErrors(card)
            }
        }
    }

    fun deleteCardFromFirebase(card: Card){
        viewModelScope.launch {
            suspendDeleteCard(card)
        }
    }

    private suspend fun suspendUpdateCardDetails(card: Card) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            cardSavedState.send(NetworkResult.Loading())
            val savedCardState = savedCardsRepoImpl.updateCardToFirebase(card)
            cardSavedState.send(savedCardState)
        }else{
            cardSavedState.send(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }

    private suspend fun emitCardValidationErrors(card: Card) {
        val cardState = savedCardState(validateHolderName(card.holderName),
            validateCardNumber(card.cardNumber),
            validateExpiry(card.expiryDate),
            validateCVV(card.cvv,card.cardType))
        cardValidationState.send(cardState)
    }

    private suspend fun suspendSaveCardDetails(card: Card) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            cardSavedState.send(NetworkResult.Loading())
            val savedCardState = savedCardsRepoImpl.saveCardToFirebase(card)
            cardSavedState.send(savedCardState)
        }else{
            cardSavedState.send(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }

    }

    private suspend fun suspendDeleteCard(card: Card){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            cardDeleteState.send(NetworkResult.Loading())
            val cardDeletedState = savedCardsRepoImpl.deleteCardFromFirebase(card)
            cardDeleteState.send(cardDeletedState)
        }else{
            cardDeleteState.send(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }




    private fun isValidatedCard(card: Card): Boolean{
        val isNameValidated = validateHolderName(card.holderName)
        val isCardNumberValidated  = validateCardNumber(card.cardNumber)
        val isExpiryValidated = validateExpiry(card.expiryDate)
        val isCvvValidated = validateCVV(card.cvv,card.cardType)

        return isNameValidated is CardValidation.Success && isCardNumberValidated is CardValidation.Success && isExpiryValidated is CardValidation.Success && isCvvValidated is CardValidation.Success
    }

}