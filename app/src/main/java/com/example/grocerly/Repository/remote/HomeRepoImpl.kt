package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.Repository.local.CategoryLocalRepoImpl
import com.example.grocerly.Repository.local.OfferLocalRepoImpl
import com.example.grocerly.model.Address
import com.example.grocerly.model.Category
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.Product
import com.example.grocerly.room.dao.ProductDao
import com.example.grocerly.utils.Constants.ADDRESS
import com.example.grocerly.utils.Constants.OFFERS
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.Mappers.toCategoryEntity
import com.example.grocerly.utils.Mappers.toDomainModelList
import com.example.grocerly.utils.Mappers.toEntityList
import com.example.grocerly.utils.Mappers.toOfferEntityList
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.ProductCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@ActivityRetainedScoped
class HomeRepoImpl @Inject constructor(private val auth: FirebaseAuth,private val db:FirebaseFirestore,private val addressRepoImpl: SavedAddressRepoImpl,private val categoryLocalRepoImpl: CategoryLocalRepoImpl,private val productDao: ProductDao) {



    fun getProductsFlow(): Flow<NetworkResult<List<ParentCategoryItem>>> {
        return productDao.getAllProducts().map { products ->
            if (products.isEmpty()) {
                NetworkResult.Loading()
            } else {
                val groupedProducts = products.groupBy { it.category }
                val categories = groupedProducts.map { (category, prods) ->
                    ParentCategoryItem(
                        categoryName = category.displayName,
                        childCategoryItems = prods.toDomainModelList()
                    )
                }.sortedBy { it.categoryName }

                NetworkResult.Success(categories)
            }
        }
    }


    fun syncProductsFromNetwork() {
        try {
            val query = db.collectionGroup(PRODUCTS)
                .whereEqualTo("isEnabled", true)

            query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SyncError", "Failed to sync products: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val productsToUpsert = mutableListOf<Product>()
                val productIdsToDelete = mutableListOf<String>()

                for (change in snapshot.documentChanges) {
                    val product = change.document.toObject(Product::class.java)

                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            productsToUpsert.add(product)
                        }
                        DocumentChange.Type.REMOVED -> {
                            productIdsToDelete.add(product.productId)
                        }
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (productsToUpsert.isNotEmpty()) {
                            productDao.upsertProducts(productsToUpsert.toEntityList())
                        }

                        if (productIdsToDelete.isNotEmpty()) {
                            productDao.deleteProductsById(productIdsToDelete)
                            Log.d("SyncSuccess", "Deleted ${productIdsToDelete.size} products from Room")
                        }
                    } catch (e: Exception) {
                        Log.e("SyncError", "Room Database Error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncError", "Firestore Listener Setup Error: ${e.message}")
        }
    }

    fun fetchByCategoryFromFirebase(category: ProductCategory): Flow<NetworkResult<List<Product>>> = callbackFlow {

        val listener = db.collectionGroup(PRODUCTS).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NetworkResult.Error(error.message)).isFailure
                return@addSnapshotListener
            }

            snapshot?.let {
                val groupedProducts = it.toObjects(Product::class.java).filter {
                    it.category == category && it.isEnabled == true
                }


                trySend(NetworkResult.Success(groupedProducts))
            }



        }

        awaitClose{
            listener.remove()
        }
    }







    fun getCategoriesFlow(): Flow<NetworkResult<List<Category>>> {
        return categoryLocalRepoImpl.getCategories()
            .map { localEntities ->
                if (localEntities.isNotEmpty()) {
                    NetworkResult.Success(localEntities.map { it.toCategory() })
                } else {
                    NetworkResult.Loading()
                }
            }
    }


    suspend fun syncCategoriesFromNetwork() {
        try {
            val querySnapshot = db.collectionGroup("categories").get().await()

            if (!querySnapshot.isEmpty) {
                val fetchedCategories = querySnapshot.toObjects(Category::class.java).sortedBy { it.id }
                categoryLocalRepoImpl.upsertCategory(fetchedCategories.map { it.toCategoryEntity() })
            }
        } catch (e: Exception) {
            Log.e("CategorySync", "Failed to sync categories: ${e.message}")
        }
    }

     fun getCityAndState(): Flow<NetworkResult<String>> = callbackFlow{

         val userId = auth.currentUser?.uid.toString()

          val listener = db.collection(USERS).document(userId).collection(ADDRESS).addSnapshotListener { snapshot, error ->

              if (error!=null) {
                  trySend(NetworkResult.Error(error.message))
                  return@addSnapshotListener
              }

              if (snapshot==null||snapshot.isEmpty){
                  trySend(NetworkResult.Success("Add New Address"))
                  return@addSnapshotListener
              }

              val address = snapshot.documents.firstNotNullOf { it.toObject(Address::class.java) }
              Log.d("addressgot",address.toString())

              val formatedAddress = buildString {
                  append(address.city )
                  append(" , " )
                  append(address.state)
              }
              trySend(NetworkResult.Success(formatedAddress))
          }
         awaitClose {
             listener.remove()
         }

    }




}