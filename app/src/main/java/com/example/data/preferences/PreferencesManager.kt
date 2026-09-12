package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AccentTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumenhance_settings")

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_ID = stringPreferencesKey("accent_id")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val EXPORT_QUALITY = intPreferencesKey("export_quality")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val FACE_ENHANCE_CONSENT = booleanPreferencesKey("face_enhance_consent")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "DARK" -> ThemeMode.DARK
            "LIGHT" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }

    val accentThemeFlow: Flow<AccentTheme> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.ACCENT_ID] ?: "cyan"
        AccentTheme.fromId(id)
    }

    val exportFormatFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.EXPORT_FORMAT] ?: "JPEG"
    }

    val exportQualityFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.EXPORT_QUALITY] ?: 95
    }

    val hasSeenOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING] ?: false
    }

    val faceEnhanceConsentFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FACE_ENHANCE_CONSENT] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAccentTheme(theme: AccentTheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_ID] = theme.id
        }
    }

    suspend fun setExportFormat(format: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXPORT_FORMAT] = format
        }
    }

    suspend fun setExportQuality(quality: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXPORT_QUALITY] = quality
        }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_ONBOARDING] = seen
        }
    }

    suspend fun setFaceEnhanceConsent(consent: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FACE_ENHANCE_CONSENT] = consent
        }
    }
}
