package com.example.grocerly.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.grocerly.room.convertors.CategoryConvertor
import com.example.grocerly.room.dao.CategoryDao
import com.example.grocerly.room.dao.OfferDao
import com.example.grocerly.room.dao.ProfileDao
import com.example.grocerly.room.entity.CategoryEntity
import com.example.grocerly.room.entity.OfferEntity
import com.example.grocerly.room.entity.ProfileEntity

@Database(entities = [CategoryEntity::class, ProfileEntity::class, OfferEntity::class], version = 1, exportSchema = false)
@TypeConverters(CategoryConvertor::class)
abstract class GrocerlyDatabase: RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun profileDao(): ProfileDao
    abstract fun offerDao(): OfferDao
}