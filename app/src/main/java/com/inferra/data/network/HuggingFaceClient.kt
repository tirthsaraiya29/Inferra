package com.inferra.data.network

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object HuggingFaceClient {
    private const val TAG = "HuggingFaceApi"
    private const val BASE_URL = "https://huggingface.co/"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = false
    }

    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "--> SENDING REQUEST: ${request.method} ${request.url}")

            return try {
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "<-- RECEIVED RESPONSE (${response.code} ${response.message}) from ${request.url} in ${duration}ms [Headers: ${response.headers.size}]")
                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "<-- REQUEST FAILED: ${request.url} after ${duration}ms: ${e.localizedMessage}", e)
                throw e
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
}
