package com.example.grocerly.Repository.remote

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.grocerly.googleclient.GoogleSignInClientRepoImpl
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.Constants.ACCOUNTS
import com.example.grocerly.utils.Constants.FCM_TOKEN
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.FirebaseErrorMapper
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@ActivityRetainedScoped
class LoginRepoImpl @Inject constructor(private val auth: FirebaseAuth,
                                        private val db: FirebaseFirestore,
                                        private val grocerlyDataStore: GrocerlyDataStore,
    private val messaging: FirebaseMessaging
) {



    suspend fun makeFirebaseLogin(email: String, password: String): NetworkResult<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email,password).await()
            result.user?.let {
                NetworkResult.Success(it)
            }?: NetworkResult.Error("Something Went Wrong,User Authentication Failed")
        }catch(e: Exception){
            NetworkResult.Error(e.message ?: "Login Failed")
        }
    }


    suspend fun saveUserSession(user: FirebaseUser): NetworkResult<FirebaseUser> {
       return try {
            val userId = user.uid
            val userEmail = user.email
            val sessionToken = UUID.randomUUID().toString()

            Log.d("LoginViewModel", "Handling successful login for user: ${user.email}")

            val accountDocRef = db.collection(ACCOUNTS).document(userId)
            val snapshot = accountDocRef.get().await()

            if (snapshot.exists()) {
                accountDocRef.update("email", userEmail).await()
            }else{
                val newAccountData = hashMapOf(
                    "userId" to userId,
                    "email" to userEmail,
                    "firstName" to (user.displayName ?: ""),
                    "imageUrl" to (user.photoUrl?.toString() ?: "")
                )
                accountDocRef.set(newAccountData).await()
            }


            val sessionData = mapOf("sessionToken" to sessionToken)
            db.collection(USERS).document(userId).set(sessionData, SetOptions.merge()).await()

            grocerlyDataStore.setSessionToken(sessionToken)
            grocerlyDataStore.setLoginState(true)

           NetworkResult.Success(user)

        } catch (e: Exception) {
            Log.e("LoginViewModel", "Post-login Firestore/DataStore Exception: ${e.message}")
           NetworkResult.Error("Failed to save session: ${e.message}")
        }
    }


}