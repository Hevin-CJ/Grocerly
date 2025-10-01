package com.example.grocerly.Repository.remote

import com.example.grocerly.model.Card
import com.example.grocerly.utils.Constants.SAVED_CARDS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SavedCardsRepoImpl @Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {

    val userId = auth.currentUser?.uid.toString()

    private val savedCardsRef = db.collection(USERS).document(userId).collection(SAVED_CARDS)

    suspend fun saveCardToFirebase(card: Card): NetworkResult<String>{
      return try {

         if (userId.isEmpty()) return NetworkResult.Error("User Not found,Please Login again...")

          val docRef = savedCardsRef.document()
         val cardWithId = card.copy(cardId = docRef.id)

          docRef.set(cardWithId).await()

          NetworkResult.Success("Your Card Has Been Added Successfully")
     }catch (e: Exception){
         NetworkResult.Error(e.message.toString())
     }
    }

    suspend fun updateCardToFirebase(card: Card): NetworkResult<String>{
        return try {
            if (userId.isEmpty()) return NetworkResult.Error("User Not found,Please Login again...")

            savedCardsRef.document(card.cardId).update(card.toMap()).await()
            NetworkResult.Success("Your Card Has Been Updated Successfully")
        }catch (e: Exception){
            NetworkResult.Error(e.message.toString())
        }
    }


    suspend fun deleteCardFromFirebase(card: Card): NetworkResult<String>{
        return try {
            if (userId.isEmpty()) return NetworkResult.Error("User Not found,Please Login again...")

            savedCardsRef.document(card.cardId).delete().await()

            val last4No = card.cardNumber.takeLast(4)
            NetworkResult.Success("Card Ending $last4No has been Deleted Successfully ")

        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }

    fun Card.toMap():Map<String, Any>{
        return mapOf(
            "cardId" to cardId,
            "holderName" to holderName,
            "cardNumber" to cardNumber,
            "expiryDate" to expiryDate,
            "cvv" to cvv,
            "cardType" to cardType
        )


    }

    fun getAllSavedCardsFromFirebase(): Flow<NetworkResult<List<Card>>> = callbackFlow {

           val listener = savedCardsRef.addSnapshotListener { snapshot,exception ->

               if (exception!=null){
                   trySend(NetworkResult.Error(exception.message   ?: "Unable to fetch Cards,Please try later...")).isFailure
               }


               snapshot?.let {
                   val address = it.documents.mapNotNull { it.toObject(Card::class.java)}
                   trySend(NetworkResult.Success(address))
               }


           }

           awaitClose{
               listener.remove()
           }

    }

}