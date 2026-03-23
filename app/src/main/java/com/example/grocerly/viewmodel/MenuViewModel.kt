package com.example.grocerly.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.R
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.Repository.remote.HomeRepoImpl
import com.example.grocerly.Repository.remote.LogoutRepoImpl
import com.example.grocerly.Repository.remote.MenuRepoImpl
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Category
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.ProductCategory
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(private val menuRepoImpl: MenuRepoImpl,private val homeRepoImpl: HomeRepoImpl,private val logoutRepoImpl: LogoutRepoImpl,private val favouritesRepoImpl: FavouritesRepoImpl,private val cartRepoImpl: CartRepoImpl, private val application: Application) : AndroidViewModel(application) {

    private val _categories = MutableStateFlow<NetworkResult<List<Category>>>(NetworkResult.UnSpecified())
    val categories get() = _categories.asStateFlow()

    private val _search_data = MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.UnSpecified())
    val search_data get() = _search_data.asStateFlow()

    private val _cartItems = MutableStateFlow<NetworkResult<List<CartProduct>>>(NetworkResult.UnSpecified())
    val cartItems get() = _cartItems.asStateFlow()

    private val _favourites = MutableStateFlow<NetworkResult<List<FavouriteItem>>>(NetworkResult.UnSpecified())
    val favourites get() = _favourites.asStateFlow()

    private val _logoutstate = MutableSharedFlow<NetworkResult<String>>()
    val logoutstate : Flow<NetworkResult<String>> get() = _logoutstate.asSharedFlow()

    private var isInitialSearchDone = false

    init {
        getCategoriesForSidebar()
        fetchFavouritesList()
        fetchCartList()
    }

    fun getCategoriesForSidebar(){
        viewModelScope.launch {
            fetchCategoriesFromDb()
        }
    }

    fun searchCategory(categoryName: ProductCategory){
        viewModelScope.launch {
            getCategoriesFromSearchFirebase(categoryName)
        }
    }

    fun addToCart(cartProduct: CartProduct){
        viewModelScope.launch {
            cartRepoImpl.addProductToCart(cartProduct)
        }

    }

    fun addToFavourite(favouriteItem: FavouriteItem){
        viewModelScope.launch {
            favouritesRepoImpl.addToFavouritesFirebase(favouriteItem)
        }
    }

    fun signOut(){
        viewModelScope.launch {
            implementMenuLogout()
        }
    }

    fun fetchCartList(){
        viewModelScope.launch {
            cartRepoImpl.fetchAllCartItems().collectLatest {
                _cartItems.emit(it)
            }
        }
    }

    fun fetchFavouritesList(){
        viewModelScope.launch {
            favouritesRepoImpl.fetchAllFavourites().collectLatest {
                _favourites.emit(it)
            }
        }
    }

    private suspend fun implementMenuLogout() {
      if (NetworkUtils.isNetworkAvailable(getApplication())){
          val result = logoutRepoImpl.enableLogout()
          _logoutstate.emit(result)
      }else{
          _logoutstate.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
      }
    }


    private suspend fun fetchCategoriesFromDb() {
       menuRepoImpl.getCategoriesFromFirebase().collectLatest {result->


           if (result is NetworkResult.Success && !result.data.isNullOrEmpty()) {
               val accountCategory = Category(
                   id = 7,
                   imageUrl = "android.resource://${application.packageName}/${R.drawable.person}"
               ).apply {
                   categoryTitleForFirebase = "Account"
               }

               val modifiedList = result.data + accountCategory
               _categories.value = NetworkResult.Success(modifiedList)

               if (!isInitialSearchDone) {
                   val firstRealCategory = result.data[0]

                   searchCategory(firstRealCategory.category)
                   isInitialSearchDone = true
               }
           }else{
               _categories.value = result
           }

        }
    }

    private suspend fun getCategoriesFromSearchFirebase(categoryName: ProductCategory){
        homeRepoImpl.fetchByCategoryFromFirebase(categoryName).collectLatest {
            _search_data.value = it
            Log.d("search_data",it.data.toString())
        }
    }

}