package com.tgvlessproxy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val SUBSCRIPTION_URL = stringPreferencesKey("subscription_url")
        val SELECTED_SERVER_INDEX = intPreferencesKey("selected_server_index")
        val CACHED_SUBSCRIPTION = stringPreferencesKey("cached_subscription")
        val LAST_FETCH_TIMESTAMP = longPreferencesKey("last_fetch_timestamp")
    }

    val subscriptionUrl: Flow<String?> = context.dataStore.data.map { it[SUBSCRIPTION_URL] }
    val selectedServerIndex: Flow<Int> = context.dataStore.data.map { it[SELECTED_SERVER_INDEX] ?: 0 }
    val cachedSubscription: Flow<String?> = context.dataStore.data.map { it[CACHED_SUBSCRIPTION] }
    val lastFetchTimestamp: Flow<Long> = context.dataStore.data.map { it[LAST_FETCH_TIMESTAMP] ?: 0L }

    suspend fun saveSubscription(url: String, response: String) {
        context.dataStore.edit { prefs ->
            prefs[SUBSCRIPTION_URL] = url
            prefs[CACHED_SUBSCRIPTION] = response
            prefs[LAST_FETCH_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun saveSelectedServerIndex(index: Int) {
        context.dataStore.edit { it[SELECTED_SERVER_INDEX] = index }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun shouldRefresh(): Boolean {
        val lastFetch = lastFetchTimestamp.first()
        val elapsed = System.currentTimeMillis() - lastFetch
        return elapsed > 24 * 60 * 60 * 1000L
    }
}
