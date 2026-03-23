package com.example.grocerly.Repository.remote

import com.example.grocerly.Repository.local.ProfileLocalRepoImpl
import com.example.grocerly.googleclient.GoogleSignInClientRepoImpl
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.NetworkResult
import com.facebook.login.LoginManager
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ActivityRetainedScoped
class LogoutRepoImpl @Inject constructor(private val auth: FirebaseAuth,private val db: FirebaseFirestore,private val grocerlyDataStore: GrocerlyDataStore,private val googleSignInClientRepoImpl: GoogleSignInClientRepoImpl,private val profileLocalRepoImpl: ProfileLocalRepoImpl) {

    suspend fun enableLogout(): NetworkResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            checkAndLogoutFromProviders()
            auth.signOut()

            grocerlyDataStore.clearAll()
            profileLocalRepoImpl.deleteProfile()

            NetworkResult.Success("Logged out")
        }catch (e: Exception){
            e.printStackTrace()
            NetworkResult.Error(e.message.toString())
        }
    }

    private suspend fun checkAndLogoutFromProviders() {
        val user = auth.currentUser ?: return

        val providers = user.providerData.map { it.providerId }

        if (providers.contains(GoogleAuthProvider.PROVIDER_ID)) {
            googleSignInClientRepoImpl.signOut()
        }

        if (providers.contains(FacebookAuthProvider.PROVIDER_ID)) {
            LoginManager.getInstance().logOut()
        }
    }
}