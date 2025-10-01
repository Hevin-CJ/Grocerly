package com.example.grocerly

import com.example.grocerly.model.Address

interface onAddressMenuClickListener {
    fun onEditClicked(address: Address)
    fun onsetDefaultClicked(address: Address)
    fun onDeleteClicked(address: Address)
}