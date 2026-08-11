package com.example.grocerly.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.CustomSearchRepoImpl
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.Repository.remote.SearchRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.ui.uievents.CustomSearchUiEvents
import com.example.grocerly.ui.uistate.CustomSearchUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CustomSearchViewModel @Inject constructor(private val customSearchRepoImpl: CustomSearchRepoImpl,private val cartRepoImpl: CartRepoImpl,private val favouritesRepoImpl: FavouritesRepoImpl,private val searchRepoImpl: SearchRepoImpl,application: Application): AndroidViewModel(application) {


    private val _uiState = MutableStateFlow(CustomSearchUiState())
    val uiState: StateFlow<CustomSearchUiState> get() = _uiState.asStateFlow()

    private val _uiEvents = Channel<CustomSearchUiEvents>()
    val uiEvents: Flow<CustomSearchUiEvents> get() = _uiEvents.receiveAsFlow()


    init {
        getCartItems()
        getFavouriteItems()
    }

    fun searchCategory(categoryName: ProductCategory) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                    _uiState.update { it.copy(isLoading = true) }
                customSearchRepoImpl.searchByCategory(categoryName).collectLatest { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    categoryProducts = result.data ?: emptyList()
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _uiState.update { it.copy(isLoading = false) }
                            _uiEvents.send(CustomSearchUiEvents.ShowMessage(result.message ?: "Failed to load category"))
                        }
                        is NetworkResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } else {
                _uiEvents.send(CustomSearchUiEvents.ShowMessage("Enable Wifi or Mobile data"))
            }
        }
    }

    fun addProductIntoCartFirebase(cartProduct: CartProduct){
        viewModelScope.launch {
            cartRepoImpl.addProductToCart(cartProduct)
        }
    }

    fun addFavouriteIntoCartFirebase(favouriteItem: FavouriteItem){
        viewModelScope.launch {
            favouritesRepoImpl.addToFavouritesFirebase(favouriteItem)
        }
    }

    fun getCartItems(){
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest {result ->
               when(result){
                   is NetworkResult.Error<*> -> {
                       _uiState.update { it.copy(isLoading = false) }
                       _uiEvents.send(CustomSearchUiEvents.ShowMessage(result.message ?: "Failed to load category"))
                   }
                   is NetworkResult.Loading<*> -> {
                       _uiState.update { it.copy(isLoading = true) }
                   }
                   is NetworkResult.Success<*> ->{
                       _uiState.update {
                           it.copy(
                               isLoading = false,
                               cartItems = result.data ?: emptyList()
                           )
                       }
                   }
                   is NetworkResult.UnSpecified<*> ->{
                       _uiState.update { it.copy(isLoading = false) }
                   }
               }
            }
        }
    }

    fun getFavouriteItems(){
        viewModelScope.launch {
            favouritesRepoImpl.fetchAllFavourites().collectLatest {result ->
                when(result){
                    is NetworkResult.Error<*> -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvents.send(CustomSearchUiEvents.ShowMessage(result.message ?: "Failed to load category"))
                    }
                    is NetworkResult.Loading<*> -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success<*> -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                favouriteItems = result.data ?: emptyList()
                            )
                        }
                    }
                    is NetworkResult.UnSpecified<*> -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }


    @OptIn(FlowPreview::class)
    fun searchItemsInFirebase(query: String) {
        viewModelScope.launch {
            searchRepoImpl.searchProduct(query).debounce(400L.milliseconds).distinctUntilChanged().collectLatest { result ->
                when (result) {
                    is NetworkResult.Error<*> ->{
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvents.send(CustomSearchUiEvents.ShowMessage(result.message ?: "Failed to load category"))
                    }
                    is NetworkResult.Loading<*> -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success<*> -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                searchResults = result.data ?: emptyList()
                            )
                        }
                    }
                    is NetworkResult.UnSpecified<*> -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }

                Log.d("searchItem", result.data.toString())
            }
        }
    }

    private suspend fun searchByCategory(categoryName: ProductCategory) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            customSearchRepoImpl.searchByCategory(categoryName).collectLatest {result ->
               when(result){
                   is NetworkResult.Error<*> -> {
                       _uiState.update { it.copy(isLoading = false) }
                       _uiEvents.send(CustomSearchUiEvents.ShowMessage(result.message ?: "Failed to load category"))
                   }
                   is NetworkResult.Loading<*> -> {
                       _uiState.update { it.copy(isLoading = true) }
                   }
                   is NetworkResult.Success<*> -> {
                       _uiState.update {
                           it.copy(
                               isLoading = false,
                               categoryProducts = result.data ?: emptyList()
                           )
                       }
                   }
                   is NetworkResult.UnSpecified<*> -> {
                       _uiState.update { it.copy(isLoading = false) }
                   }
               }
            }
        }else{
            _uiEvents.send(CustomSearchUiEvents.ShowMessage("Enable Wifi or Mobile data"))
        }
    }

}