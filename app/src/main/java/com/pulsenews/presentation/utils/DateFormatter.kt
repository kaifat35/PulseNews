package com.pulsenews.presentation.utils

import java.text.SimpleDateFormat
import java.util.Locale

private val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)

fun Long.formatDate(): String {
    return formatter.format(this)
}
