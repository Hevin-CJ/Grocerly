package com.example.grocerly.Repository.remote

import android.net.Uri
import android.util.Log
import com.example.grocerly.Repository.local.ProfileLocalRepoImpl
import com.example.grocerly.googleclient.GoogleSignInClientRepoImpl
import com.example.grocerly.model.Account
import com.example.grocerly.preferences.GrocerlyDataStore
import com.example.grocerly.utils.AccountResult
import com.example.grocerly.utils.Constants.ACCOUNTS
import com.example.grocerly.utils.Constants.FIREBASE_DOMAIN
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.Mappers.toProfileEntity
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

@ActivityRetainedScoped
class ProfileRepoImpl @Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth,private val storage: FirebaseStorage,private val grocerlyDataStore: GrocerlyDataStore,private val googleSignInClientRepoImpl: GoogleSignInClientRepoImpl,private val profileLocalRepoImpl: ProfileLocalRepoImpl) {

    private val userId = auth.currentUser?.uid.toString()
    private val profileRef = db.collection(ACCOUNTS).document(userId)


    suspend fun fetchProfileDetails(): NetworkResult<Unit>{
       return try {
            if (userId.isBlank()){
                return NetworkResult.Error("User Not Found")
            }

           val snapshot = profileRef.get().await()
           val account = if (snapshot.exists()) {
               snapshot.toObject(Account::class.java) ?: createAccountIfNotExists(userId)
           } else {
               createAccountIfNotExists(userId)
           }
           Log.d("accountgetting",account.toString())
           profileLocalRepoImpl.upsertProfile(account.toProfileEntity())
           NetworkResult.Success(Unit)

        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }

    private fun createAccountIfNotExists(userId: String): Account {
        val currentUser = auth.currentUser
        return Account(
            userId = userId,
            firstName = currentUser?.displayName ?: "",
            lastName = "",
            email = currentUser?.email ?: "",
            imageUrl = currentUser?.photoUrl?.toString() ?: "",
            countryCode = "+91",
            phoneNumber = currentUser?.phoneNumber ?: ""
        )
    }


    suspend fun updateProfileDetailsFirebase(account: Account): AccountResult<String> = suspendCancellableCoroutine {  continuation->

        val userEmail = auth.currentUser?.email.toString()

        if (account.userId.isEmpty()){
             continuation.resume(AccountResult.Error("User Not found"))
            return@suspendCancellableCoroutine
        }

        if (account.email.isNotEmpty() && account.email!=userEmail)  {
            continuation.resume(AccountResult.EmailUpdated(account.email))
            return@suspendCancellableCoroutine
        }


            val accountRef = db.collection(ACCOUNTS).document(account.userId)
            val task = accountRef.update(account.toHashMap())

            task.addOnSuccessListener {
                if (continuation.isActive){
                    continuation.resume(AccountResult.Success("Updated"))
                }

            }.addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(AccountResult.Error(it.message))
                }
            }

            continuation.invokeOnCancellation {
                if (!task.isComplete){
                    task.isCanceled
                }
            }

    }


     fun uploadImageToFirebaseImpl(uri: Uri): Flow<NetworkResult<String>> = flow {
         try {
             emit(NetworkResult.Loading())

            if (uri.path.isNullOrEmpty()){
                emit(NetworkResult.Error("Image path not found"))
                return@flow
            }

            val userId = auth.currentUser?.uid.toString()

            if (userId.isEmpty()){
                emit(NetworkResult.Error("User not found"))
                return@flow
            }

            val storageRef = storage.reference.child("$USERS/$userId/images/${UUID.randomUUID()}.jpg")
            val uploadTaskSnapshot = storageRef.putFile(uri).await()
            val imageUrl = uploadTaskSnapshot.storage.downloadUrl.await().toString()
             emit(NetworkResult.Success(imageUrl))


        }catch (e: Exception){
             emit(NetworkResult.Error(e.message))
        }
    }


    suspend fun reAuthenticateAndChangeEmailSuspend(
        oldEmail: String,
        password: String,
        newEmail: String
    ): NetworkResult<String> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext NetworkResult.Error("User not logged in")

        return@withContext try {
            user.reauthenticate(EmailAuthProvider.getCredential(oldEmail, password)).await()
            user.verifyBeforeUpdateEmail(newEmail, getActionCodeSettings()).await()
            NetworkResult.Success("Verification email sent to $newEmail")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to update email")
        }
    }





    suspend fun enableLogout(): Boolean = withContext(Dispatchers.Main) {
       return@withContext try {
           checkAndLogoutFromProviders()
           auth.signOut()
           grocerlyDataStore.clearAll()
           true
       }catch (e: Exception){
           e.printStackTrace()
           false
       }
    }

    private suspend fun checkAndLogoutFromProviders() {
        val provider = getLoginProviders()
       when(provider){
           GoogleAuthProvider.PROVIDER_ID -> {
               googleSignInClientRepoImpl.signOut()
           }
       }
    }

    private fun getLoginProviders(): String? {
        val user = auth.currentUser ?: return null
        val providerId = user.providerData.lastOrNull()?.providerId
        return providerId
    }


    @Suppress("DEPRECATION")
    private fun getActionCodeSettings(): ActionCodeSettings {
        return actionCodeSettings {
            url = FIREBASE_DOMAIN
            handleCodeInApp = false
            setIOSBundleId("com.example.ios")
            setAndroidPackageName("com.example.grocerly", true, "1")
            dynamicLinkDomain = "grocerlymail.page.link"
        }
    }

    private fun Account.toHashMap(): HashMap<String, Any?> {
        return hashMapOf(
            "userId" to userId,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "imageUrl" to imageUrl,
            "countryCode" to countryCode,
            "phoneNumber" to phoneNumber
        )
    }
}
