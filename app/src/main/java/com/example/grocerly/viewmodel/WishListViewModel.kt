package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.WishListRepoImpl
import com.example.grocerly.model.WishItem
import com.example.grocerly.model.uievents.WishListUiEvents
import com.example.grocerly.model.uievents.WishListUiEvents.*
import com.example.grocerly.model.uistate.WishListUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class WishListViewModel @Inject constructor(application: Application
                                            ,private val wishListRepoImpl: WishListRepoImpl,private val cartRepoImpl: CartRepoImpl): AndroidViewModel(application) {

   private val uiState_ =  MutableStateFlow(WishListUiState())
   val uiState : StateFlow<WishListUiState> get() = uiState_.asStateFlow()

    private val uiEvents_ = Channel<WishListUiEvents>()
    val uiEvents : Flow<WishListUiEvents> get() = uiEvents_.receiveAsFlow()

    init {
        fetchWishListItems()
        fetchCartItems()
    }


    fun fetchWishListItems() {
        viewModelScope.launch {
            implementFetchWishListItems()
        }
    }



    fun addWishItemToCart(wishItem: WishItem) {
        viewModelScope.launch {
            implementAddWishItemToCart(wishItem)
        }
    }

    fun removeWishItemFromWishList(wishItem: WishItem) {
        viewModelScope.launch {
            implementRemoveWishItemFromWishList(wishItem)
        }
    }

    fun fetchCartItems(){
        viewModelScope.launch {
            fetchCartItemsFromDb()
        }
    }

    private suspend fun fetchCartItemsFromDb() {
       cartRepoImpl.fetchAllCartItems().collectLatest { result ->
           if (result is NetworkResult.Success) {
               uiState_.update {
                   it.copy(cartItems = result.data ?: emptyList())
               }
           } else if (result is NetworkResult.Error) {
               uiEvents_.send(ShowMessage(result.message.toString()))
           }
        }
    }

    private  suspend fun implementRemoveWishItemFromWishList(wishItem: WishItem) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){

            uiState_.update { it.copy(isLoading = true) }
            val response = wishListRepoImpl.removeItemFromWishList(wishItem)
            when(response) {
                is NetworkResult.Success -> {
                    uiState_.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }

                is NetworkResult.Error<*> -> {
                    uiState_.update {
                        it.copy(
                            isLoading = false
                        )
                    }

                    uiEvents_.send(ShowMessage(response.message.toString()))
                }
                is NetworkResult.Loading<*> -> {
                    uiState_.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }
                is NetworkResult.UnSpecified<*> ->{
                    uiState_.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
            }
        }else{
            uiEvents_.send(ShowMessage("No Internet Connection"))
        }
    }


    private suspend fun implementAddWishItemToCart(wishItem: WishItem) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            uiState_.update { it.copy(isLoading = true) }
            val response = wishListRepoImpl.addWishListItemToCart(wishItem)
            when (response) {
                is NetworkResult.Success -> {
                    uiState_.update { it.copy(isLoading = false) }
                }
                is NetworkResult.Error<*> -> {
                    uiState_.update { it.copy(isLoading = false) }
                    uiEvents_.send(ShowMessage(response.message.toString()))
                }
                else -> {
                    uiState_.update { it.copy(isLoading = false) }
                }
            }
        } else {
            uiEvents_.send(ShowMessage("Enable Wifi or Mobile Data"))
        }
    }

    private suspend fun implementFetchWishListItems() {
        wishListRepoImpl.getWishListItems().collectLatest {result ->
            when(result){
                is NetworkResult.Error<*> ->{
                    uiState_.update {
                        it.copy(
                            isLoading = false,
                        )
                    }

                    uiEvents_.send(ShowMessage(result.message ?: "Unknown Error Occurred"))
                }
                is NetworkResult.Loading<*> -> {
                    uiState_.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }
                is NetworkResult.Success<*> -> {
                    uiState_.update {
                        it.copy(
                            isLoading = false,
                            wishList = result.data ?: emptyList()
                        )
                    }
                }
                is NetworkResult.UnSpecified<*> ->{
                    uiState_.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
            }
        }
    }


}