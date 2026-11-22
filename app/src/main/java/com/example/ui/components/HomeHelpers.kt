package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.R
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun rememberCurrentTimeMillis(intervalMs: Long = 10_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(intervalMs)
        }
    }
    return now
}

@Composable
fun localizedEditionLabel(customTitle: String, timeMillis: Long): String {
    if (customTitle.isNotBlank()) return customTitle
    val hour = remember(timeMillis / 60_000L) {
        Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.HOUR_OF_DAY)
    }
    val resId = when (hour) {
        in 5..11 -> R.string.edition_morning
        in 12..16 -> R.string.edition_afternoon
        in 17..21 -> R.string.edition_evening
        else -> R.string.edition_night
    }
    return stringResource(resId)
}

fun formatLocalizedTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}

fun formatLocalizedDate(timeMillis: Long): String {
    val formatter = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault())
    return formatter.format(Date(timeMillis))
}
