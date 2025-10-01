package com.example.grocerly.utils

enum class Reason(val displayName: String) {
    ReasonNotDepicted("reason not given"),
    ordercreatedbymistake("The order created by mistake"),
    itempricetoohigh("The item price too high"),
    foundcheapersomewhereelse("Found cheaper somewhere else"),
    Anitemhastobeadded("An item has to be added"),
    Anitemhastoberemoved("An item has to be removed"),
    addresschange("The Address has to be changed"),
    itempricedecreased("Item Price has decreased"),
    deliverytimetoolong("The delivery time is too long")
}