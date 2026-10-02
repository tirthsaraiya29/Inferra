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

        if (dbFile.exists() && !forceOverwrite) {
            Log.d(TAG, "Database $DB_NAME already exists at ${dbFile.absolutePath}")
            return true
        }

        return copyAssetDatabase(context, dbFile)
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
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy database asset '$ASSET_NAME': ${e.message}", e)
            return false
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
