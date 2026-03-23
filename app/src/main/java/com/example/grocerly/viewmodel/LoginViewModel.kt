package com.example.grocerly.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.grocerly.Repository.remote.SocialAuthRepoImpl
import com.example.grocerly.Repository.remote.LoginRepoImpl
import com.example.grocerly.activity.MainActivity
import com.example.grocerly.googleclient.GoogleSignInClientRepoImpl
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.Constants.ACCOUNTS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.FirebaseErrorMapper
import com.example.grocerly.utils.LoginRegisterFieldState
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.utils.validateEmail
import com.example.grocerly.utils.validatePassword
import com.facebook.AccessToken
import com.facebook.AccessTokenManager
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class  LoginViewModel @Inject constructor(private val googleSignInRepo: GoogleSignInClientRepoImpl,private val socialAuthRepoImpl: SocialAuthRepoImpl,private val loginRepoImpl: LoginRepoImpl,application: Application): AndroidViewModel(application) {



    private val _loginstate = MutableSharedFlow<NetworkResult<FirebaseUser>>()
    val loginstate : Flow<NetworkResult<FirebaseUser>> get() = _loginstate.asSharedFlow()

    private var _validationState = Channel<LoginRegisterFieldState>()
    val validationState:Flow<LoginRegisterFieldState> get() = _validationState.receiveAsFlow()



    fun loginUserIntoFirebase(email: String,password: String){

        viewModelScope.launch{
            if (validationChecker(email,password)){
                performLoginUser(email,password)
            }else{
                emitValidationErrors(email,password)
            }
        }
    }

    fun signInWithGoogle(){
        viewModelScope.launch {
            handleGoogleLogin()
        }
    }

    private suspend fun handleGoogleLogin() {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            val result = socialAuthRepoImpl.signInWithGoogle()
            handleAuthResult(result)
        }else{
            _loginstate.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }

    fun signInWithX(activity: Activity){
        viewModelScope.launch {
           startSignUpWithX(activity)
        }
    }

    private suspend fun startSignUpWithX(activity: Activity) {
      if (NetworkUtils.isNetworkAvailable(getApplication())) {
          socialAuthRepoImpl.firebaseSignInWithTwitter(activity).collectLatest {result ->
              when (result) {
                  is NetworkResult.Success -> {
                     handleAuthResult(result)
                  }
                  is NetworkResult.Error -> _loginstate.emit(NetworkResult.Error(result.message))
                  is NetworkResult.Loading -> _loginstate.emit(NetworkResult.Loading())
                  else -> Unit
              }
          }
      }
    }


    fun signInWithFacebook(token: AccessToken){
        viewModelScope.launch {
           handleFacebookLogin(token)
        }
    }

    private suspend fun handleFacebookLogin(token: AccessToken) {
        if (NetworkUtils.isNetworkAvailable(getApplication())){
            socialAuthRepoImpl.firebaseSignInWithFacebook(token).collectLatest {result->
                handleAuthResult(result = result)
            }
        }else{
            _loginstate.emit(NetworkResult.Error("Enable Wifi or Mobile Data"))
        }
    }


    private suspend fun handleAuthResult(result: NetworkResult<FirebaseUser>) {
        when (result) {
            is NetworkResult.Success -> {
                result.data?.let { user ->
                    finalizeLogin(user)
                }
            }

            is NetworkResult.Error -> {
                _loginstate.emit(NetworkResult.Error(result.message))
            }

            is NetworkResult.Loading -> {
                _loginstate.emit(NetworkResult.Loading())
            }

            else -> Unit
        }
    }

    private suspend fun finalizeLogin(user: FirebaseUser) {
        val result = loginRepoImpl.saveUserSession(user)
        _loginstate.emit(result)
    }



    private suspend fun performLoginUser(email: String,password: String){
        _loginstate.emit(NetworkResult.Loading())
        val loginResult = loginRepoImpl.makeFirebaseLogin(email, password)

        when (loginResult) {
            is NetworkResult.Success -> {

                loginResult.data?.let { user ->
                    finalizeLogin(user)
                }
            }
            is NetworkResult.Error -> {
                _loginstate.emit(NetworkResult.Error(loginResult.message))
            }
            else -> Unit
        }
    }



    private fun validationChecker(email: String, password: String): Boolean {
        val isEmailValidated = validateEmail(email)
        val isPasswordValidated = validatePassword(password)
        val isValidated = isEmailValidated is RegisterValidation.Success && isPasswordValidated is RegisterValidation.Success
        return isValidated

    }

    private suspend fun emitValidationErrors(email: String, password: String) {
        val state = LoginRegisterFieldState(
           validateEmail(email),validatePassword(password)
        )
        _validationState.send(state)
    }

}