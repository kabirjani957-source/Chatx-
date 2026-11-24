package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.DarkModePreference
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chatx_settings")

enum class ChatWallpaper(val id: String, val label: String) {
    DEFAULT_PATTERN("default_pattern", "ChatX Pattern"),
    SOFT_MINT("soft_mint", "Soft Mint"),
    WARM_IVORY("warm_ivory", "Warm Ivory"),
    EMERALD_MIST("emerald_mist", "Emerald Mist"),
    MIDNIGHT_SLATE("midnight_slate", "Midnight Slate");

    companion object {
        fun fromId(id: String): ChatWallpaper = entries.firstOrNull { it.id == id } ?: DEFAULT_PATTERN
    }
}

enum class PrivacyVisibility(val id: String, val label: String) {
    EVERYONE("everyone", "Everyone"),
    MY_CONTACTS("contacts", "My Contacts"),
    NOBODY("nobody", "Nobody");

    companion object {
        fun fromId(id: String): PrivacyVisibility = entries.firstOrNull { it.id == id } ?: EVERYONE
    }
}

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val themePreset: ThemePreset = ThemePreset.EMERALD,
    val wallpaper: ChatWallpaper = ChatWallpaper.DEFAULT_PATTERN,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val notificationsEnabled: Boolean = true,
    val ringtoneName: String = "Emerald Chime",
    val vibrationEnabled: Boolean = true,
    val lastSeenPrivacy: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val onlinePrivacyEveryone: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val twoStepPinHash: String = "",
    val defaultDisappearingSeconds: Int = 0
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val WALLPAPER = stringPreferencesKey("wallpaper")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val RINGTONE_NAME = stringPreferencesKey("ringtone_name")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LAST_SEEN_PRIVACY = stringPreferencesKey("last_seen_privacy")
        val ONLINE_PRIVACY_EVERYONE = booleanPreferencesKey("online_privacy_everyone")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val TWO_STEP_PIN_HASH = stringPreferencesKey("two_step_pin_hash")
        val DEFAULT_DISAPPEARING = intPreferencesKey("default_disappearing_sec")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            darkMode = DarkModePreference.fromId(prefs[Keys.DARK_MODE] ?: DarkModePreference.SYSTEM.id),
            themePreset = ThemePreset.fromId(prefs[Keys.THEME_PRESET] ?: ThemePreset.EMERALD.id),
            wallpaper = ChatWallpaper.fromId(prefs[Keys.WALLPAPER] ?: ChatWallpaper.DEFAULT_PATTERN.id),
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            ringtoneName = prefs[Keys.RINGTONE_NAME] ?: "Emerald Chime",
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            lastSeenPrivacy = PrivacyVisibility.fromId(prefs[Keys.LAST_SEEN_PRIVACY] ?: PrivacyVisibility.EVERYONE.id),
            onlinePrivacyEveryone = prefs[Keys.ONLINE_PRIVACY_EVERYONE] ?: true,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            twoStepPinHash = prefs[Keys.TWO_STEP_PIN_HASH] ?: "",
            defaultDisappearingSeconds = prefs[Keys.DEFAULT_DISAPPEARING] ?: 0
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setDarkMode(mode: DarkModePreference) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode.id }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.dataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setWallpaper(wallpaper: ChatWallpaper) {
        context.dataStore.edit { it[Keys.WALLPAPER] = wallpaper.id }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setRingtoneName(name: String) {
        context.dataStore.edit { it[Keys.RINGTONE_NAME] = name }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setLastSeenPrivacy(visibility: PrivacyVisibility) {
        context.dataStore.edit { it[Keys.LAST_SEEN_PRIVACY] = visibility.id }
    }

    suspend fun setOnlinePrivacyEveryone(everyone: Boolean) {
        context.dataStore.edit { it[Keys.ONLINE_PRIVACY_EVERYONE] = everyone }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setTwoStepPinHash(hash: String) {
        context.dataStore.edit { it[Keys.TWO_STEP_PIN_HASH] = hash }
    }

    suspend fun setDefaultDisappearingSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_DISAPPEARING] = seconds }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
