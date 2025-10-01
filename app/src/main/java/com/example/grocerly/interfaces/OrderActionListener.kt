package com.example.grocerly.interfaces

import android.view.View
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order

interface OrderActionListener {
    fun onItemTouchListener(cartProduct: CartProduct,order: Order)
}