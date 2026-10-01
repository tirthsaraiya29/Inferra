package com.inferra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ModelEntity::class,
        CanonicalModelEntity::class,
        ModelFamilyEntity::class,
        ModelAliasEntity::class,
        BenchmarkDefinitionEntity::class,
        BenchmarkVersionEntity::class,
        BenchmarkResultEntity::class,
        ProviderEntity::class,
        ProviderDeploymentEntity::class,
        PricingRecordEntity::class,
        WatchlistEntity::class,
        DownloadJobEntity::class,
        HardwareProfileEntity::class,
        DeviceTargetEntity::class,
        LocalModelEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun canonicalModelDao(): CanonicalModelDao
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun providerDao(): ProviderDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun downloadJobDao(): DownloadJobDao
    abstract fun hardwareProfileDao(): HardwareProfileDao
    abstract fun deviceTargetDao(): DeviceTargetDao
    abstract fun localModelDao(): LocalModelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inferra_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
