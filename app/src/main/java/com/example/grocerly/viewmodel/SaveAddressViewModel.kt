package com.example.grocerly.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.SavedAddressRepoImpl
import com.example.grocerly.model.Address
import com.example.grocerly.model.LocationInfo
import com.example.grocerly.utils.AddressFieldState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.utils.validateName
import com.example.grocerly.utils.validatePhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaveAddressViewModel @Inject constructor(private val savedAddressRepoImpl: SavedAddressRepoImpl,application:Application) : AndroidViewModel(application) {

    private val _addressValidateState = Channel<AddressFieldState>()
    val addressValidateState:Flow<AddressFieldState> get() = _addressValidateState.receiveAsFlow()

    private val _savedAddressState = Channel<NetworkResult<String>>()
    val savedAddressState:Flow<NetworkResult<String>> get() = _savedAddressState.receiveAsFlow()

    private val _fetchedAddress = MutableStateFlow<NetworkResult<List<Address>>>(NetworkResult.UnSpecified())
    val fetchedAddress:Flow<NetworkResult<List<Address>>> get() = _fetchedAddress.asStateFlow()

    private val _setDefaultState = Channel<NetworkResult<String>>()
    val setDefaultState:Flow<NetworkResult<String>> get() = _setDefaultState.receiveAsFlow()

    private val _deleteAddress = Channel<NetworkResult<String>>()
    val deleteAddress:Flow<NetworkResult<String>> get() = _deleteAddress.receiveAsFlow()

    private val _locationAddress = MutableStateFlow<NetworkResult<LocationInfo>>(NetworkResult.UnSpecified())
    val locationAddress: Flow<NetworkResult<LocationInfo>> get() = _locationAddress.asStateFlow()

    init {
        getAllAddressFromDatabase()
    }

    fun insertAddressIntoDb(address: Address){
        viewModelScope.launch {
            saveAddressToFirebase(address)
        }
    }

    fun deleteAddress(address: Address){
        viewModelScope.launch {
            deleteAddressFromDb(address)
        }
    }

    fun updateAddressInDb(address: Address){
        viewModelScope.launch {
            updateAddressToFirebase(address)
        }
    }

    fun getLocationDetails(context: Context){
        viewModelScope.launch {
            getLocationFromFusedClient(context)
        }
    }



    fun getAllAddressFromDatabase(){
        viewModelScope.launch {
            _fetchedAddress.emit(NetworkResult.Loading())
            savedAddressRepoImpl.getAllAddressFromDb().collectLatest { address ->
                _fetchedAddress.emit(address)
            }
        }
    }

    fun setAddressAsDefault(address: Address){
        viewModelScope.launch {
            setAddressToDefaultFirebase(address)
        }
    }

    private suspend fun setAddressToDefaultFirebase(address: Address) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val defaultState = savedAddressRepoImpl.setAsDefaultAddressInDb(address)
            _setDefaultState.send(defaultState)
        }else{
            _setDefaultState.send(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }


    private suspend fun deleteAddressFromDb(address: Address){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            _deleteAddress.send(NetworkResult.Loading())
            val deletedAddress = savedAddressRepoImpl.deleteAddressFromFirebase(address)
            _deleteAddress.send(deletedAddress)
        }else{
            _deleteAddress.send(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }


    private suspend fun saveAddressToFirebase(address: Address) {
        if(isAddressValidated(address)){
            _savedAddressState.send(NetworkResult.Loading())
            val savedAddressResponse = savedAddressRepoImpl.saveAddressToFirebase(address)
            _savedAddressState.send(savedAddressResponse)
        }else{
           sendValidationErrors(address)
        }
    }

    private suspend fun updateAddressToFirebase(address: Address) {
        if(isAddressValidated(address)){
            _savedAddressState.send(NetworkResult.Loading())
            val savedAddressResponse = savedAddressRepoImpl.updateAddressInFirebase(address)
            _savedAddressState.send(savedAddressResponse)
        }else{
            sendValidationErrors(address)
        }
    }


    private suspend fun getLocationFromFusedClient(context: Context){
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            _locationAddress.emit(NetworkResult.Loading())
            val locationAddress = savedAddressRepoImpl.getCurrentLocationForClient(context)
            _locationAddress.emit(locationAddress)
        }else{
            _locationAddress.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }

    private suspend fun sendValidationErrors(address: Address) {
        val validateState = when{
            address.state.isEmpty() ->  RegisterValidation.Failed("Required:State Cannot be Empty")
            !address.state.matches(Regex("^[a-zA-Z\\s]+\$")) -> RegisterValidation.Failed("Invalid State")
            else -> RegisterValidation.Success
        }
        val validateCity = when{
            address.city.isEmpty() ->  RegisterValidation.Failed("Enter the city")
            !address.city.matches(Regex("^[a-zA-Z\\s]+\$")) -> RegisterValidation.Failed("Invalid City")
            else -> RegisterValidation.Success
        }

        val validateAddress = when{
            address.deliveryAddress.isEmpty() -> RegisterValidation.Failed("Enter the Address")
            !address.deliveryAddress.matches(Regex("^[a-zA-Z\\s]+\$")) -> RegisterValidation.Failed("Invalid Delivery Address")
            else -> RegisterValidation.Success
        }
        val validateLandMark = when{
            address.landMark.isEmpty() -> RegisterValidation.Failed("Enter the LandMark")
            !address.deliveryAddress.matches(Regex("^[a-zA-Z\\s]+\$")) -> RegisterValidation.Failed("Invalid LandMark , support text only")
            else -> RegisterValidation.Success
        }
        val validatePinCode = when {
            address.pinCode.isEmpty() -> RegisterValidation.Failed("Enter the PinCode")
            !address.pinCode.isDigitsOnly() -> RegisterValidation.Failed("Incorrect PinCode Format")
            address.pinCode.length < 6 -> RegisterValidation.Failed("PinCode must be 6 digits")
            else -> RegisterValidation.Success
        }

        val validationState = AddressFieldState(validateName(address.firstName),validatePhoneNumber(address.phoneNumber),validatePhoneNumber(address.alternateNumber),validateState,validateCity,validateAddress,validateLandMark,validatePinCode)
        _addressValidateState.send(validationState)
    }

    private fun isAddressValidated(address: Address): Boolean {
        val validateFirstName = validateName(address.firstName)
        val validatePhoneNo = validatePhoneNumber(address.phoneNumber)
        val validateAlternateNo = validatePhoneNumber(address.alternateNumber)
        val validateState = if (address.state.isEmpty())  RegisterValidation.Failed("Enter the  state") else RegisterValidation.Success
        val validateCity = if (address.city.isEmpty()) RegisterValidation.Failed("Enter the city") else RegisterValidation.Success
        val validateAddress = if (address.deliveryAddress.isEmpty()) RegisterValidation.Failed("Enter the Address") else RegisterValidation.Success
        val validateLandMark = if (address.landMark.isEmpty()) RegisterValidation.Failed("Enter the LandMark") else RegisterValidation.Success
        val validatePinCode = when {
            address.pinCode.isEmpty() -> RegisterValidation.Failed("Enter the PinCode")
            !address.pinCode.isDigitsOnly() -> RegisterValidation.Failed("Incorrect PinCode Format")
            address.pinCode.length < 6 -> RegisterValidation.Failed("PinCode must be atleast 5 digits")
            else -> RegisterValidation.Success
        }

        return listOf(
            validateFirstName,
            validatePhoneNo,
            validateAlternateNo,
            validateState,
            validateCity,
            validateAddress,
            validateLandMark,
            validatePinCode
        ).all { it is RegisterValidation.Success }
    }

}