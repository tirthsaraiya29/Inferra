package com.inferra.data.network

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object HuggingFaceClient {
    private const val TAG = "HuggingFaceApi"
    private const val BASE_URL = "https://huggingface.co/"

    private var tokenProvider: (() -> String?)? = null

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

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

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val token = tokenProvider?.invoke()
            val requestBuilder = original.newBuilder()

            if (!token.isNullOrBlank() && original.header("Authorization") == null && original.url.host.contains("huggingface.co")) {
                requestBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            return chain.proceed(requestBuilder.build())
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
                safeLogD("<-- RECEIVED RESPONSE (${response.code} ${response.message}) from ${request.url} in ${duration}ms")
                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                safeLogE("<-- REQUEST FAILED: ${request.url} after ${duration}ms: ${e.localizedMessage}", e)
                throw e
            }
        }
    }

    private object StringConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            if (type == String::class.java) {
                return Converter<ResponseBody, String> { body -> body.string() }
            }
            return null
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

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .addInterceptor(AuthInterceptor())
            .addInterceptor(LoggingInterceptor())
            .build()
    }

    val api: HuggingFaceApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(StringConverterFactory)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(HuggingFaceApi::class.java)
    }
}
