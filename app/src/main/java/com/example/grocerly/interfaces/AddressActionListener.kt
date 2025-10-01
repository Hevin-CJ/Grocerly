package com.example.grocerly.interfaces

import com.example.grocerly.model.Address

interface AddressActionListener {
    fun onAddressActionRequested()
    fun onEditRequested(address: Address)
    fun onDeleteRequested(address: Address)
    fun onClickLayoutToMakeDefault(address: Address)
}