package com.example.grocerly.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CouponRepoImpl
import com.example.grocerly.ui.uievents.CouponUiEvent
import com.example.grocerly.ui.uistate.CouponUiState
import com.example.grocerly.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CouponViewModel @Inject constructor(
    application: Application,
    private val couponRepoImpl: CouponRepoImpl
): AndroidViewModel(application) {

    private val _couponUiState = MutableStateFlow(CouponUiState())
    val couponUiState = _couponUiState.asStateFlow()

    private val _couponUiEvent = Channel<CouponUiEvent>()
    val couponUiEvent = _couponUiEvent.receiveAsFlow()



    init {
        fetchAllAvailableCoupons()
    }

    private fun fetchAllAvailableCoupons() {
        viewModelScope.launch {
            couponRepoImpl.fetchAllAvailableCoupons().collect { result ->
                when(result){
                    is NetworkResult.Error<*> ->{
                        _couponUiState.update {
                            it.copy(
                                isLoading = false,
                            )
                        }

                        _couponUiEvent.send(CouponUiEvent.ShowMessage(result.message ?: "Something went wrong"))
                    }
                    is NetworkResult.Loading<*> -> {
                        _couponUiState.update {
                            it.copy(
                                isLoading = true
                            )
                        }
                    }
                    is NetworkResult.Success<*> -> {
                        _couponUiState.update {
                            it.copy(
                                isLoading = false,
                                couponList = result.data ?: emptyList()
                            )
                        }

                        Log.d("couponlistgotvm",result.data.toString())
                    }
                    is NetworkResult.UnSpecified<*> ->{
                        _couponUiState.update {
                            it.copy(
                                isLoading = false
                            )
                        }
                    }
                }

            }
        }
    }


}