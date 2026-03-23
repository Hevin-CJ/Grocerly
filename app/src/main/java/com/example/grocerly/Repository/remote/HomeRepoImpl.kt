package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.Repository.local.CategoryLocalRepoImpl
import com.example.grocerly.Repository.local.OfferLocalRepoImpl
import com.example.grocerly.model.Address
import com.example.grocerly.model.Category
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Constants.ADDRESS
import com.example.grocerly.utils.Constants.OFFERS
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.Constants.USERS
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.Mappers.toCategoryEntity
import com.example.grocerly.utils.Mappers.toOfferEntityList
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.ProductCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@ActivityRetainedScoped
class HomeRepoImpl @Inject constructor(private val auth: FirebaseAuth,private val db:FirebaseFirestore,private val addressRepoImpl: SavedAddressRepoImpl,private val categoryLocalRepoImpl: CategoryLocalRepoImpl,private val offerLocalRepoImpl: OfferLocalRepoImpl) {



     fun fetchProductFromFirebase(): Flow<NetworkResult<List<ParentCategoryItem>>> = callbackFlow {

         trySend(NetworkResult.Loading())


         val query = db.collectionGroup(PRODUCTS)
             .whereEqualTo("isEnabled", true)

             val listener = query.addSnapshotListener { snapshot, error ->
                 if (error != null) {
                     trySend(NetworkResult.Error(error.message)).isFailure
                     Log.d("errorfound",error.message.toString())
                     return@addSnapshotListener
                 }

                 if (snapshot == null || snapshot.isEmpty) {
                     trySend(NetworkResult.Success(emptyList()))
                     return@addSnapshotListener
                 }


                     val groupedProducts = snapshot.documents.mapNotNull { it.toObject(Product::class.java) }.groupBy { it.category }

                     val categories = groupedProducts.map { (category, products) ->
                         ParentCategoryItem(
                             categoryName = category.displayName,
                             childCategoryItems = products
                         )
                     }.sortedBy { it.categoryName }

                     trySend(NetworkResult.Success(categories))


             }

             awaitClose{
                 listener.remove()
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







    suspend fun getCategoriesFromFirebase(): NetworkResult<List<Category>>{
        return try {

            val querySnapshot = db.collectionGroup("categories").get().await()
            val fetchedCategories = querySnapshot.toObjects(Category::class.java).sortedBy { it.id }

            categoryLocalRepoImpl.upsertCategory(fetchedCategories.map { it.toCategoryEntity() })
            NetworkResult.Success(fetchedCategories)

        }catch (e: Exception){
            try {
                val cachedCategories = categoryLocalRepoImpl.getCategories().first()
                if (cachedCategories.isNotEmpty()) {
                    NetworkResult.Success(cachedCategories.map { it.toCategory() })
                } else {
                    NetworkResult.Error(e.message ?: "Unknown Error")
                }
            } catch (cacheError: Exception) {
                NetworkResult.Error(e.message)
            }
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