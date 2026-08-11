package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.FavouritesRepoImpl
import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.ui.uievents.FavouriteUiEvents
import com.example.grocerly.ui.uistate.FavouriteUiState
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
class FavouriteViewModel @Inject constructor(
    private val favouritesRepoImpl: FavouritesRepoImpl,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FavouriteUiState())
    val uiState: StateFlow<FavouriteUiState> get() = _uiState.asStateFlow()


    private val _uiEvents = Channel<FavouriteUiEvents>()
    val uiEvents: Flow<FavouriteUiEvents> get() = _uiEvents.receiveAsFlow()

    init {
        getAllFavouritesFromFirebase()
    }

    private fun getAllFavouritesFromFirebase() {
        viewModelScope.launch {
            favouritesRepoImpl.fetchAllFavourites().collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                favouriteItems = result.data ?: emptyList()
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvents.send(FavouriteUiEvents.ShowMessage(result.message ?: "Failed to load favourites"))
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun deleteFavouriteFromFirebase(favouriteItem: FavouriteItem) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _uiEvents.send(FavouriteUiEvents.ShowMessage("Enable Wifi or Mobile data"))
                return@launch
            }

            _uiState.update { it.copy(isActionLoading = true) }

            val result = favouritesRepoImpl.deleteFavourite(favouriteItem)

            when (result) {
                is NetworkResult.Success -> {
                    _uiEvents.send(FavouriteUiEvents.ShowMessage("Removed from favourites"))
                }
                is NetworkResult.Error -> {
                    _uiEvents.send(FavouriteUiEvents.ShowMessage(result.message ?: "Failed to delete item"))
                }
                else -> {}
            }

            _uiState.update { it.copy(isActionLoading = false) }
        }
    }
}