package com.example.grocerly.utils

object QuantityUtils {
    fun convertQuantityIntoString(quantityType: QuantityType): String {
        return when (quantityType) {
            QuantityType.selectQuantity -> "/ Quantity"
            QuantityType.perKilogram -> "/ Kg"
            QuantityType.perLiter -> "/ L"
            QuantityType.perPiece -> "/ Piece"
            QuantityType.perPacket -> "/ Packet"
        }
    }
}