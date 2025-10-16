package com.anthroteacher.servitorconnect.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private const val DS_NAME = "servitor_prefs"
val Context.dataStore by preferencesDataStore(DS_NAME)

object Keys {
    val INTENTION = stringPreferencesKey("intention")
    val BURST_COUNT = intPreferencesKey("burst_count")
    val FREQ = stringPreferencesKey("frequency")
    val DURATION_SEC = intPreferencesKey("duration_sec")
    val FORCE_DARK = booleanPreferencesKey("force_dark")
}

enum class Frequency { Max, Hz3, Hz8, Min5 }

data class SavedSettings(
    val intention: String = "",
    val burstCount: Int = 888_888,
    val frequency: Frequency = Frequency.Min5,
    val durationSec: Int = 86_400,
    val forceDark: Boolean = false
)

fun readSettings(context: Context) = context.dataStore.data.map { p ->
    SavedSettings(
        intention = p[Keys.INTENTION] ?: "",
        burstCount = p[Keys.BURST_COUNT] ?: 888_888,
        frequency = runCatching { Frequency.valueOf(p[Keys.FREQ] ?: "Hourly") }.getOrDefault(Frequency.Max),
        durationSec = p[Keys.DURATION_SEC] ?: 86_400,
        forceDark = p[Keys.FORCE_DARK] ?: false
    )
}

suspend fun saveSettings(context: Context, s: SavedSettings) {
    context.dataStore.edit { p ->
        p[Keys.INTENTION] = s.intention
        p[Keys.BURST_COUNT] = s.burstCount
        p[Keys.FREQ] = s.frequency.name
        p[Keys.DURATION_SEC] = s.durationSec
        p[Keys.FORCE_DARK] = s.forceDark
    }
}
