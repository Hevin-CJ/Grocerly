package com.example.grocerly.Repository.local

import com.example.grocerly.room.dao.CategoryDao
import com.example.grocerly.room.entity.CategoryEntity
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class CategoryLocalRepoImpl @Inject constructor(private val categoryDao: CategoryDao) {

    suspend fun upsertCategory(categoryEntity: List<CategoryEntity>) = categoryDao.upsertCategories(categoryEntity)

     fun getCategories() = categoryDao.getCategories()

}