package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiChatMessage
import com.example.data.local.AppSettings
import com.example.data.local.CallDirection
import com.example.data.local.CallRecordEntity
import com.example.data.local.CallType
import com.example.data.local.ChatEntity
import com.example.data.local.ChatWallpaper
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.PrivacyVisibility
import com.example.data.local.StatusPrivacy
import com.example.data.local.StatusType
import com.example.data.local.StatusUpdateEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.ChatXRepository
import com.example.data.repository.StorageStats
import com.example.security.SecretChatCrypto
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.DarkModePreference
import com.example.ui.theme.ThemePreset
import com.example.util.NotificationHelper
import com.example.util.VoiceAudioManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveCallState(
    val peerId: String,
    val peerName: String,
    val peerPhone: String,
    val callType: CallType,
    val direction: CallDirection,
    val isRingingIncoming: Boolean,
    val isConnected: Boolean,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = true
)

class ChatXViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ChatXRepository(application)
    val voiceAudioManager = VoiceAudioManager(application)

    val currentUser: StateFlow<UserProfileEntity?> = repo.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> = repo.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val contacts: StateFlow<List<UserProfileEntity>> = repo.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chats: StateFlow<List<ChatEntity>> = repo.observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statuses: StateFlow<List<StatusUpdateEntity>> = repo.observeActiveStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calls: StateFlow<List<CallRecordEntity>> = repo.observeCallHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0L, 0L))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()
    private var callTimerJob: Job? = null

    private val _aiMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "ai_welcome",
                isUser = false,
                text = "Hello! I'm the ChatX AI Assistant. Ask me to draft a message, translate to Hindi or Odia, or summarize your chats.\n\nNote: Live Gemini responses require enabling Firebase AI Logic."
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repo.purgeExpiredContent()
            val s = repo.settings.settingsFlow.first()
            if (s.onboardingCompleted && (s.biometricLockEnabled || s.twoStepPinHash.isNotBlank())) {
                _isLocked.value = true
            }
            refreshStorageStats()
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun verifyPinAndUnlock(pin: String): Boolean {
        val hash = SecretChatCrypto.hashPin(pin)
        return if (hash == settings.value.twoStepPinHash) {
            _isLocked.value = false
            true
        } else {
            _errorMessage.value = "Incorrect PIN. Please try again."
            false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun registerUser(
        phoneNumber: String,
        username: String,
        bio: String,
        photoUri: String?,
        twoStepPin: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.registerLocalUser(phoneNumber, username, bio, photoUri, twoStepPin)
            _isLoading.value = false
            res.onSuccess {
                refreshStorageStats()
                onDone()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to create profile"
            }
        }
    }

    fun updateProfile(username: String, bio: String, photoUri: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.updateProfile(username, bio, photoUri)
                .onSuccess { _toastMessage.value = "Profile updated" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Update failed" }
            _isLoading.value = false
        }
    }

    fun addContact(username: String, phone: String, onAdded: (UserProfileEntity) -> Unit = {}) {
        if (username.isBlank() || phone.isBlank()) {
            _errorMessage.value = "Name and phone number are required."
            return
        }
        viewModelScope.launch {
            repo.addContact(username, phone)
                .onSuccess {
                    _toastMessage.value = "${it.username} added to contacts"
                    onAdded(it)
                }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Could not add contact" }
        }
    }

    fun toggleBlockContact(uid: String, currentlyBlocked: Boolean) {
        viewModelScope.launch {
            repo.setContactBlocked(uid, !currentlyBlocked)
            _toastMessage.value = if (!currentlyBlocked) "Contact blocked" else "Contact unblocked"
        }
    }

    fun startDirectChat(peer: UserProfileEntity, isSecret: Boolean, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val chat = repo.startOrGetDirectChat(peer, isSecret, settings.value.defaultDisappearingSeconds)
            onOpened(chat.chatId)
        }
    }

    fun createGroupChat(title: String, members: List<UserProfileEntity>, onCreated: (String) -> Unit) {
        if (title.isBlank() || members.isEmpty()) {
            _errorMessage.value = "Enter a group name and select at least one contact."
            return
        }
        viewModelScope.launch {
            val chat = repo.createGroupChat(title, members)
            onCreated(chat.chatId)
        }
    }

    fun observeChat(chatId: String): Flow<ChatEntity?> = repo.observeChat(chatId)

    fun observeMessages(chat: ChatEntity): Flow<List<MessageEntity>> =
        repo.observeMessages(chat.chatId, chat.sessionKeyHex, chat.isSecret)

    fun markChatRead(chatId: String) {
        viewModelScope.launch { repo.markChatRead(chatId) }
    }

    fun sendTextMessage(chat: ChatEntity, text: String, replyTo: MessageEntity? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.sendMessage(
                chat = chat,
                type = MessageType.TEXT,
                plaintextContent = trimmed,
                replyTo = replyTo
            ).onSuccess {
                notifyFeedback()
                launch { repo.simulateDeliveryAndPeerReply(chat.chatId, trimmed) }
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Message failed to send"
            }
        }
    }

    fun sendMediaMessage(chat: ChatEntity, type: MessageType, uri: Uri, displayName: String? = null) {
        viewModelScope.launch {
            val label = when (type) {
                MessageType.IMAGE -> "Shared a photo"
                MessageType.VIDEO -> "Shared a video"
                MessageType.DOCUMENT -> displayName ?: "Document"
                else -> "Attachment"
            }
            repo.sendMessage(
                chat = chat,
                type = type,
                plaintextContent = label,
                mediaUri = uri.toString(),
                mediaName = displayName
            ).onSuccess {
                notifyFeedback()
                refreshStorageStats()
                launch { repo.simulateDeliveryAndPeerReply(chat.chatId, label) }
            }
        }
    }

    fun sendVoiceMessage(chat: ChatEntity, filePath: String, durationSec: Int) {
        viewModelScope.launch {
            repo.sendMessage(
                chat = chat,
                type = MessageType.VOICE,
                plaintextContent = "Voice message (${durationSec}s)",
                mediaUri = filePath,
                mediaName = "Voice note",
                mediaDurationSec = durationSec
            ).onSuccess {
                notifyFeedback()
                refreshStorageStats()
                launch { repo.simulateDeliveryAndPeerReply(chat.chatId, "Voice note") }
            }
        }
    }

    fun forwardMessage(targetChat: ChatEntity, sourceMessage: MessageEntity) {
        viewModelScope.launch {
            val type = MessageType.entries.firstOrNull { it.name == sourceMessage.type } ?: MessageType.TEXT
            repo.sendMessage(
                chat = targetChat,
                type = type,
                plaintextContent = sourceMessage.content,
                mediaUri = sourceMessage.mediaUri,
                mediaName = sourceMessage.mediaName,
                mediaDurationSec = sourceMessage.mediaDurationSec,
                isForwarded = true
            ).onSuccess {
                _toastMessage.value = "Forwarded to ${targetChat.title}"
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repo.deleteMessage(messageId)
        }
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            repo.clearChatMessages(chatId)
            _toastMessage.value = "Chat cleared"
        }
    }

    fun setDisappearingTimer(chatId: String, seconds: Int) {
        viewModelScope.launch {
            repo.setDisappearingTimer(chatId, seconds)
            val label = when (seconds) {
                0 -> "Off"
                86400 -> "24 hours"
                604800 -> "7 days"
                else -> "90 days"
            }
            _toastMessage.value = "Disappearing messages: $label"
        }
    }

    fun postStatus(
        type: StatusType,
        caption: String,
        mediaUri: String?,
        backgroundColorHex: Long,
        privacy: StatusPrivacy
    ) {
        viewModelScope.launch {
            repo.postStatusUpdate(type, caption, mediaUri, backgroundColorHex, privacy)
                .onSuccess { _toastMessage.value = "Status posted (expires in 24h)" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Failed to post status" }
        }
    }

    fun markStatusViewed(statusId: String) {
        viewModelScope.launch { repo.markStatusViewed(statusId) }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch { repo.deleteStatus(statusId) }
    }

    // Call management
    fun startOutgoingCall(peerId: String, peerName: String, peerPhone: String, callType: CallType) {
        callTimerJob?.cancel()
        _activeCall.value = ActiveCallState(
            peerId = peerId,
            peerName = peerName,
            peerPhone = peerPhone,
            callType = callType,
            direction = CallDirection.OUTGOING,
            isRingingIncoming = false,
            isConnected = false,
            isVideoEnabled = callType == CallType.VIDEO
        )
        callTimerJob = viewModelScope.launch {
            delay(1800)
            _activeCall.value = _activeCall.value?.copy(isConnected = true)
            while (_activeCall.value != null) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    if (it.isConnected) it.copy(durationSeconds = it.durationSeconds + 1) else it
                }
            }
        }
    }

    fun triggerIncomingCall(peer: UserProfileEntity, callType: CallType) {
        callTimerJob?.cancel()
        _activeCall.value = ActiveCallState(
            peerId = peer.uid,
            peerName = peer.username,
            peerPhone = peer.phoneNumber,
            callType = callType,
            direction = CallDirection.INCOMING,
            isRingingIncoming = true,
            isConnected = false,
            isVideoEnabled = callType == CallType.VIDEO
        )
        if (settings.value.vibrationEnabled) {
            NotificationHelper.triggerVibration(getApplication(), 150L)
        }
    }

    fun acceptIncomingCall() {
        val current = _activeCall.value ?: return
        callTimerJob?.cancel()
        _activeCall.value = current.copy(isRingingIncoming = false, isConnected = true)
        callTimerJob = viewModelScope.launch {
            while (_activeCall.value != null) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    if (it.isConnected) it.copy(durationSeconds = it.durationSeconds + 1) else it
                }
            }
        }
    }

    fun endActiveCall(declined: Boolean = false) {
        callTimerJob?.cancel()
        val call = _activeCall.value ?: return
        _activeCall.value = null
        val dir = if (declined && call.isRingingIncoming) CallDirection.MISSED else call.direction
        viewModelScope.launch {
            repo.recordCall(
                peerId = call.peerId,
                peerName = call.peerName,
                peerPhone = call.peerPhone,
                callType = call.callType,
                direction = dir,
                durationSeconds = call.durationSeconds
            )
        }
    }

    fun toggleCallMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleCallVideo() {
        _activeCall.value = _activeCall.value?.let { it.copy(isVideoEnabled = !it.isVideoEnabled) }
    }

    fun clearCallHistory() {
        viewModelScope.launch {
            repo.clearCallHistory()
            _toastMessage.value = "Call history cleared"
        }
    }

    // AI Assistant
    fun sendAiPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _aiLoading.value) return
        val userMsg = AiChatMessage(
            id = "ai_u_${UUID.randomUUID().toString().take(8)}",
            isUser = true,
            text = clean
        )
        _aiMessages.value = _aiMessages.value + userMsg
        _aiLoading.value = true
        viewModelScope.launch {
            val res = repo.askAiAssistant(_aiMessages.value, clean, settings.value.language)
            _aiLoading.value = false
            res.onSuccess { aiReply ->
                _aiMessages.value = _aiMessages.value + AiChatMessage(
                    id = "ai_m_${UUID.randomUUID().toString().take(8)}",
                    isUser = false,
                    text = aiReply.text
                )
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "AI Assistant error"
            }
        }
    }

    // Settings & Storage
    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repo.getStorageStats()
        }
    }

    fun clearMediaCache() {
        viewModelScope.launch {
            repo.clearMediaCache()
            refreshStorageStats()
            _toastMessage.value = "Media & voice cache cleared"
        }
    }

    fun setDarkMode(mode: DarkModePreference) = viewModelScope.launch { repo.settings.setDarkMode(mode) }
    fun setThemePreset(preset: ThemePreset) = viewModelScope.launch { repo.settings.setThemePreset(preset) }
    fun setWallpaper(wallpaper: ChatWallpaper) = viewModelScope.launch { repo.settings.setWallpaper(wallpaper) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { repo.settings.setLanguage(language) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { repo.settings.setNotificationsEnabled(enabled) }
    fun setRingtoneName(name: String) = viewModelScope.launch {
        repo.settings.setRingtoneName(name)
        NotificationHelper.playNotificationTone(getApplication())
    }
    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.settings.setVibrationEnabled(enabled)
        if (enabled) NotificationHelper.triggerVibration(getApplication(), 60L)
    }
    fun setLastSeenPrivacy(v: PrivacyVisibility) = viewModelScope.launch { repo.settings.setLastSeenPrivacy(v) }
    fun setOnlinePrivacyEveryone(everyone: Boolean) = viewModelScope.launch { repo.settings.setOnlinePrivacyEveryone(everyone) }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { repo.settings.setBiometricLock(enabled) }
    fun setTwoStepPin(pin: String) = viewModelScope.launch {
        val hash = if (pin.isBlank()) "" else SecretChatCrypto.hashPin(pin)
        repo.settings.setTwoStepPinHash(hash)
        _toastMessage.value = if (pin.isBlank()) "Two-step verification disabled" else "Two-step verification PIN updated"
    }
    fun setDefaultDisappearingSeconds(seconds: Int) = viewModelScope.launch {
        repo.settings.setDefaultDisappearingSeconds(seconds)
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logoutAndResetLocalSession()
            onDone()
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAccountAndWipeData()
            onDone()
        }
    }

    private fun notifyFeedback() {
        val s = settings.value
        if (s.vibrationEnabled) {
            NotificationHelper.triggerVibration(getApplication(), 25L)
        }
    }
}
