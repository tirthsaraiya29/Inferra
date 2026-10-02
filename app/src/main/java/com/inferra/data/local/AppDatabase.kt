package com.inferra.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AndroidModelEntity::class,
        AndroidArtifactEntity::class,
        AndroidBenchmarkEntity::class,
        AndroidBenchmarkScoreEntity::class,
        AndroidProviderPricingEntity::class,
        AndroidModelWideEntity::class,
        ModelEntity::class,
        ProviderEntity::class,
        ProviderDeploymentEntity::class,
        PricingRecordEntity::class,
        WatchlistEntity::class,
        DownloadJobEntity::class,
        HardwareProfileEntity::class,
        DeviceTargetEntity::class,
        LocalModelEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun androidModelDao(): AndroidModelDao
    abstract fun modelDao(): ModelDao
    abstract fun providerDao(): ProviderDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun downloadJobDao(): DownloadJobDao
    abstract fun hardwareProfileDao(): HardwareProfileDao
    abstract fun deviceTargetDao(): DeviceTargetDao
    abstract fun localModelDao(): LocalModelDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            DatabaseAssetManager.ensureDatabaseAssetCopied(appContext)

            return try {
                Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DatabaseAssetManager.DB_NAME
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            } catch (e: Exception) {
                Log.e(TAG, "Error building Room database instance: ${e.message}. Attempting catalog rebuild...", e)
                DatabaseAssetManager.rebuildCatalogCache(appContext)
                Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DatabaseAssetManager.DB_NAME
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            }
        }
    }
}
