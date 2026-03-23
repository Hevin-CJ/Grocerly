package com.example.grocerly.Repository.remote

import android.app.Activity
import com.example.grocerly.googleclient.GoogleSignInClientRepoImpl
import com.example.grocerly.utils.NetworkResult
import com.facebook.AccessToken
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ActivityRetainedScoped
class SocialAuthRepoImpl @Inject constructor(private val db: FirebaseFirestore, private val auth: FirebaseAuth, private val googleSignInClientRepoImpl: GoogleSignInClientRepoImpl){


    suspend fun signInWithGoogle(): NetworkResult<FirebaseUser> {
        return try {
            val user = googleSignInClientRepoImpl.signIn()
            NetworkResult.Success(user)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Google Sign In Failed")
        }
    }

    fun firebaseSignInWithFacebook(token: AccessToken): Flow<NetworkResult<FirebaseUser>> = flow {
        try {
            emit(NetworkResult.Loading())
            val credential = FacebookAuthProvider.getCredential(token.token)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                emit(NetworkResult.Success(user))
            } else {
                emit(NetworkResult.Error("Firebase authentication failed"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Facebook Login Failed"))
        }
    }


    fun firebaseSignInWithTwitter(activity: Activity): Flow<NetworkResult<FirebaseUser>> =
        callbackFlow {
            trySend(NetworkResult.Loading())

            val provider = OAuthProvider.newBuilder("twitter.com")
            val pendingResultTask = auth.pendingAuthResult

            val onSuccess: (AuthResult) -> Unit = { result: AuthResult ->
                val user = result.user
                if (user != null) {
                    trySend(NetworkResult.Success(user))
                } else {
                    trySend(NetworkResult.Error("User not found"))
                }
                close()
            }

            val onFailure: (Exception) -> Unit = { e: Exception ->
                trySend(NetworkResult.Error(e.message ?: "Twitter Login Failed"))
                close()
            }

            if (pendingResultTask != null) {
                pendingResultTask.addOnSuccessListener(onSuccess).addOnFailureListener(onFailure)
            } else {
                auth.startActivityForSignInWithProvider(activity, provider.build())
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure)
            }
            awaitClose { }
        }


}