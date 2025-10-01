package com.example.grocerly.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.grocerly.room.database.GrocerlyDatabase
import com.example.grocerly.utils.Constants.GROCERLY_DATABASE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext  context: Context): GrocerlyDatabase {
        return Room.databaseBuilder(context, GrocerlyDatabase::class.java, GROCERLY_DATABASE)
            .fallbackToDestructiveMigration(true)
            .build()
    }


    @Provides
    @Singleton
    fun provideCategoryDao(database: GrocerlyDatabase) = database.categoryDao()

    @Provides
    @Singleton
    fun provideProfileDao(database: GrocerlyDatabase) = database.profileDao()

    @Provides
    @Singleton
    fun provideOfferDao(database: GrocerlyDatabase) = database.offerDao()

}