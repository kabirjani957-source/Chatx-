package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.navigation.ChatXAppRoot
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.OnboardingFlowScreen
import com.example.ui.theme.ChatXTheme
import com.example.ui.viewmodel.ChatXViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatXViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

            val strings = stringsFor(settings.language)

            CompositionLocalProvider(LocalChatXStrings provides strings) {
                ChatXTheme(
                    darkModePreference = settings.darkMode,
                    themePreset = settings.themePreset
                ) {
                    when {
                        !settings.onboardingCompleted || currentUser == null -> {
                            OnboardingFlowScreen(
                                selectedLanguage = settings.language,
                                onLanguageSelected = { viewModel.setLanguage(it) },
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                onClearError = { viewModel.clearError() },
                                onCompleteRegistration = { phone, username, bio, photoUri, pin ->
                                    viewModel.registerUser(phone, username, bio, photoUri, pin) {}
                                }
                            )
                        }

                        isLocked -> {
                            AppLockScreen(
                                biometricEnabled = settings.biometricLockEnabled,
                                hasPin = settings.twoStepPinHash.isNotEmpty(),
                                onBiometricUnlock = { viewModel.unlockApp() },
                                onPinUnlock = { pin -> viewModel.verifyPinAndUnlock(pin) },
                                errorMessage = errorMessage,
                                onClearError = { viewModel.clearError() }
                            )
                        }

                        else -> {
                            ChatXAppRoot(
                                viewModel = viewModel,
                                onLoggedOutOrDeleted = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
