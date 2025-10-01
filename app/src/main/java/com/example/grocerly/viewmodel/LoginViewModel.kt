package com.example.grocerly.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: FirebaseAuth, private val db: FirebaseFirestore,private val grocerlyDataStore: GrocerlyDataStore,private val googleSignInRepo: GoogleSignInClientRepoImpl,application: Application): AndroidViewModel(application) {



    private val _loginstate = MutableSharedFlow<NetworkResult<FirebaseUser>>()
    val loginstate : Flow<NetworkResult<FirebaseUser>> get() = _loginstate.asSharedFlow()

    private var _validationState = Channel<LoginRegisterFieldState>()
    val validationState:Flow<LoginRegisterFieldState> get() = _validationState.receiveAsFlow()

    fun setLoginState(loginstate:Boolean){
        viewModelScope.launch {
            grocerlyDataStore.setLoginState(loginstate)
        }
    }

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
           try {
              val user =  googleSignInRepo.signIn()
               handleSuccessfulLogin(user)
           }catch (e: Exception){
               Log.d("errorgoogle",e.message.toString())
               _loginstate.emit(NetworkResult.Error(FirebaseErrorMapper.getUserMessage(e)))
           }
        }
    }

    fun signInWithX(activity: Activity){
        viewModelScope.launch {
           startSignUpWithX(activity)
        }
    }

    private suspend fun startSignUpWithX(activity: Activity) {
      if (NetworkUtils.isNetworkAvailable(getApplication())){
        try {
            val provider = OAuthProvider.newBuilder("twitter.com").build()
            val authResult =  auth.startActivityForSignInWithProvider(activity,provider).await()
            authResult.user?.let {
                handleSuccessfulLogin(it)
            }?:_loginstate.emit(NetworkResult.Error("Twitter login failed:User not found"))
        }catch (e: Exception){
            _loginstate.emit(NetworkResult.Error(e.message.toString()))
            Log.d("errortwitter",e.message.toString())
        }
      }else{
          _loginstate.emit(NetworkResult.Error("Enable Wifi or Mobile data"))
      }
    }


    fun signInWithFacebook(token: AccessToken){
        viewModelScope.launch {
            try {
                _loginstate.emit(NetworkResult.Loading())
                val credential = FacebookAuthProvider.getCredential(token.token)
                val result = auth.signInWithCredential(credential).await()
                result.user?.let {
                    handleSuccessfulLogin(it)
                } ?: _loginstate.emit(NetworkResult.Error("User not found."))

            } catch (e: Exception) {
                _loginstate.emit(NetworkResult.Error(FirebaseErrorMapper.getUserMessage(e)))
            }
        }
    }

    private suspend fun handleSuccessfulLogin(user: FirebaseUser) {
        try {
            val userId = user.uid
            val userEmail = user.email
            val sessionToken = UUID.randomUUID().toString()

            Log.d("LoginViewModel", "Handling successful login for user: ${user.email}")

            val accountDocRef = db.collection(ACCOUNTS).document(userId)
            if (accountDocRef.get().await().exists()) {
                accountDocRef.update("email", userEmail).await()
            }


            val sessionData = mapOf("sessionToken" to sessionToken)
            db.collection(USERS).document(userId).set(sessionData, SetOptions.merge()).await()

            grocerlyDataStore.setSessionToken(sessionToken)
            setLoginState(true)


            _loginstate.emit(NetworkResult.Success(user))

        } catch (e: Exception) {
            Log.e("LoginViewModel", "Post-login Firestore/DataStore Exception: ${e.message}")
            _loginstate.emit(NetworkResult.Error("Failed to finalize session: ${FirebaseErrorMapper.getUserMessage(e)}"))
        }
    }



    private suspend fun performLoginUser(email: String,password: String){

        try {
            _loginstate.emit(NetworkResult.Loading())
            val result = auth.signInWithEmailAndPassword(email,password).await()
            result.user?.let {
                handleSuccessfulLogin(it)
            }?: _loginstate.emit(NetworkResult.Error("User authentication failed."))
        }catch (e: Exception){
            _loginstate.emit(NetworkResult.Error(FirebaseErrorMapper.getUserMessage(e)))
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