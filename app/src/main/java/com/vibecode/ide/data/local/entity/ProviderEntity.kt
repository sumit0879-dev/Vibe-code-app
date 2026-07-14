package com.vibecode.ide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined AI provider. The API key itself is NOT stored here — only a
 * reference (keyAlias) into the encrypted SecureKeyStore. This table is safe
 * to export/back up as-is.
 */
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,                 // e.g. https://api.openai.com/v1
    val chatCompletionsPath: String = "/chat/completions",
    val modelsListPath: String = "/models",
    val authMethod: String = "BEARER",   // BEARER | API_KEY_HEADER | QUERY_PARAM | NONE
    val authHeaderName: String = "Authorization",
    val requestFormat: String = "OPENAI",// OPENAI | ANTHROPIC | CUSTOM
    val keyAlias: String,                // alias into SecureKeyStore, empty string if no key
    val isEnabled: Boolean = true,
    val createdAt: Long,
    val extraHeadersJson: String? = null,
)
