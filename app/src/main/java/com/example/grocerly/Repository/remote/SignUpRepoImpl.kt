package com.example.grocerly.Repository.remote

import com.example.grocerly.model.Account
import com.example.grocerly.utils.Constants.ACCOUNTS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SignUpRepoImpl @Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth)  {


    suspend fun performSignUpAndSaveUserDetails(account: Account,password: String): NetworkResult<FirebaseUser>{
        return try {

            val user = auth.createUserWithEmailAndPassword(account.email,password).await()
            val firebaseUser = user.user

            if (firebaseUser?.email.isNullOrEmpty()) {
                return NetworkResult.Error("Unable to Create Account, Please try later...")
            }

            val isSaved = saveUserDetailsToFirebase(account)

            if (isSaved){
               NetworkResult.Success(firebaseUser)
            }else{
               NetworkResult.Error("User registration failed.")
            }


        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }

    private suspend fun saveUserDetailsToFirebase(account: Account): Boolean {
        return try {

            val userId = auth.currentUser?.uid.toString() ?: return false
            val updatedProfile = account.copy(userId = userId)

            db.collection(ACCOUNTS)
                .document(userId)
                .set(updatedProfile)
                .await()
            true
        }catch (e: Exception){
            false
        }


    }

}