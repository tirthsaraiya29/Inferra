package com.inferra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedRepoEntity::class,
        BookmarkEntity::class,
        DownloadJobEntity::class,
        LocalFileEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedRepoDao(): CachedRepoDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadJobDao(): DownloadJobDao
    abstract fun localFileDao(): LocalFileDao

    companion object {
        private const val DB_NAME = "inferra_hub.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DB_NAME
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        }
    }
}
