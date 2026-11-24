package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.security.BiometricHelper
import com.example.ui.components.ServiceNoticeBanner
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    hasPin: Boolean,
    onBiometricUnlock: () -> Unit,
    onPinUnlock: (String) -> Boolean,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var promptMessage by remember { mutableStateOf<String?>(null) }

    fun launchBiometric() {
        BiometricHelper.authenticate(
            context = context,
            title = "Unlock ChatX",
            subtitle = "Verify your identity to access messages",
            negativeButtonText = "Use PIN",
            onSuccess = onBiometricUnlock,
            onFallbackToPin = { promptMessage = "Enter your Two-Step Verification PIN below." },
            onError = { promptMessage = it }
        )
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
            launchBiometric()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("ChatX Locked", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Connect. Chat. Enjoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))

                if (biometricEnabled) {
                    Button(
                        onClick = { launchBiometric() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("biometric_unlock_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock with Fingerprint / Device")
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (hasPin) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                pinInput = it
                                onClearError()
                            }
                        },
                        label = { Text("Enter Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_pin_input")
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onPinUnlock(pinInput) },
                        enabled = pinInput.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pin_unlock_button")
                    ) {
                        Text("Unlock with PIN")
                    }
                } else if (!biometricEnabled || !BiometricHelper.isBiometricAvailable(context)) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBiometricUnlock,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue (No device biometrics enrolled)")
                    }
                }

                (errorMessage ?: promptMessage)?.let { msg ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingFlowScreen(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onCompleteRegistration: (
        phoneNumber: String,
        username: String,
        bio: String,
        photoUri: String?,
        twoStepPin: String?
    ) -> Unit
) {
    val strings = LocalChatXStrings.current
    val context = LocalContext.current

    var step by remember { mutableIntStateOf(0) } // 0 = phone, 1 = otp, 2 = profile
    var countryCode by remember { mutableStateOf("+91") }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var phoneDigits by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var localError by remember { mutableStateOf<String?>(null) }

    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("Hey there! I am using ChatX.") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var twoStepPin by remember { mutableStateOf("") }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val fullPhone = "$countryCode ${phoneDigits.trim()}"
        NotificationHelper.showLocalNotification(
            context,
            "ChatX Verification Code",
            "Your local verification code for $fullPhone is $generatedCode",
            notificationId = 2001
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedPhotoUri = uri
    }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000)
            resendSeconds--
        }
    }

    fun issueVerificationCode() {
        val code = (100000..999999).random().toString()
        generatedCode = code
        resendSeconds = 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val fullPhone = "$countryCode ${phoneDigits.trim()}"
            NotificationHelper.showLocalNotification(
                context,
                "ChatX Verification Code",
                "Your local verification code for $fullPhone is $code",
                notificationId = 2001
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Language selector strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        IconButton(onClick = { step--; localError = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { onLanguageSelected(lang) },
                                label = { Text(lang.nativeName, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Step Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (idx == step) 32.dp else 14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx <= step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (step) {
                    0 -> {
                        Image(
                            painter = painterResource(R.drawable.chatx_welcome_hero_1795500346459),
                            contentDescription = "ChatX Secure Messaging Illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(176.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        ServiceNoticeBanner(text = strings.offlineFirebaseNotice)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = strings.verifyPhoneTitle,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.enterPhonePrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                OutlinedButton(
                                    onClick = { countryMenuExpanded = true },
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text(countryCode, fontWeight = FontWeight.SemiBold)
                                }
                                DropdownMenu(
                                    expanded = countryMenuExpanded,
                                    onDismissRequest = { countryMenuExpanded = false }
                                ) {
                                    listOf("+91" to "India (+91)", "+1" to "USA (+1)", "+44" to "UK (+44)", "+971" to "UAE (+971)")
                                        .forEach { (code, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    countryCode = code
                                                    countryMenuExpanded = false
                                                }
                                            )
                                        }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedTextField(
                                value = phoneDigits,
                                onValueChange = { input ->
                                    if (input.length <= 15 && input.all { it.isDigit() || it == ' ' }) {
                                        phoneDigits = input
                                        localError = null
                                    }
                                },
                                label = { Text("Mobile Number") },
                                placeholder = { Text("9876543210") },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("phone_number_input")
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val cleaned = phoneDigits.filter(Char::isDigit)
                                if (cleaned.length < 7) {
                                    localError = "Please enter a valid mobile number (at least 7 digits)."
                                } else {
                                    localError = null
                                    issueVerificationCode()
                                    step = 1
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("send_otp_button")
                        ) {
                            Text(strings.sendOtp)
                        }
                    }

                    1 -> {
                        Surface(
                            modifier = Modifier.size(68.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Verification",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Local Verification Code",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Confirming $countryCode ${phoneDigits.trim()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))

                        // Transparent local code box since Firebase SMS OTP was declined
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Local Verification Code (SMS requires Firebase Auth)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = generatedCode,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        letterSpacing = 4.dp.value.let { androidx.compose.ui.unit.TextUnit.Unspecified }
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all(Char::isDigit)) {
                                    otpInput = it
                                    localError = null
                                }
                            },
                            label = { Text("Enter 6-digit code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input")
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { otpInput = generatedCode; localError = null }
                            ) {
                                Text("Auto-fill code")
                            }
                            TextButton(
                                onClick = { issueVerificationCode() },
                                enabled = resendSeconds == 0
                            ) {
                                Text(if (resendSeconds > 0) "Resend in ${resendSeconds}s" else "Resend Code")
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (otpInput == generatedCode) {
                                    localError = null
                                    step = 2
                                } else {
                                    localError = "Verification code does not match. Please check and retry."
                                }
                            },
                            enabled = otpInput.length == 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("verify_otp_button")
                        ) {
                            Text(strings.verifyOtp)
                        }
                    }

                    2 -> {
                        Text(
                            text = strings.createProfileTitle,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Add a profile photo, display name, and bio for ChatX.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_photo_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Selected profile photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Add profile photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Text(
                                        text = "Photo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                localError = null
                                onClearError()
                            },
                            label = { Text(strings.usernameLabel) },
                            placeholder = { Text("e.g. Kabir Jani") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_username_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text(strings.bioLabel) },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_bio_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = twoStepPin,
                            onValueChange = {
                                if (it.length <= 6 && it.all(Char::isDigit)) twoStepPin = it
                            },
                            label = { Text("Optional 2-Step Security PIN (4-6 digits)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_pin_input")
                        )

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (username.trim().length < 2) {
                                    localError = "Please enter a username (at least 2 characters)."
                                } else {
                                    onCompleteRegistration(
                                        "$countryCode ${phoneDigits.trim()}",
                                        username,
                                        bio,
                                        selectedPhotoUri?.toString(),
                                        twoStepPin.takeIf { it.length >= 4 }
                                    )
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("complete_registration_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.continueText)
                            }
                        }
                    }
                }

                (localError ?: errorMessage)?.let { err ->
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
