package com.example.grocerly.utils

sealed class NetworkResult<T>(val data:T?=null,val message:String?=null) {
    class Success<T>(data: T):NetworkResult<T>(data)
    class Loading<T>:NetworkResult<T>()
    class UnSpecified<T>:NetworkResult<T>()
    class Error<T>(message: String?):NetworkResult<T>(data = null,message)
}

sealed class AccountResult<T>(val data:T?=null,val message: String?=null){
    class Success<T>(data: T):AccountResult<T>(data)
    class Loading<T>:AccountResult<T>()
    class UnSpecified<T>:AccountResult<T>()
    class Error<T>(message: String?):AccountResult<T>(null,message)
    class EmailUpdated<T>(data:T): AccountResult<T>(data)
}