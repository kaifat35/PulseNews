package com.pulsenews.domain.repository

import com.pulsenews.domain.entity.Language
import com.pulsenews.domain.entity.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun getSettings(): Flow<Settings>

    suspend fun updateLanguage(language: Language)

    suspend fun updateInterval(minutes: Int)

    suspend fun updateNotificationEnabled(enabled: Boolean)

    suspend fun updateWifiOnly(wifiOnly: Boolean)
}