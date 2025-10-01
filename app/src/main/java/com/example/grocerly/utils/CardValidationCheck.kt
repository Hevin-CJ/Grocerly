package com.example.grocerly.utils

import android.icu.util.Calendar
import com.example.grocerly.model.ExpiryDate


fun validateHolderName(holderName: String): CardValidation{

    if (holderName.isEmpty()) return CardValidation.Failed("Cannot be left blank")

    if (holderName.length<8) return CardValidation.Failed("Error:At least 8 Characters")

    return CardValidation.Success

}


fun validateCardNumber(cardNo: String): CardValidation{

    val cleaned = cardNo.replace("\\s".toRegex(), "")

    if (cleaned.isEmpty()) return CardValidation.Failed("Card Number Cannot be Empty")

    if (!cleaned.all { it.isDigit() }) return CardValidation.Failed("Card number must be numeric")

    if (!cleaned.matches("\\d{13,19}".toRegex())) return CardValidation.Failed("Card number seem to be invalid")

    isValidLuhn(cleaned)

    return CardValidation.Success
}


fun validateExpiry(expiry: ExpiryDate): CardValidation{

    val(month,year) = expiry

    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)+1



    if (month == 0 || year == 0) {
        return CardValidation.Failed("Invalid card format")
    }

    if (year.toString().length!=4) return CardValidation.Failed("Incorrect Year Format,Must be in YYYY eg:2025")

    if (month !in 1..12 || year !in currentYear..(currentYear+6)) {
        return CardValidation.Failed("Invalid expiry date")
    }


    return if (year>currentYear || year == currentYear && month>=currentMonth ) CardValidation.Success else CardValidation.Failed("Card Expired")
}


fun validateCVV(cvv: String,cardType: String): CardValidation{

    val cleaned = cvv.trim()


    if (cleaned.isEmpty()) {
        return CardValidation.Failed("CVV cannot be empty")
    }

  val isValid =  when (cardType) {
        "American Express" -> cvv.length == 4
        else -> cvv.length == 3
    } && cvv.all { it.isDigit() }
   return if (isValid)  CardValidation.Success else CardValidation.Failed("Invalid CVV for card type")
}






private fun isValidLuhn(number: String): CardValidation {
    var sum = 0
    var alternate = false
    for (i in number.length - 1 downTo 0) {
        var n = number[i].digitToInt()
        if (alternate) {
            n *= 2
            if (n > 9) n -= 9
        }
        sum += n
        alternate = !alternate
    }
    return if ( sum % 10 == 0) CardValidation.Success else CardValidation.Failed("Error:Enter Valid Card Number")
}