package com.example.grocerly.Repository.remote

import android.util.Log
import androidx.compose.ui.text.toUpperCase
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Constants.PARTNERS
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject

@ActivityRetainedScoped
class SearchRepoImpl@Inject constructor(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {

    fun searchProduct(productName: String): Flow<NetworkResult<List<Product>>> = callbackFlow {

        trySend(NetworkResult.Loading())

        val queryText = productName.lowercase(Locale.getDefault()).trim()

        var query = db.collectionGroup(PRODUCTS)

        if (queryText.isNotBlank()) {
           query =  query.whereArrayContains("searchKeywords", queryText)
        }

        val listener = query.limit(20).addSnapshotListener { snapshot, error ->

                if (error != null) {
                    trySend(NetworkResult.Error(error.message))
                    Log.d("search-error", error.message.toString())
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val productList =
                        snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
                    trySend(NetworkResult.Success(productList))
                }


            }
        awaitClose {
            listener.remove()
        }
    }

}