package com.example.grocerly.Repository.remote

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.grocerly.R
import com.example.grocerly.Repository.local.CategoryLocalRepoImpl
import com.example.grocerly.Repository.local.OfferLocalRepoImpl
import com.example.grocerly.model.Category
import com.example.grocerly.model.OfferItem
import com.example.grocerly.model.ParentCategoryItem
import com.example.grocerly.model.Product
import com.example.grocerly.utils.Constants.OFFERS
import com.example.grocerly.utils.Constants.PRODUCTS
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.Mappers.toCategoryEntity
import com.example.grocerly.utils.Mappers.toOfferEntityList
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.ProductCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
                    it.category == category
                }


                trySend(NetworkResult.Success(groupedProducts))
            }



        }

        awaitClose{
            listener.remove()
        }
    }


        suspend fun getOffersFromFirebase():NetworkResult<Unit>{
            return try {

                val querySnapshot = db.collectionGroup(OFFERS)
                    .get()
                    .await()

                val offers = if (querySnapshot.isEmpty) {
                    listOf(createDummyOffer())
                } else {
                    querySnapshot.documents.mapNotNull { it.toObject(OfferItem::class.java) }
                }

                offerLocalRepoImpl.upsertOffer(offers.toOfferEntityList() )
                Log.d("currentitemrepo",offers.toString())
                NetworkResult.Success(Unit)

            }catch (E:Exception){
                NetworkResult.Error(E.message)
            }
        }

    fun createDummyOffer(): OfferItem {

        return OfferItem(
            offerId = "offer_123",
            offerImage = "",
            offerBgColor = "#E3F2FD",
            buttonText = "",
            buttonBgColor = "#FFFFFF",
            buttonTxtColor = "#FFFFFF",
            descriptionText = "No Offers Found",
            descriptionTextColor = "#000000"
        )
    }


    suspend fun getCategoriesFromFirebase(): NetworkResult<List<Category>>{
        return try {
            val cachedCategories = categoryLocalRepoImpl.getCategories().first()

            val querySnapshot = db.collectionGroup("categories").get().await()
            val fetchedCategories = querySnapshot.toObjects(Category::class.java).sortedBy { it.id }

            if (cachedCategories.isEmpty() || cachedCategories != fetchedCategories){
                categoryLocalRepoImpl.upsertCategory(fetchedCategories.map { it.toCategoryEntity() })
            }

            NetworkResult.Success(cachedCategories.map { it.toCategory() })

        }catch (e: Exception){
            NetworkResult.Error(e.message)
        }
    }


    suspend fun getCityAndState(): NetworkResult<String>{
      return try {
          val address =  addressRepoImpl.getDefaultAddressFromDb().firstOrNull()?.data
          if (address==null){
             return NetworkResult.Success("Add New Address")
          }
          
          val formatedAddress = buildString {
              append(address.city )
              append(" , " )
              append(address.state)
          }
         return NetworkResult.Success(formatedAddress)
      }catch (e: Exception){
          NetworkResult.Error(e.message)
      }
    }




}