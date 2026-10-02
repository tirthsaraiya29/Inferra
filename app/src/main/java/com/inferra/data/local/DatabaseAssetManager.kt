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
                    // 1. android_models
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

                    // 2. android_artifacts
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS android_artifacts_temp (
                            artifact_id TEXT NOT NULL PRIMARY KEY,
                            model_id TEXT NOT NULL,
                            format TEXT NOT NULL,
                            quantization TEXT,
                            file_size_bytes INTEGER,
                            repository_id TEXT NOT NULL,
                            file_path TEXT,
                            FOREIGN KEY(model_id) REFERENCES android_models(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT OR IGNORE INTO android_artifacts_temp SELECT * FROM android_artifacts;")
                    db.execSQL("DROP TABLE android_artifacts;")
                    db.execSQL("ALTER TABLE android_artifacts_temp RENAME TO android_artifacts;")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_android_artifacts_model_id ON android_artifacts(model_id);")

                    // 3. android_benchmarks
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS android_benchmarks_temp (
                            benchmark_id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            domain TEXT NOT NULL,
                            metric_name TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT OR IGNORE INTO android_benchmarks_temp SELECT * FROM android_benchmarks;")
                    db.execSQL("DROP TABLE android_benchmarks;")
                    db.execSQL("ALTER TABLE android_benchmarks_temp RENAME TO android_benchmarks;")

                    // 4. android_benchmark_scores
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS android_benchmark_scores_temp (
                            id TEXT NOT NULL PRIMARY KEY,
                            model_id TEXT NOT NULL,
                            benchmark_id TEXT NOT NULL,
                            score REAL NOT NULL,
                            score_normalized REAL NOT NULL,
                            measurement_type TEXT NOT NULL,
                            FOREIGN KEY(model_id) REFERENCES android_models(id) ON DELETE CASCADE,
                            FOREIGN KEY(benchmark_id) REFERENCES android_benchmarks(benchmark_id) ON DELETE NO ACTION
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT OR IGNORE INTO android_benchmark_scores_temp SELECT * FROM android_benchmark_scores;")
                    db.execSQL("DROP TABLE android_benchmark_scores;")
                    db.execSQL("ALTER TABLE android_benchmark_scores_temp RENAME TO android_benchmark_scores;")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_android_benchmark_scores_model_id ON android_benchmark_scores(model_id);")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_android_benchmark_scores_benchmark_id ON android_benchmark_scores(benchmark_id);")

                    // 5. android_provider_pricing
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS android_provider_pricing_temp (
                            id TEXT NOT NULL PRIMARY KEY,
                            model_id TEXT NOT NULL,
                            provider_name TEXT NOT NULL,
                            input_cost_per_m REAL,
                            output_cost_per_m REAL,
                            context_window INTEGER,
                            updated_at TEXT NOT NULL,
                            FOREIGN KEY(model_id) REFERENCES android_models(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT OR IGNORE INTO android_provider_pricing_temp SELECT * FROM android_provider_pricing;")
                    db.execSQL("DROP TABLE android_provider_pricing;")
                    db.execSQL("ALTER TABLE android_provider_pricing_temp RENAME TO android_provider_pricing;")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_android_provider_pricing_model_id ON android_provider_pricing(model_id);")

                    // 6. android_models_wide
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS android_models_wide_temp (
                            model_id TEXT NOT NULL PRIMARY KEY,
                            display_name TEXT NOT NULL,
                            organization TEXT NOT NULL,
                            parameter_count INTEGER,
                            context_length INTEGER,
                            license TEXT,
                            mmlu_pro REAL,
                            gpqa REAL,
                            math_l5 REAL,
                            ifeval REAL,
                            musr REAL,
                            bbh REAL,
                            aime REAL,
                            gsm8k REAL,
                            swe_bench_verified REAL,
                            swe_bench_lite REAL,
                            humaneval_plus REAL,
                            mbpp_plus REAL,
                            livecodebench REAL,
                            aider_polyglot REAL,
                            bfcl REAL,
                            tau_bench REAL,
                            gaia REAL,
                            ruler REAL,
                            mmmu REAL,
                            mathvista REAL,
                            chartqa REAL,
                            docvqa REAL,
                            video_mme REAL,
                            livebench REAL,
                            simpleqa REAL,
                            arena_elo REAL,
                            arena_coding_elo REAL,
                            arena_hard_elo REAL,
                            FOREIGN KEY(model_id) REFERENCES android_models(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("INSERT OR IGNORE INTO android_models_wide_temp SELECT * FROM android_models_wide;")
                    db.execSQL("DROP TABLE android_models_wide;")
                    db.execSQL("ALTER TABLE android_models_wide_temp RENAME TO android_models_wide;")

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            }
            Log.i(TAG, "All pre-packaged database tables sanitized successfully.")
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
