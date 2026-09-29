package com.inferra

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class CertificatePinTest {

    @Test
    fun testHuggingFaceCertificatePinning() {
        val pinner = CertificatePinner.Builder()
            .add("huggingface.co", "sha256/2MqLa/44pZTt+hMDJgp71yGHBqOjWTnulG4H5xsmyQk=")
            .add("huggingface.co", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
            .add("huggingface.co", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
            .add("*.huggingface.co", "sha256/2MqLa/44pZTt+hMDJgp71yGHBqOjWTnulG4H5xsmyQk=")
            .add("*.huggingface.co", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
            .add("*.huggingface.co", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
            .build()

        val client = OkHttpClient.Builder()
            .certificatePinner(pinner)
            .build()

        val request = Request.Builder()
            .url("https://huggingface.co/api/models?limit=1")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
    }
}
