package com.example.grocerly.model.uievents

import android.os.Bundle
import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.Order

sealed interface HomeUiEvents {
    data class ShowMessage(val message: String) : HomeUiEvents
    data class ActionToOrderDetails(val order: Order, val cartProduct: CartProduct) : HomeUiEvents
}