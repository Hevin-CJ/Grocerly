package com.example.grocerly.Repository.remote

import android.util.Log
import com.example.grocerly.Repository.local.CategoryLocalRepoImpl
import com.example.grocerly.model.Category
import com.example.grocerly.utils.Mappers.toCategory
import com.example.grocerly.utils.Mappers.toCategoryEntity
import com.example.grocerly.utils.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ActivityRetainedScoped
class MenuRepoImpl @Inject constructor(private val categoryLocalRepoImpl: CategoryLocalRepoImpl,private val db: FirebaseFirestore) {

     fun getCategoriesFromFirebase(): Flow<NetworkResult<List<Category>>> = flow{
         try {
             val localData = categoryLocalRepoImpl.getCategories().first()
             if (localData.isNotEmpty()) {
                 emit(NetworkResult.Success(localData.map { it.toCategory() }))
             } else {
                 emit(NetworkResult.Loading())
             }
         } catch (e: Exception) {
             Log.e("MenuRepo", "Failed to load cache", e)
         }

         try {
             val snapshot = db.collectionGroup("categories").get().await()
             val remoteCategories = snapshot.toObjects(Category::class.java).sortedBy { it.id }

             categoryLocalRepoImpl.upsertCategory(remoteCategories.map { it.toCategoryEntity() })
             emit(NetworkResult.Success(remoteCategories))

         } catch (e: Exception) {
             val localDataCheck = categoryLocalRepoImpl.getCategories().first()
             if (localDataCheck.isEmpty()) {
                 emit(NetworkResult.Error(e.message.toString()))
             } else {
                  emit(NetworkResult.Error("No Internet Connection"))
             }
         }
    }

}