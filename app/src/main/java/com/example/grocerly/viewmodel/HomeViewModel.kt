package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.Repository.remote.HomeRepoImpl
import com.example.grocerly.Repository.remote.NotificationRepoImpl
import com.example.grocerly.Repository.remote.OfferRepoImpl
import com.example.grocerly.Repository.remote.SavedAddressRepoImpl
import com.example.grocerly.Repository.remote.WishListRepoImpl
import com.example.grocerly.model.Address
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.WishItem
import com.example.grocerly.ui.uievents.HomeUiEvents
import com.example.grocerly.ui.uistate.HomeUiState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
class HomeViewModel @Inject constructor(
    application: Application,
    private val homeRepoImpl: HomeRepoImpl,
    private val cartRepoImpl: CartRepoImpl,
    private val offerRepoImpl: OfferRepoImpl,
    private val favouritesRepoImpl: FavouritesRepoImpl,
    private val savedAddressRepoImpl: SavedAddressRepoImpl,
    private val addressRepoImpl: SavedAddressRepoImpl,
    private val wishListRepoImpl: WishListRepoImpl,
    private val notificationRepoImpl: NotificationRepoImpl
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> get() = _uiState.asStateFlow()

    private val _uiEvents = Channel<HomeUiEvents>()
    val uiEvents: Flow<HomeUiEvents> get() = _uiEvents.receiveAsFlow()

    init {
        fetchProductFromFirebase()
        fetchCartItems()
        fetchHomeAddress()
        fetchFavouriteItems()
        fetchWishItemsFromFirebase()

        observeLocalOffersFromDb()
        syncCurrentData()
        claimActiveToken()
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            syncBackgroundData()
            delay(1000L)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun claimActiveToken() {
        viewModelScope.launch {
            notificationRepoImpl.claimActiveDeviceToken()
        }
    }

    fun syncCurrentData() {
        viewModelScope.launch {
            syncBackgroundData()
        }
    }

    private var isSyncing = false

    private suspend fun syncBackgroundData() {
        if (isSyncing) return
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _uiEvents.send(HomeUiEvents.ShowMessage("Enable Wifi or Mobile Data"))
            return
        }

        try {
            isSyncing = true
            homeRepoImpl.syncProductsFromNetwork()
            homeRepoImpl.syncCategoriesFromNetwork()
            offerRepoImpl.syncOffersFromNetwork()
        } catch (e: Exception) {
            _uiEvents.send(HomeUiEvents.ShowMessage("Sync failed: ${e.message}"))
        } finally {
            isSyncing = false
        }
    }

    private fun observeLocalOffersFromDb() {
        viewModelScope.launch {
            offerRepoImpl.getOffers().collectLatest { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(localOffers = result.data ?: emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            homeRepoImpl.getCategoriesFlow().collectLatest { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(categoryItems = result.data ?: emptyList()) }
                }
            }
        }
    }

    fun fetchOrderForNotification(orderId: String, productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = notificationRepoImpl.fetchOrderForNotification(orderId, productId)

            when (result) {
                is NetworkResult.Success -> {
                    val order = result.data?.first
                    val cartProduct = result.data?.second

                    if (order != null && cartProduct != null) {
                        _uiEvents.send(HomeUiEvents.ActionToOrderDetails(order, cartProduct))
                    } else {
                        _uiEvents.send(HomeUiEvents.ShowMessage("Data Parsing Error"))
                    }
                }
                is NetworkResult.Error -> {
                    _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Unknown Error"))
                }
                else -> {}
            }
            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    fun addProductToCart(cartProduct: CartProduct) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val result = cartRepoImpl.addProductToCart(cartProduct)
                if (result is NetworkResult.Error) {
                    _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to add to cart"))
                }
            } else {
                _uiEvents.send(HomeUiEvents.ShowMessage("No Internet Connection"))
            }
        }
    }

    fun addOfferToCart(productId: String, partnerId: String) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvents.send(HomeUiEvents.ShowMessage("Enable Wifi or Mobile Data"))
                return@launch
            }

            val result = offerRepoImpl.addOfferFromFirebaseToCart(productId, partnerId)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvents.send(HomeUiEvents.ShowMessage("Offer Item \nAdded to Cart"))
                }
                is NetworkResult.Error -> {
                    _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to add to Cart"))
                }
                else -> {}
            }
        }
    }

    fun setAsDefaultAddress(address: Address) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val defaultAddress = addressRepoImpl.setAsDefaultAddressInDb(address)
                if (defaultAddress is NetworkResult.Error) {
                    _uiEvents.send(HomeUiEvents.ShowMessage(defaultAddress.message ?: "Failed to set as default"))
                }
            } else {
                _uiEvents.send(HomeUiEvents.ShowMessage("Enable Wifi or Mobile Data"))
            }
        }
    }

    fun addProductToFavourites(favouriteItem: FavouriteItem) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val result = favouritesRepoImpl.addToFavouritesFirebase(favouriteItem)
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is NetworkResult.Error -> {
                        _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to add to favourites"))
                    }
                    else -> {}
                }
            } else {
                _uiEvents.send(HomeUiEvents.ShowMessage("Enable Wifi or Mobile Data"))
            }
        }
    }

    fun addProductToWishlist(wishItem: WishItem) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val result = wishListRepoImpl.addItemToWishList(wishItem)
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvents.send(HomeUiEvents.ShowMessage("Your Item (${wishItem.item.itemName}) \nAdded to Wishlist"))
                    }
                    is NetworkResult.Error -> {
                        _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to add to wishlist"))
                    }
                    else -> {}
                }
            } else {
                _uiEvents.send(HomeUiEvents.ShowMessage("No Internet Connection"))
            }
        }
    }

    fun deleteAddress(address: Address) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val result = savedAddressRepoImpl.deleteAddressFromFirebase(address)
                if (result is NetworkResult.Error) {
                    _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to delete address"))
                } else if (result is NetworkResult.Success) {
                    _uiEvents.send(HomeUiEvents.ShowMessage("Address deleted"))
                }
            } else {
                _uiEvents.send(HomeUiEvents.ShowMessage("No Internet Connection"))
            }
        }
    }

    fun fetchProductFromFirebase() {
        viewModelScope.launch {
            homeRepoImpl.getProductsFlow().collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, products = result.data ?: emptyList()) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to fetch products"))
                    }
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    else -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun fetchWishItemsFromFirebase() {
        viewModelScope.launch {
            wishListRepoImpl.getWishListItems().collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val productIds = result.data?.let { list ->
                            list.mapTo(HashSet(list.size)) { it.item.productId }
                        } ?: emptySet()

                        _uiState.update { it.copy(wishListProductIds = productIds) }
                    }
                    is NetworkResult.Error -> {
                        _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to fetch wishlist items"))
                    }
                    else -> {}
                }
            }
        }
    }

    fun fetchCartItems() {
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest { result ->
                when (result) {
                    is NetworkResult.Success ->{
                        val cartIds = result.data?.let { cartProducts ->
                            cartProducts.mapTo(HashSet(cartProducts.size)) { it.product.productId }
                        }?: emptySet()
                        _uiState.update { it.copy(cartProductIds = cartIds) }
                    }
                    is NetworkResult.Error -> _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to load cart"))
                    else -> {}
                }
            }
        }
    }

    private fun fetchFavouriteItems() {
        viewModelScope.launch {
            favouritesRepoImpl.fetchAllFavourites().collectLatest { result ->
                if (result is NetworkResult.Success) {
                    val favouriteIds = result.data?.let { favouriteItems ->
                        favouriteItems.mapTo(HashSet(favouriteItems.size)) { it.favouriteId }
                    }?:emptySet()

                    _uiState.update { it.copy(favouriteProductIds = favouriteIds) }
                }
            }
        }
    }

    fun fetchHomeAddress() {
        viewModelScope.launch {
            homeRepoImpl.getCityAndState().collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> _uiState.update { it.copy(homeAddress = result.data ?: "") }
                    is NetworkResult.Error -> _uiEvents.send(HomeUiEvents.ShowMessage(result.message ?: "Failed to fetch address"))
                    else -> {}
                }
            }
        }
    }
}