package com.univesp.lumme.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lumme_prefs")

/**
 * Armazena o JWT do próprio backend (não os tokens SmartThings — esses ficam no servidor).
 *
 * Em produção, considere usar androidx.security:security-crypto com EncryptedSharedPreferences
 * ou o EncryptedDataStore para criptografia em repouso.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val jwtKey = stringPreferencesKey("jwt")
    private val stateKey = stringPreferencesKey("oauth_state")

    val jwtFlow: Flow<String?> = context.dataStore.data.map { it[jwtKey] }

    suspend fun jwt(): String? = jwtFlow.first()

    suspend fun saveJwt(token: String) {
        context.dataStore.edit { it[jwtKey] = token }
    }

    suspend fun clearJwt() {
        context.dataStore.edit { it.remove(jwtKey) }
    }

    suspend fun saveState(state: String) {
        context.dataStore.edit { it[stateKey] = state }
    }

    suspend fun state(): String? = context.dataStore.data.map { it[stateKey] }.first()
}
