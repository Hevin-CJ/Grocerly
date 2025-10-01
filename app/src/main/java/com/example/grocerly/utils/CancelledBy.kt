package com.example.grocerly.utils

enum class CancelledBy(val displayName: String) {
    NONE("None"),
    USER("User"),
    SELLER("Seller");

    override fun toString(): String = displayName
}