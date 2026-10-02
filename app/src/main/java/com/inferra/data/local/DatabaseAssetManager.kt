package com.inferra.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object DatabaseAssetManager {

    private const val TAG = "DatabaseAssetManager"
    const val ASSET_NAME = "inferra_models_v1.sqlite3"
    const val DB_NAME = "inferra_models_v1.db"

    fun ensureDatabaseAssetCopied(context: Context, forceOverwrite: Boolean = false): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)

        val success = if (!dbFile.exists() || forceOverwrite) {
            copyAssetDatabase(context, dbFile)
        } else {
            Log.d(TAG, "Database $DB_NAME already exists at ${dbFile.absolutePath}")
            true
        }

        if (success && dbFile.exists()) {
            sanitizeSchema(dbFile)
        }

        return success
    }

    private fun copyAssetDatabase(context: Context, destinationFile: File): Boolean {
        Log.i(TAG, "Unpacking asset database '$ASSET_NAME' to '${destinationFile.absolutePath}'...")
        try {
            val dbDir = destinationFile.parentFile
            if ((dbDir != null) && !dbDir.exists()) {
                dbDir.mkdirs()
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            context.assets.open(ASSET_NAME).use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.i(TAG, "Successfully unpacked database asset (${destinationFile.length()} bytes)")
            sanitizeSchema(destinationFile)
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy database asset '$ASSET_NAME': ${e.message}", e)
            return false
        }
    }

    private fun sanitizeSchema(dbFile: File) {
        if (!dbFile.exists()) return
        try {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                db.execSQL("PRAGMA foreign_keys = OFF;")
                db.beginTransaction()
                try {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS android_models_temp (
                            id TEXT NOT NULL PRIMARY KEY,
                            display_name TEXT NOT NULL,
                            family_name TEXT,
                            organization TEXT NOT NULL,
                            model_type TEXT NOT NULL,
                            parameter_count INTEGER,
                            context_length INTEGER,
                            license TEXT,
                            is_open_weights INTEGER NOT NULL,
                            updated_at TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT OR IGNORE INTO android_models_temp SELECT * FROM android_models;")
                    db.execSQL("DROP TABLE android_models;")
                    db.execSQL("ALTER TABLE android_models_temp RENAME TO android_models;")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            }
            Log.i(TAG, "Database schema sanitized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sanitize database schema: ${e.message}", e)
        }
    }

    fun rebuildCatalogCache(context: Context): Boolean {
        Log.w(TAG, "Triggering forced catalog cache reconstruction...")
        val dbFile = context.getDatabasePath(DB_NAME)
        val journalFile = File(dbFile.path + "-journal")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        try {
            if (dbFile.exists()) dbFile.delete()
            if (journalFile.exists()) journalFile.delete()
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning corrupt database files: ${e.message}")
        }

        return copyAssetDatabase(context, dbFile)
    }

    fun handleCorruptDatabase(context: Context, exception: Exception): Boolean {
        if (exception is SQLiteDatabaseCorruptException || exception is SQLiteException) {
            Log.e(TAG, "Caught SQLite corrupt or access error: ${exception.message}. Recovering...", exception)
            return rebuildCatalogCache(context)
        }
        return false
    }
}
