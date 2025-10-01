package com.example.grocerly

import com.example.grocerly.model.Card

interface SavedCardListener {
    fun onEditCardClicked(card: Card)
}