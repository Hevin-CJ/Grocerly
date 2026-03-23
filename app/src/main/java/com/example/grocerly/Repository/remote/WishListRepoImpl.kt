package com.example.grocerly.Repository.remote

import com.example.grocerly.model.CartProduct
import com.example.grocerly.model.WishItem
import com.example.grocerly.utils.Constants
import com.example.grocerly.utils.Constants.CART
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.Constants.WISHLIST
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
class WishListRepoImpl @Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {

    suspend fun addItemToWishList(wishItem: WishItem): NetworkResult<Unit>{
        return try {

            val userId = auth.currentUser?.uid ?: return NetworkResult.Error("User not found")

            val wishListRef = db.collection(USERS).document(userId).collection(WISHLIST)

            val itemToSave = wishItem.copy(id = wishItem.item.productId)

            wishListRef.document(wishItem.item.productId).set(itemToSave).await()

            NetworkResult.Success(Unit)
        }catch (e: Exception){
            NetworkResult.Error(e.message ?: "Unknown Error Occurred")
        }
    }

    fun getWishListItems(): Flow<NetworkResult<List<WishItem>>> = callbackFlow {
        trySend(NetworkResult.Loading())

        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(NetworkResult.Error("User not found"))
            close()
            return@callbackFlow
        }

        val listener = db.collection(USERS).document(userId).collection(WISHLIST).addSnapshotListener{ snapshot, exception ->

            if (exception!=null){
                trySend(NetworkResult.Error(exception.message ?: "Unknown Error Occurred"))
                return@addSnapshotListener

            }

            snapshot?.let {
                val wishListItems = it.documents.mapNotNull { it.toObject(WishItem::class.java) }
                trySend(NetworkResult.Success(wishListItems))
            }

        }

        awaitClose {
            listener.remove()
        }

    }

    suspend fun addWishListItemToCart(wishItem: WishItem): NetworkResult<Unit>{
        return try {
            val userId = auth.currentUser?.uid ?: return NetworkResult.Error("User not found")

            val cartRef = db.collection(USERS).document(userId).collection(CART).document(wishItem.item.productId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(cartRef)

                if (snapshot.exists()){
                    val currentCartItem = snapshot.toObject(CartProduct::class.java)

                    throw Exception("Item ${currentCartItem?.product?.itemName} is already in cart")
                }else{
                    val newCartItem = CartProduct(product = wishItem.item, quantity = 1)
                    transaction.set(cartRef, newCartItem)
                }
                null
            }.await()

            NetworkResult.Success(Unit)

        }catch (e: Exception){
            NetworkResult.Error(e.message ?: "Failed to add to cart")
        }
    }

    suspend fun removeItemFromWishList(wishItem: WishItem): NetworkResult<Unit>{
        return try {
            val userId = auth.currentUser?.uid ?: return NetworkResult.Error("User not found")

            val wishListRef = db.collection(USERS).document(userId).collection(WISHLIST).document(wishItem.item.productId)

            wishListRef.delete().await()

            NetworkResult.Success(Unit)

        }catch (e: Exception){
            NetworkResult.Error(e.message ?: "Failed to add to cart")
        }
    }

}