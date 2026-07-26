package com.example.grocerly.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.grocerly.room.dao.PendingOrderDao
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


    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `offer_entity_table` ADD COLUMN `productId` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `offer_entity_table` ADD COLUMN `partnerId` TEXT NOT NULL DEFAULT ''")

            // Note: If your pending_orders table also failed in this v1 -> v2 jump, add it here:
             db.execSQL("ALTER TABLE `pending_orders` ADD COLUMN `retryCount` INTEGER NOT NULL DEFAULT 0")
        }
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

    @Provides
    @Singleton
    fun providePendingOrderDao(database: GrocerlyDatabase): PendingOrderDao {
        return database.pendingOrderDao()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: GrocerlyDatabase) = database.productDao()


}