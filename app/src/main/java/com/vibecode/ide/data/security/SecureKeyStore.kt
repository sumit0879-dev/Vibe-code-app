package com.vibecode.ide.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores API keys (and any other secrets) using an AES256-GCM encrypted
 * SharedPreferences file backed by a key held in the Android Keystore.
 * API keys never touch the Room database or plain-text files.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** Generates a new random alias and stores the secret under it. Returns the alias. */
    fun storeNewSecret(secret: String): String {
        val alias = UUID.randomUUID().toString()
        prefs.edit().putString(alias, secret).apply()
        return alias
    }

    /** Overwrites the secret stored at an existing alias (or creates it if absent). */
    fun updateSecret(alias: String, secret: String) {
        prefs.edit().putString(alias, secret).apply()
    }

    fun getSecret(alias: String?): String? {
        if (alias.isNullOrBlank()) return null
        return prefs.getString(alias, null)
    }

    fun deleteSecret(alias: String) {
        prefs.edit().remove(alias).apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "secure_prefs"
    }
}
