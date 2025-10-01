package com.example.grocerly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.CartRepoImpl
import com.example.grocerly.Repository.remote.SavedAddressRepoImpl
import com.example.grocerly.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderSharedViewModel @Inject constructor(private val cartRepoImpl: CartRepoImpl,private val savedAddressRepoImpl: SavedAddressRepoImpl): ViewModel() {

    private val _currentOrder = MutableStateFlow<Order?>(null)
    val currentOrder =_currentOrder.asStateFlow()


    fun setOrder(order: Order){
        viewModelScope.launch {
            _currentOrder.emit(order)
        }
    }

    fun clearOrder(){
        viewModelScope.launch {
            _currentOrder.emit(null)
        }
    }



}