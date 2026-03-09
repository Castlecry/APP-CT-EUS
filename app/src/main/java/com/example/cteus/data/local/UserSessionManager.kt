package com.example.cteus.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class UserSessionManager(private val context: Context) {
    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_NAME = stringPreferencesKey("username")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    suspend fun saveSession(token: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token
            preferences[USER_NAME] = username
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(USER_NAME)
        }
    }
}
