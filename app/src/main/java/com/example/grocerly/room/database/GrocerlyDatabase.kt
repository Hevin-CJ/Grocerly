    package com.example.grocerly.room.database

    import androidx.room.AutoMigration
    import androidx.room.Database
    import androidx.room.RoomDatabase
    import androidx.room.TypeConverters
    import com.example.grocerly.db.ProductTypeConverters
    import com.example.grocerly.room.convertors.CategoryConvertor
    import com.example.grocerly.room.dao.CategoryDao
    import com.example.grocerly.room.dao.OfferDao
    import com.example.grocerly.room.dao.PendingOrderDao
    import com.example.grocerly.room.dao.ProductDao
    import com.example.grocerly.room.dao.ProfileDao
    import com.example.grocerly.room.entity.CategoryEntity
    import com.example.grocerly.room.entity.OfferEntity
    import com.example.grocerly.room.entity.PendingOrderEntity
    import com.example.grocerly.room.entity.ProductEntity
    import com.example.grocerly.room.entity.ProfileEntity

    @Database(entities = [CategoryEntity::class, ProfileEntity::class, OfferEntity::class, PendingOrderEntity::class, ProductEntity::class], version = 3, exportSchema = true,
        )//autoMigrations = [AutoMigration(from = 1, to = 2)]
    @TypeConverters(value = [CategoryConvertor::class, ProductTypeConverters::class])
    abstract class GrocerlyDatabase: RoomDatabase() {
        abstract fun categoryDao(): CategoryDao
        abstract fun profileDao(): ProfileDao
        abstract fun offerDao(): OfferDao

        abstract fun pendingOrderDao(): PendingOrderDao

        abstract fun productDao(): ProductDao

    }