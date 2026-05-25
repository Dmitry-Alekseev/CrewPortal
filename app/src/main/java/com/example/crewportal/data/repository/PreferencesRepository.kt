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
        val A320_MINUTES = intPreferencesKey("a320_minutes")
        val A330_MINUTES = intPreferencesKey("a330_minutes")
        val A350_MINUTES = intPreferencesKey("a350_minutes")
        val INSTALLED_APP_VERSION = stringPreferencesKey("installed_app_version")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val NEXT_MONTH_ROSTER_REVIEWED = booleanPreferencesKey("next_month_roster_reviewed")
        val ENHANCED_ROSTER_TARGET = booleanPreferencesKey("enhanced_roster_target")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMEMBER_ME] ?: false }
    val rememberedLogin: Flow<String> = context.dataStore.data.map { it[Keys.REMEMBERED_LOGIN] ?: "" }
    val totalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_MINUTES] ?: 240000 }
    val picMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.PIC_MINUTES] ?: 90000 }
    val a320Minutes: Flow<Int> = context.dataStore.data.map { it[Keys.A320_MINUTES] ?: 180000 }
    val a330Minutes: Flow<Int> = context.dataStore.data.map { it[Keys.A330_MINUTES] ?: 36000 }
    val a350Minutes: Flow<Int> = context.dataStore.data.map { it[Keys.A350_MINUTES] ?: 24000 }
    val installedAppVersion: Flow<String> = context.dataStore.data.map { it[Keys.INSTALLED_APP_VERSION] ?: "" }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: false }
    val appLanguage: Flow<String> = context.dataStore.data.map { it[Keys.APP_LANGUAGE] ?: "en" }
    val nextMonthRosterReviewed: Flow<Boolean> = context.dataStore.data.map { it[Keys.NEXT_MONTH_ROSTER_REVIEWED] ?: false }
    val enhancedRosterTarget: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENHANCED_ROSTER_TARGET] ?: false }

    suspend fun setLoginState(loggedIn: Boolean, remember: Boolean, login: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_LOGGED_IN] = loggedIn
            preferences[Keys.REMEMBER_ME] = remember
            preferences[Keys.REMEMBERED_LOGIN] = if (remember) login else ""
        }
    }

    suspend fun logout() { context.dataStore.edit { it[Keys.IS_LOGGED_IN] = false } }

    suspend fun setInstalledAppVersion(versionName: String) {
        context.dataStore.edit { preferences -> preferences[Keys.INSTALLED_APP_VERSION] = versionName }
    }

    suspend fun setDarkTheme(enabled: Boolean) { context.dataStore.edit { it[Keys.DARK_THEME] = enabled } }
    suspend fun setAppLanguage(language: String) { context.dataStore.edit { it[Keys.APP_LANGUAGE] = language } }
    suspend fun setNextMonthRosterDecision(reviewed: Boolean, enhancedTarget: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NEXT_MONTH_ROSTER_REVIEWED] = reviewed
            preferences[Keys.ENHANCED_ROSTER_TARGET] = enhancedTarget
        }
    }

    suspend fun addFlightTime(minutes: Int, aircraftLabel: String = "") {
        context.dataStore.edit { preferences ->
            val currentTotal = preferences[Keys.TOTAL_MINUTES] ?: 240000
            val currentPic = preferences[Keys.PIC_MINUTES] ?: 90000
            preferences[Keys.TOTAL_MINUTES] = currentTotal + minutes
            preferences[Keys.PIC_MINUTES] = currentPic + minutes
            when {
                aircraftLabel.contains("A330", ignoreCase = true) -> preferences[Keys.A330_MINUTES] = (preferences[Keys.A330_MINUTES] ?: 36000) + minutes
                aircraftLabel.contains("A350", ignoreCase = true) -> preferences[Keys.A350_MINUTES] = (preferences[Keys.A350_MINUTES] ?: 24000) + minutes
                else -> preferences[Keys.A320_MINUTES] = (preferences[Keys.A320_MINUTES] ?: 180000) + minutes
            }
        }
    }
}
