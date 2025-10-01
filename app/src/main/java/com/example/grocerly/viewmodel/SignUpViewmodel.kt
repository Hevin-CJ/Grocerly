package com.example.grocerly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.SignUpRepoImpl
import com.example.grocerly.model.Account
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.RegisterFieldState
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.utils.validateEmail
import com.example.grocerly.utils.validateName
import com.example.grocerly.utils.validatePassword
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewmodel @Inject constructor(application: Application,private val signUpRepoImpl: SignUpRepoImpl):AndroidViewModel(application) {

    private var _isSigned = MutableStateFlow<NetworkResult<FirebaseUser>>(NetworkResult.UnSpecified())
    val isSigned: Flow<NetworkResult<FirebaseUser>> get() = _isSigned

    private var _validationState = Channel<RegisterFieldState>()
    var validationState:Flow<RegisterFieldState> = _validationState.receiveAsFlow()


    fun createUser(account: Account,password:String){
         viewModelScope.launch {
             if (validationChecker(account.firstName,account.lastName, account.email, password)) {
                 performUserSignUp(account, password)
             } else {
                 emitValidationErrors(account.firstName, account.lastName,account.email, password)
             }
         }
    }

    private suspend fun performUserSignUp(account: Account,password: String){
        _isSigned.value = NetworkResult.Loading()
        val firebaseUser = signUpRepoImpl.performSignUpAndSaveUserDetails(account,password)
        _isSigned.value = firebaseUser
    }



    private fun validationChecker(name:String,lastname:String,email: String, password: String): Boolean {
        val isFirstNameValidated = validateName(name)
        val isLastNameValidated = validateName(lastname)
        val isEmailValidated = validateEmail(email)
        val isPasswordValidated = validatePassword(password)
        val isValidated = isFirstNameValidated is RegisterValidation.Success && isLastNameValidated is RegisterValidation.Success && isEmailValidated is RegisterValidation.Success && isPasswordValidated is RegisterValidation.Success
        return isValidated

    }

    private suspend fun emitValidationErrors(name: String,lastname: String, email: String, password: String) {
        val state = RegisterFieldState(
            validateName(name),validateName(lastname),validateEmail(email),validatePassword(password)
        )
        _validationState.send(state)
    }

}