package com.pulsenews.data.mapper

import com.pulsenews.domain.entity.RefreshConfig
import com.pulsenews.domain.entity.Settings

fun Settings.toRefreshConfig(): RefreshConfig {
    return RefreshConfig(language, interval, wifiOnly)
}