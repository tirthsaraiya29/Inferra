package com.inferra.data.network

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLPeerUnverifiedException

object HuggingFaceClient {
    private const val TAG = "HuggingFaceApi"
    private const val BASE_URL = "https://huggingface.co/"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = false
    }

    private fun safeLogD(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun safeLogE(msg: String, tr: Throwable? = null) {
        try {
            Log.e(TAG, msg, tr)
        } catch (_: Throwable) {
            println("[$TAG] ERROR: $msg")
            tr?.printStackTrace()
        }
    }

    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startTime = System.currentTimeMillis()
            safeLogD("--> SENDING REQUEST: ${request.method} ${request.url}")

            return try {
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime
                safeLogD("<-- RECEIVED RESPONSE (${response.code} ${response.message}) from ${request.url} in ${duration}ms [Headers: ${response.headers.size}]")
                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                safeLogE("<-- REQUEST FAILED: ${request.url} after ${duration}ms: ${e.localizedMessage}", e)
                throw e
            }
        }
    }

    private val certificatePinner = CertificatePinner.Builder()
        .add("huggingface.co", "sha256/2MqLa/44pZTt+hMDJgp71yGHBqOjWTnulG4H5xsmyQk=")
        .add("huggingface.co", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
        .add("huggingface.co", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
        .add("*.huggingface.co", "sha256/2MqLa/44pZTt+hMDJgp71yGHBqOjWTnulG4H5xsmyQk=")
        .add("*.huggingface.co", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
        .add("*.huggingface.co", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
        .build()

    private val unpinnedFallbackClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor())
            .build()
    }

    private class ResilientPinningInterceptor(
        private val fallbackClientProvider: () -> OkHttpClient
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            return try {
                chain.proceed(request)
            } catch (e: SSLPeerUnverifiedException) {
                safeLogE("SSL Certificate Pinning failed for ${request.url.host}. Falling back to system trust store validation...", e)
                fallbackClientProvider().newCall(request).execute()
            }
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .addInterceptor(ResilientPinningInterceptor { unpinnedFallbackClient })
            .addInterceptor(LoggingInterceptor())
            .build()
    }

    val api: HuggingFaceApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(HuggingFaceApi::class.java)
    }

    val datasetApi: HuggingFaceDatasetApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(HuggingFaceDatasetApi::class.java)
    }
}
