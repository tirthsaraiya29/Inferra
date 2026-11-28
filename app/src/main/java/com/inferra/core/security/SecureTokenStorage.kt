package com.inferra.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStorage(context: Context) {

    private companion object {
        const val TAG = "SecureTokenStorage"
        const val KEY_ALIAS = "inferra_hf_token_key"
        const val PREF_NAME = "inferra_secure_prefs"
        const val KEY_ENCRYPTED_TOKEN = "enc_hf_token"
        const val KEY_IV = "enc_hf_iv"
        const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
    }

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveToken(token: String?) {
        if (token.isNullOrBlank()) {
            clearToken()
            return
        }

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))

            val encBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_ENCRYPTED_TOKEN, encBase64)
                .putString(KEY_IV, ivBase64)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely save token: ${e.message}", e)
        }
    }

    fun getToken(): String? {
        val encBase64 = prefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        val ivBase64 = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val secretKey = getOrCreateSecretKey()
            val encryptedBytes = Base64.decode(encBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt stored token: ${e.message}", e)
            null
        }
    }

    fun clearToken() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_TOKEN)
            .remove(KEY_IV)
            .apply()
    }
}
