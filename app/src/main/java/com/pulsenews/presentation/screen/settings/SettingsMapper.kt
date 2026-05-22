package com.pulsenews.presentation.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pulsenews.R
import com.pulsenews.domain.entity.Interval
import com.pulsenews.domain.entity.Language

@Composable
fun Language.toReadableFormat(): String {
    return when (this) {
        Language.ENGLISH -> {
            stringResource(R.string.english)
        }

        Language.RUSSIAN -> {
            stringResource(R.string.russian)
        }

        Language.FRENCH -> {
            stringResource(R.string.francais)
        }

        Language.GERMAN -> {
            stringResource(R.string.deutsch)
        }
    }
}

@Composable
fun Interval.toReadableFormat(): String {
    return when (this) {
        Interval.MIN_15 -> "15 minutes"
        Interval.MIN_30 -> "30 minutes"
        Interval.HOUR_1 -> "1 hour"
        Interval.HOUR_2 -> "2 hours"
        Interval.HOUR_4 -> "4 hours"
        Interval.HOUR_8 -> "8 hours"
        Interval.HOUR_24 -> "24 hours"
    }
}
