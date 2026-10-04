package com.inferra.data.download

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.inferra.data.network.HuggingFaceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long
)

class RealDownloadManager(private val context: Context) {

    private companion object {
        const val TAG = "RealDownloadManager"
        const val BUFFER_SIZE = 32 * 1024 // 32 KB
    }

    suspend fun downloadFile(
        url: String,
        targetFile: File,
        expectedSizeBytes: Long,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = targetFile.parentFile
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }

            // Storage Check
            val stat = StatFs(dir?.absolutePath ?: context.filesDir.absolutePath)
            val availableSpaceBytes = stat.availableBlocksLong * stat.blockSizeLong
            if (expectedSizeBytes > 0L && availableSpaceBytes < expectedSizeBytes + (50 * 1024 * 1024L)) {
                Log.e(TAG, "Insufficient disk space: $availableSpaceBytes available vs $expectedSizeBytes required")
                return@withContext false
            }

            val existingLength = if (targetFile.exists()) targetFile.length() else 0L

            val requestBuilder = Request.Builder().url(url)
            if (existingLength > 0L && expectedSizeBytes > 0L && existingLength < expectedSizeBytes) {
                requestBuilder.header("Range", "bytes=$existingLength-")
                Log.d(TAG, "Resuming download from byte offset $existingLength...")
            }

            val request = requestBuilder.build()
            val response = HuggingFaceClient.okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download HTTP request failed with code ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val isPartial = response.code == 206
            val totalContentLength = if (isPartial) existingLength + body.contentLength() else body.contentLength().let { if (it > 0) it else expectedSizeBytes }

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile, isPartial)

            var bytesDownloaded = if (isPartial) existingLength else 0L
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            var startTime = System.currentTimeMillis()
            var bytesSinceLastCalc = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                bytesSinceLastCalc += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - startTime
                if (elapsed >= 500) { // Update progress every 500ms
                    val speed = if (elapsed > 0) (bytesSinceLastCalc * 1000) / elapsed else 0L
                    val remainingBytes = (totalContentLength - bytesDownloaded).coerceAtLeast(0L)
                    val eta = if (speed > 0) remainingBytes / speed else 0L

                    onProgress(
                        DownloadProgress(
                            downloadedBytes = bytesDownloaded,
                            totalBytes = totalContentLength,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                    )

                    startTime = now
                    bytesSinceLastCalc = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            onProgress(
                DownloadProgress(
                    downloadedBytes = bytesDownloaded,
                    totalBytes = totalContentLength,
                    speedBytesPerSec = 0L,
                    etaSeconds = 0L
                )
            )

            Log.i(TAG, "File downloaded successfully to ${targetFile.absolutePath} ($bytesDownloaded bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during streaming download from $url: ${e.message}", e)
            false
        }
    }

    suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute SHA-256 for ${file.name}: ${e.message}")
            ""
        }
    }
}
