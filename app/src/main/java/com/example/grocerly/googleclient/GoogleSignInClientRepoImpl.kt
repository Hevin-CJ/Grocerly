package com.example.grocerly.googleclient

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.grocerly.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ActivityRetainedScoped
class GoogleSignInClientRepoImpl  @Inject constructor (private val context: Context,private val auth: FirebaseAuth) {

    private val  googleTag = "google_sign_client"

    private val credentialManager = CredentialManager.create(context)

    fun isSignedIn(): Boolean{
        if (auth.currentUser != null) {
            println(googleTag + "already signed in")
            return true
        }

        return false
    }

    suspend fun signIn(): FirebaseUser{
        if (isSignedIn()) {

            auth.currentUser?.let { return it }
        }
        val result = buildCredentialRequest()
        return handleSignIn(result)

    }

    private suspend fun handleSignIn(result: GetCredentialResponse): FirebaseUser {
        val credential = result.credential

        if (credential is CustomCredential &&  credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
            try {

                val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                println(googleTag + "name: ${tokenCredential.displayName}")
                println(googleTag + "email: ${tokenCredential.id}")
                println(googleTag + "image: ${tokenCredential.profilePictureUri}")

                val authCredential = GoogleAuthProvider.getCredential(
                    tokenCredential.idToken, null
                )
                val authResult = auth.signInWithCredential(authCredential).await()

                return authResult.user ?: throw Exception("Firebase authentication failed: user is null.")

            } catch (e: GoogleIdTokenParsingException) {
                println(googleTag + "GoogleIdTokenParsingException: ${e.message}")
                throw e
            }
        }else {
            println(googleTag + "credential is not GoogleIdTokenCredential")
            throw Exception("Sign-in failed: incorrect credential type.")
        }
    }

    private suspend fun buildCredentialRequest(): GetCredentialResponse{
        val serverClientId = context.getString(R.string.google_server_client_id)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(serverClientId).setAutoSelectEnabled(false).build())
            .build()
        return CredentialManager.create(context).getCredential(request = request,context =context)
    }

    suspend fun signOut(){
        credentialManager.clearCredentialState(
            request = ClearCredentialStateRequest()
        )
        auth.signOut()
    }

}