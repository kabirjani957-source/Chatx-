package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.data.local.ChatWallpaper
import com.example.data.local.PrivacyVisibility
import com.example.data.local.UserProfileEntity
import com.example.data.repository.StorageStats
import com.example.security.BiometricHelper
import com.example.ui.components.ChatXAvatar
import com.example.ui.components.ServiceNoticeBanner
import com.example.ui.components.formatBytes
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.theme.CallDeclineRed
import com.example.ui.theme.DarkModePreference
import com.example.ui.theme.ThemePreset

@Composable
fun SettingsScreen(
    user: UserProfileEntity?,
    settings: AppSettings,
    contacts: List<UserProfileEntity>,
    storageStats: StorageStats,
    onBack: () -> Unit,
    onOpenQrProfile: () -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onSetDarkMode: (DarkModePreference) -> Unit,
    onSetThemePreset: (ThemePreset) -> Unit,
    onSetWallpaper: (ChatWallpaper) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetRingtone: (String) -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onSetLastSeenPrivacy: (PrivacyVisibility) -> Unit,
    onSetOnlinePrivacy: (Boolean) -> Unit,
    onToggleBlockContact: (UserProfileEntity) -> Unit,
    onSetBiometricLock: (Boolean) -> Unit,
    onSetTwoStepPin: (String) -> Unit,
    onSetDefaultDisappearing: (Int) -> Unit,
    onRefreshStorage: () -> Unit,
    onClearMediaCache: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onToast: (String) -> Unit
) {
    val strings = LocalChatXStrings.current
    val context = LocalContext.current

    var showEditProfile by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onRefreshStorage() }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(strings.settings, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditProfile = true }
                        .padding(16.dp)
                        .testTag("settings_profile_header"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatXAvatar(
                        name = user?.username ?: "User",
                        photoUri = user?.photoUri,
                        colorHex = user?.avatarColorHex ?: 0xFF008069,
                        size = 62.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.username ?: "ChatX User", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = user?.bio ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = user?.phoneNumber ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenQrProfile, modifier = Modifier.testTag("settings_qr_button")) {
                        Icon(Icons.Default.QrCode2, contentDescription = strings.qrProfile, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showEditProfile = true }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.editProfile)
                    }
                }
                ServiceNoticeBanner(
                    text = strings.offlineFirebaseNotice,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            item { SectionLabel(strings.appearance) }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = strings.darkMode,
                    subtitle = settings.darkMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    testTag = "setting_dark_mode",
                    onClick = { showDarkModeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = strings.customTheme,
                    subtitle = settings.themePreset.displayName,
                    testTag = "setting_theme_preset",
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Wallpaper,
                    title = strings.chatWallpaper,
                    subtitle = settings.wallpaper.label,
                    testTag = "setting_wallpaper",
                    onClick = { showWallpaperDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = strings.language,
                    subtitle = settings.language.nativeName,
                    testTag = "setting_language",
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider()
            }

            item { SectionLabel("Notifications & Sound") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = strings.notifications,
                    subtitle = if (settings.notificationsEnabled) "Enabled" else "Muted",
                    checked = settings.notificationsEnabled,
                    testTag = "setting_notifications_switch",
                    onCheckedChange = onSetNotifications
                )
                SettingsItem(
                    icon = Icons.Default.MusicNote,
                    title = strings.ringtone,
                    subtitle = settings.ringtoneName,
                    testTag = "setting_ringtone",
                    onClick = { showRingtoneDialog = true }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = strings.vibration,
                    subtitle = if (settings.vibrationEnabled) "Haptic feedback active" else "Off",
                    checked = settings.vibrationEnabled,
                    testTag = "setting_vibration_switch",
                    onCheckedChange = onSetVibration
                )
                HorizontalDivider()
            }

            item { SectionLabel(strings.privacySecurity) }
            item {
                SettingsItem(
                    icon = Icons.Default.Visibility,
                    title = strings.lastSeenPrivacy,
                    subtitle = settings.lastSeenPrivacy.label,
                    onClick = { showLastSeenDialog = true }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.ColorLens,
                    title = strings.onlinePrivacy,
                    subtitle = if (settings.onlinePrivacyEveryone) "Everyone" else "Same as Last Seen",
                    checked = settings.onlinePrivacyEveryone,
                    onCheckedChange = onSetOnlinePrivacy
                )
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = strings.blockedContacts,
                    subtitle = "${contacts.count { it.isBlocked }} blocked",
                    onClick = { showBlockedDialog = true }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    title = strings.biometricLock,
                    subtitle = if (settings.biometricLockEnabled) "Enabled on launch" else "Off",
                    checked = settings.biometricLockEnabled,
                    testTag = "setting_biometric_switch",
                    onCheckedChange = { enable ->
                        if (!enable) {
                            onSetBiometricLock(false)
                        } else if (BiometricHelper.isBiometricAvailable(context)) {
                            BiometricHelper.authenticate(
                                context = context,
                                title = "Enable Biometric Lock",
                                subtitle = "Confirm your fingerprint or device lock",
                                negativeButtonText = "Cancel",
                                onSuccess = { onSetBiometricLock(true) },
                                onFallbackToPin = { onSetBiometricLock(true) },
                                onError = { onToast(it) }
                            )
                        } else {
                            onSetBiometricLock(true)
                            onToast("Biometric Lock enabled (using PIN fallback if no sensor enrolled)")
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = strings.twoStepVerification,
                    subtitle = if (settings.twoStepPinHash.isNotEmpty()) "PIN enabled" else "Off",
                    testTag = "setting_two_step_pin",
                    onClick = { showPinDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = strings.disappearingMessages,
                    subtitle = if (settings.defaultDisappearingSeconds == 0) "Off" else "${settings.defaultDisappearingSeconds / 3600} hours",
                    onClick = { showDisappearingDialog = true }
                )
                HorizontalDivider()
            }

            item { SectionLabel("Storage & Account") }
            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = strings.storageManagement,
                    subtitle = "${storageStats.totalMessages} messages • ${formatBytes(storageStats.databaseBytes + storageStats.voiceFolderBytes)}",
                    testTag = "setting_storage",
                    onClick = { showStorageDialog = true }
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = strings.logout,
                    subtitle = "Sign out of local session",
                    testTag = "setting_logout",
                    onClick = onLogout
                )
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = strings.deleteAccount,
                    subtitle = "Permanently wipe local account & all chats",
                    titleColor = CallDeclineRed,
                    testTag = "setting_delete_account",
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showEditProfile && user != null) {
        var name by remember { mutableStateOf(user.username) }
        var bio by remember { mutableStateOf(user.bio) }
        var photoUri by remember { mutableStateOf(user.photoUri) }
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) photoUri = uri.toString()
        }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text(strings.editProfile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        ChatXAvatar(name = name, photoUri = photoUri, colorHex = user.avatarColorHex, size = 76.dp)
                    }
                    TextButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Change Photo")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.usernameLabel) }, singleLine = true)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text(strings.bioLabel) }, maxLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = { onUpdateProfile(name, bio, photoUri); showEditProfile = false }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showDarkModeDialog) {
        ChoiceDialog(
            title = strings.darkMode,
            options = DarkModePreference.entries.map { it to it.name.lowercase().replaceFirstChar(Char::uppercase) },
            selected = settings.darkMode,
            onSelect = { onSetDarkMode(it); showDarkModeDialog = false },
            onDismiss = { showDarkModeDialog = false }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(strings.customTheme) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemePreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetThemePreset(preset); showThemeDialog = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(preset.lightPrimary)
                                    .border(2.dp, if (settings.themePreset == preset) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(preset.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showWallpaperDialog) {
        ChoiceDialog(
            title = strings.chatWallpaper,
            options = ChatWallpaper.entries.map { it to it.label },
            selected = settings.wallpaper,
            onSelect = { onSetWallpaper(it); showWallpaperDialog = false },
            onDismiss = { showWallpaperDialog = false }
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = strings.language,
            options = AppLanguage.entries.map { it to it.nativeName },
            selected = settings.language,
            onSelect = { onSetLanguage(it); showLanguageDialog = false },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showRingtoneDialog) {
        ChoiceDialog(
            title = strings.ringtone,
            options = listOf("Emerald Chime", "Soft Pulse", "Morning Breeze", "Classic Bell").map { it to it },
            selected = settings.ringtoneName,
            onSelect = { onSetRingtone(it); showRingtoneDialog = false },
            onDismiss = { showRingtoneDialog = false }
        )
    }

    if (showLastSeenDialog) {
        ChoiceDialog(
            title = strings.lastSeenPrivacy,
            options = PrivacyVisibility.entries.map { it to it.label },
            selected = settings.lastSeenPrivacy,
            onSelect = { onSetLastSeenPrivacy(it); showLastSeenDialog = false },
            onDismiss = { showLastSeenDialog = false }
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(strings.blockedContacts) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contacts.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.username, style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = { onToggleBlockContact(c) }) {
                                Text(if (c.isBlocked) "Unblock" else "Block")
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlockedDialog = false }) { Text("Done") } }
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.twoStepVerification) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set a 4-6 digit PIN to lock ChatX when opening the app.")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("New PIN (4-6 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (settings.twoStepPinHash.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onSetTwoStepPin(""); showPinDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disable PIN")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSetTwoStepPin(pin); showPinDialog = false },
                    enabled = pin.length >= 4
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showDisappearingDialog) {
        ChoiceDialog(
            title = strings.disappearingMessages,
            options = listOf(0 to "Off", 86400 to "24 hours", 604800 to "7 days"),
            selected = settings.defaultDisappearingSeconds,
            onSelect = { onSetDefaultDisappearing(it); showDisappearingDialog = false },
            onDismiss = { showDisappearingDialog = false }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text(strings.storageManagement) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Messages: ${storageStats.totalMessages}")
                    Text("Media & Voice Items: ${storageStats.mediaCount}")
                    Text("Voice Notes Disk Usage: ${formatBytes(storageStats.voiceFolderBytes)}")
                    Text("Local SQLite DB Size: ${formatBytes(storageStats.databaseBytes)}")
                }
            },
            confirmButton = {
                Button(onClick = { onClearMediaCache(); showStorageDialog = false }) {
                    Text("Clear Media Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteAccount) },
            text = { Text("This will permanently erase your local ChatX profile, messages, statuses, and call history from this device.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CallDeclineRed),
                    onClick = { showDeleteConfirm = false; onDeleteAccount() }
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (titleColor == CallDeclineRed) CallDeclineRed else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        if (value == selected) {
                            Text("Selected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
