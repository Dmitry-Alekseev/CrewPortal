package com.example.crewportal.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "crew_settings")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val REMEMBERED_LOGIN = stringPreferencesKey("remembered_login")
        val TOTAL_MINUTES = intPreferencesKey("total_minutes")
        val PIC_MINUTES = intPreferencesKey("pic_minutes")
        val INSTALLED_APP_VERSION = stringPreferencesKey("installed_app_version")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMEMBER_ME] ?: false }
    val rememberedLogin: Flow<String> = context.dataStore.data.map { it[Keys.REMEMBERED_LOGIN] ?: "" }
    val totalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_MINUTES] ?: 240000 }
    val picMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.PIC_MINUTES] ?: 90000 }
    val installedAppVersion: Flow<String> = context.dataStore.data.map { it[Keys.INSTALLED_APP_VERSION] ?: "" }

    suspend fun setLoginState(loggedIn: Boolean, remember: Boolean, login: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_LOGGED_IN] = loggedIn
            preferences[Keys.REMEMBER_ME] = remember
            preferences[Keys.REMEMBERED_LOGIN] = if (remember) login else ""
        }
    }

    suspend fun logout() {
        context.dataStore.edit { it[Keys.IS_LOGGED_IN] = false }
    }

    suspend fun setInstalledAppVersion(versionName: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.INSTALLED_APP_VERSION] = versionName
        }
    }

    suspend fun addFlightTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            val currentTotal = preferences[Keys.TOTAL_MINUTES] ?: 240000
            val currentPic = preferences[Keys.PIC_MINUTES] ?: 90000
            preferences[Keys.TOTAL_MINUTES] = currentTotal + minutes
            preferences[Keys.PIC_MINUTES] = currentPic + minutes
        }
    }
}
