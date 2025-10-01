package com.example.grocerly

import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.DeliveryCharge

interface CheckoutListener {
    fun onQuantityChanged(cartProduct: CartProduct)
    fun onItemDeleted(cartProduct: CartProduct)
}