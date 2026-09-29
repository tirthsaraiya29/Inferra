package com.inferra

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import javax.net.ssl.SSLPeerUnverifiedException

class CertificatePinTest {

    @Test
    fun getHuggingFaceCertPins() {
        val dummyPinner = CertificatePinner.Builder()
            .add("huggingface.co", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        val client = OkHttpClient.Builder()
            .certificatePinner(dummyPinner)
            .build()

        val request = Request.Builder()
            .url("https://huggingface.co/api/models?limit=1")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                println("Connected without cert pin failure: ${response.code}")
            }
        } catch (e: SSLPeerUnverifiedException) {
            println("Caught expected SSLPeerUnverifiedException:\n${e.message}")
        }
    }
}
