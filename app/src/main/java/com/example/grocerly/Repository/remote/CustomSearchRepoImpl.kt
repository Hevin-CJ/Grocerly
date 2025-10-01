package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.ProductCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

@ActivityRetainedScoped
class CustomSearchRepoImpl @Inject constructor(private val db: FirebaseFirestore) {

    fun searchByCategory(categoryName: ProductCategory): Flow<NetworkResult<List<Product>>> = callbackFlow {

        val baseQuery: Query = db.collectionGroup(PRODUCTS)

        val finalQuery = if (categoryName == ProductCategory.selectcatgory) {
            baseQuery
        } else {
           baseQuery.whereEqualTo("category", categoryName.name)
        }


        val listener = finalQuery.addSnapshotListener { snapshot, error ->

            if (error != null) {
                trySend(NetworkResult.Error(error.message))
                return@addSnapshotListener
            }


            if (snapshot == null||snapshot.isEmpty){
                trySend(NetworkResult.Success(emptyList()))
                return@addSnapshotListener
            }

            val productList = snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
            trySend(NetworkResult.Success(productList))
            Log.d("productlist",productList.toString())
        }
        awaitClose {
            listener.remove()
        }
    }


}