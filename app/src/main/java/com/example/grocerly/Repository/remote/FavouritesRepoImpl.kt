package com.example.grocerly.Repository.remote

import com.example.grocerly.model.FavouriteItem
import com.example.grocerly.utils.Constants.FAVOURITES
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ActivityRetainedScoped
class FavouritesRepoImpl @Inject constructor (private val db: FirebaseFirestore,private val auth: FirebaseAuth) {

    val userId = auth.currentUser?.uid.toString()

    private val favouriteRef = db.collection(USERS).document(userId).collection(FAVOURITES)

   suspend fun addToFavouritesFirebase(favouriteItem: FavouriteItem):NetworkResult<FavouriteItem>{
       return try {

            val favouriteDocRef = favouriteRef.document(favouriteItem.product.productId)

            val existingSnapshot = favouriteDocRef
                .get()
                .await()

            if (existingSnapshot.exists()) {
               return NetworkResult.Error("Your Item (${favouriteItem.product.itemName}) Already \n Exists In Favourites")
            }

            val updatedItem = favouriteItem.copy(favouriteId = favouriteItem.product.productId)
            favouriteDocRef.set(updatedItem).await()

          NetworkResult.Success(favouriteItem)

        }catch (e:Exception){
            NetworkResult.Error(e.message)

        }

   }


    fun fetchAllFavourites(): Flow<NetworkResult<List<FavouriteItem>>> = callbackFlow {

        val listener = favouriteRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                trySend(
                    NetworkResult.Error(
                        exception.message ?: "Error fetching favourites"
                    )
                ).isFailure
                return@addSnapshotListener
            }

            snapshot?.let {
                val favourites = it.documents.mapNotNull { doc -> doc.toObject(FavouriteItem::class.java) }
                trySend(NetworkResult.Success(favourites))
            }
        }

        awaitClose { listener.remove() }

    }


    suspend fun deleteFavourite(favouriteItem: FavouriteItem): NetworkResult<String>{
        return try {
            favouriteRef
                .document(favouriteItem.favouriteId)
                .delete()
                .await()

            NetworkResult.Success("Deleted ${favouriteItem.product.itemName}")

        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }

    private fun FavouriteItem.toHashMap(): HashMap<String, Any?> {
        return hashMapOf(
            "favouriteId" to product.productId,
            "product" to product
        )
    }
}