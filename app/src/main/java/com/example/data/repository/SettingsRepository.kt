package com.example.data.repository

import android.content.Context
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow

class SettingsRepository(context: Context) {
    private val dataStore = SettingsDataStore(context)

    val customApiKeyFlow: Flow<String> = dataStore.customApiKey

    suspend fun setCustomApiKey(key: String) {
        dataStore.setCustomApiKey(key)
    }
}
