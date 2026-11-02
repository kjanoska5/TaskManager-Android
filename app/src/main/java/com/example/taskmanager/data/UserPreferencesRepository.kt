package com.example.taskmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
//data/data/com.example.taskmanager/files/datastore/settings.preferences_pb
//Preferences DataStore
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val ARE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("are_notifications_enabled")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences -> preferences[IS_DARK_MODE] ?: false }

    val areNotificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences -> preferences[ARE_NOTIFICATIONS_ENABLED] ?: true }

    suspend fun saveDarkModePreference(isDarkMode: Boolean) {
        dataStore.edit { preferences -> preferences[IS_DARK_MODE] = isDarkMode }
    }

    suspend fun saveNotificationsPreference(isEnabled: Boolean) {
        dataStore.edit { preferences -> preferences[ARE_NOTIFICATIONS_ENABLED] = isEnabled }
    }
}