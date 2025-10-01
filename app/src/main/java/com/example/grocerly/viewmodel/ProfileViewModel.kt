package com.example.grocerly.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.local.ProfileLocalRepoImpl
import com.example.grocerly.Repository.remote.ProfileRepoImpl
import com.example.grocerly.model.Account
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.room.entity.ProfileEntity
import com.example.grocerly.utils.AccountResult
import com.example.grocerly.utils.EmailChangeFieldState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.ProfileFieldState
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.utils.validateEmail
import com.example.grocerly.utils.validateName
import com.example.grocerly.utils.validatePassword
import com.example.grocerly.utils.validatePhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(application: Application,private val profileRepoImpl: ProfileRepoImpl,private val profileLocalRepoImpl: ProfileLocalRepoImpl) :AndroidViewModel(application) {

    val getProfileData = profileLocalRepoImpl.getProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ProfileEntity("","","","","","",""))

    private val _logoutState = MutableSharedFlow<NetworkResult<String>>()
    val logoutState:Flow<NetworkResult<String>> get() = _logoutState.asSharedFlow()

    private val _uploadImageState = MutableSharedFlow<NetworkResult<String>>()
    val uploadImageState: SharedFlow<NetworkResult<String>> get() = _uploadImageState.asSharedFlow()

    private val _accountDetails = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.UnSpecified())
    val accountDetails: StateFlow<NetworkResult<Unit>> get() = _accountDetails.asStateFlow()

    private val _updatedDetails = Channel<AccountResult<String>>()
    val updatedDetails: Flow<AccountResult<String>> get() = _updatedDetails.receiveAsFlow()

    private val _profileValidation = Channel<ProfileFieldState>()
    val profileValidation: Flow<ProfileFieldState> = _profileValidation.receiveAsFlow()

    private val _emailChangeValidation = Channel<EmailChangeFieldState>()
    val emailChangeValidation: Flow<EmailChangeFieldState> = _emailChangeValidation.receiveAsFlow()

    private val _changeUserEmailState = Channel<NetworkResult<String>>()
    val changeUserEmailState: Flow<NetworkResult<String>> = _changeUserEmailState.receiveAsFlow()


    private val grocerlyDataStore = GrocerlyDataStore(application)

    private val _selectedString = MutableStateFlow<String>("")
    val selectedString: StateFlow<String?> = _selectedString

    fun setSelectedImage(uri: String) {
        _selectedString.value = uri
    }


    fun uploadProfileImage(uri: Uri){
        viewModelScope.launch {
            uploadImageToFirebase(uri)
        }
    }

    fun changeUserEmailOfUser(oldEmail:String,newEmail: String,password: String){
        viewModelScope.launch {
           if (isEmailValidated(oldEmail,newEmail,password)) {
               changeUserEmail(oldEmail,newEmail,password)
           }else{
               emitEmailValidationErrors(oldEmail,newEmail,password)
           }

        }
    }

    private fun isEmailValidated(
        oldEmail: String,
        newEmail: String,
        password: String
    ): Boolean {

        val isOldEmailValidated = validateEmail(oldEmail)
        val isNewEmailValidated  = validateEmail(newEmail)
        val isPasswordValidated = validatePassword(password)

        return isOldEmailValidated is RegisterValidation.Success && isNewEmailValidated is RegisterValidation.Success && isPasswordValidated is RegisterValidation.Success

    }

    private suspend fun emitEmailValidationErrors(
        oldEmail: String,
        newEmail: String,
        password: String
    ){

        val emailChangeState = EmailChangeFieldState(validateEmail(oldEmail),validatePassword(password),validateEmail(newEmail))
        _emailChangeValidation.send(emailChangeState)
    }


    init {
        fetchUserDetailsFromFirebase()
    }


    fun fetchUserDetailsFromFirebase(){
        viewModelScope.launch {
            try {
                _accountDetails.emit(NetworkResult.Loading())
                val account = profileRepoImpl.fetchProfileDetails()
                _accountDetails.emit(account)
            }catch (e: Exception){
                _accountDetails.emit(NetworkResult.Error(e.message))
            }
        }
    }

    fun updateUserDetailsToFirebase(account: Account){
        viewModelScope.launch {
            if (isDetailsValidated(account)){
                _updatedDetails.send(AccountResult.Loading())
                val updated = profileRepoImpl.updateProfileDetailsFirebase(account)
                _updatedDetails.send(updated)
                Log.d("Updateddetails",updated.data.toString())
            }else{
                emitValidationErrors(account)
            }
        }
    }

    private suspend fun emitValidationErrors(account: Account) {
        val state = ProfileFieldState(validateName(account.firstName),validateName(account.lastName),validateEmail(account.email),validatePhoneNumber(account.phoneNumber))
        _profileValidation.send(state)
    }


    fun isDetailsValidated(account: Account): Boolean{
        val firstname = validateName(account.firstName)
        val lastname = validateName(account.lastName)
        val email = validateEmail(account.email)
        val phoneNo  = validatePhoneNumber(account.phoneNumber)
        return firstname is RegisterValidation.Success && lastname is RegisterValidation.Success && email is RegisterValidation.Success && phoneNo is RegisterValidation.Success

    }




    fun LogOutUserFromFirebase(){
       viewModelScope.launch {
           enableLogout()
       }
    }

    private suspend fun enableLogout() {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            _logoutState.emit(NetworkResult.Loading())
            val isSignedOut = profileRepoImpl.enableLogout()
            val loginState = grocerlyDataStore.getLoginState().first()
            if (isSignedOut && !loginState){
                _logoutState.emit(NetworkResult.Success("Logged Out"))
            }

        }else{
            _logoutState.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }

    }

    private suspend fun uploadImageToFirebase(uri: Uri) {

        if (NetworkUtils.isNetworkAvailable(getApplication())) {
             profileRepoImpl.uploadImageToFirebaseImpl(uri).collectLatest {
                _uploadImageState.emit(it)
            }
            Log.d("imagestate",uploadImageState.toString())

        } else {
            _uploadImageState.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
        }

    }


    private suspend fun changeUserEmail(oldEmail: String, newEmail: String, password: String) {
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            _changeUserEmailState.send(NetworkResult.Loading())
            val reauthenticateState = profileRepoImpl.reAuthenticateAndChangeEmailSuspend(oldEmail, password, newEmail)
            _changeUserEmailState.send(reauthenticateState)
        } else {
            _changeUserEmailState.send(NetworkResult.Error("Enable Wifi or Mobile data"))
        }
    }

}