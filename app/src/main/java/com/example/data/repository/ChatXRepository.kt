package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AiRepository
import com.example.data.ai.AiResponse
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.CallDirection
import com.example.data.local.CallRecordEntity
import com.example.data.local.CallType
import com.example.data.local.ChatEntity
import com.example.data.local.ChatXDao
import com.example.data.local.ChatXDatabase
import com.example.data.local.DeliveryStatus
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.SeedDataHelper
import com.example.data.local.SettingsDataStore
import com.example.data.local.StatusPrivacy
import com.example.data.local.StatusType
import com.example.data.local.StatusUpdateEntity
import com.example.data.local.UserProfileEntity
import com.example.security.SecretChatCrypto
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

data class StorageStats(
    val totalMessages: Int,
    val mediaCount: Int,
    val voiceFolderBytes: Long,
    val databaseBytes: Long
)

class ChatXRepository(
    private val context: Context,
    private val dao: ChatXDao = ChatXDatabase.getInstance(context).chatXDao(),
    val settings: SettingsDataStore = SettingsDataStore(context),
    private val aiRepository: AiRepository = PlaceholderAiRepository()
) {

    fun observeCurrentUser(): Flow<UserProfileEntity?> = dao.observeCurrentUser()
    fun observeContacts(): Flow<List<UserProfileEntity>> = dao.observeContacts()
    fun observeChats(): Flow<List<ChatEntity>> = dao.observeAllChats()
    fun observeChat(chatId: String): Flow<ChatEntity?> = dao.observeChat(chatId)
    fun observeCallHistory(): Flow<List<CallRecordEntity>> = dao.observeCallHistory()

    fun observeActiveStatuses(): Flow<List<StatusUpdateEntity>> {
        return dao.observeActiveStatuses(System.currentTimeMillis())
    }

    fun observeMessages(chatId: String, sessionKeyHex: String, isSecret: Boolean): Flow<List<MessageEntity>> {
        return dao.observeMessagesForChat(chatId).map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.expiresAtMillis == null || it.expiresAtMillis > now }
                .map { msg ->
                    if (isSecret && !msg.isDeleted && msg.content.startsWith("E2EE:")) {
                        msg.copy(content = SecretChatCrypto.decrypt(msg.content, sessionKeyHex))
                    } else {
                        msg
                    }
                }
        }
    }

    suspend fun registerLocalUser(
        phoneNumber: String,
        username: String,
        bio: String,
        photoUri: String?,
        twoStepPin: String?
    ): Result<UserProfileEntity> {
        return try {
            val uid = "user_${UUID.randomUUID().toString().take(8)}"
            val keys = SecretChatCrypto.generateEcKeyPair()
            val user = UserProfileEntity(
                uid = uid,
                phoneNumber = phoneNumber.trim(),
                username = username.trim(),
                bio = bio.trim().ifEmpty { "Hey there! I am using ChatX." },
                photoUri = photoUri,
                publicKeyBase64 = keys.publicKeyBase64,
                privateKeyBase64 = keys.privateKeyBase64,
                isCurrentUser = true,
                isOnline = true,
                lastSeenMillis = System.currentTimeMillis()
            )
            dao.upsertUser(user)
            if (!twoStepPin.isNullOrBlank() && twoStepPin.length >= 4) {
                settings.setTwoStepPinHash(SecretChatCrypto.hashPin(twoStepPin))
            }
            SeedDataHelper.seedInitialLocalWorkspace(dao, uid, keys.privateKeyBase64)
            settings.setOnboardingCompleted(true)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(username: String, bio: String, photoUri: String?): Result<Unit> {
        return try {
            val current = dao.getCurrentUser() ?: return Result.failure(IllegalStateException("No active profile"))
            dao.upsertUser(
                current.copy(
                    username = username.trim().ifEmpty { current.username },
                    bio = bio.trim(),
                    photoUri = photoUri
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addContact(username: String, phoneNumber: String): Result<UserProfileEntity> {
        return try {
            val keys = SecretChatCrypto.generateEcKeyPair()
            val uid = "contact_${UUID.randomUUID().toString().take(8)}"
            val colors = listOf(0xFF008069, 0xFF0D9488, 0xFF15803D, 0xFF047857, 0xFF059669)
            val contact = UserProfileEntity(
                uid = uid,
                phoneNumber = phoneNumber.trim(),
                username = username.trim(),
                bio = "Hey there! I am using ChatX.",
                publicKeyBase64 = keys.publicKeyBase64,
                privateKeyBase64 = keys.privateKeyBase64,
                isCurrentUser = false,
                isOnline = true,
                avatarColorHex = colors.random()
            )
            dao.upsertUser(contact)
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setContactBlocked(uid: String, blocked: Boolean) {
        dao.setUserBlocked(uid, blocked)
    }

    suspend fun startOrGetDirectChat(peer: UserProfileEntity, isSecret: Boolean, defaultDisappearingSec: Int): ChatEntity {
        val current = dao.getCurrentUser()
        val myUid = current?.uid ?: "me"
        val targetChatId = if (isSecret) "secret_${peer.uid}" else "chat_${peer.uid}"
        val existing = dao.getChat(targetChatId)
        if (existing != null) return existing

        val sessionKeyHex = if (isSecret) {
            if (current != null && current.privateKeyBase64.isNotBlank() && peer.publicKeyBase64.isNotBlank()) {
                SecretChatCrypto.deriveSharedKeyHex(current.privateKeyBase64, peer.publicKeyBase64)
            } else {
                SecretChatCrypto.generateFallbackSessionKeyHex()
            }
        } else ""
        val safetyNumber = if (isSecret) SecretChatCrypto.computeSafetyNumber(sessionKeyHex, peer.uid) else ""

        val chat = ChatEntity(
            chatId = targetChatId,
            title = peer.username,
            subtitle = if (isSecret) "End-to-End Encrypted" else peer.phoneNumber,
            isGroup = false,
            isSecret = isSecret,
            peerId = peer.uid,
            participantIdsCsv = "$myUid,${peer.uid}",
            participantNamesCsv = "You,${peer.username}",
            avatarColorHex = peer.avatarColorHex,
            photoUri = peer.photoUri,
            lastMessagePreview = if (isSecret) "Secret chat initialized (AES-256-GCM)" else "Tap to start chatting",
            lastMessageTimeMillis = System.currentTimeMillis(),
            disappearingSeconds = if (isSecret && defaultDisappearingSec == 0) 86400 else defaultDisappearingSec,
            sessionKeyHex = sessionKeyHex,
            safetyNumber = safetyNumber
        )
        dao.upsertChat(chat)
        return chat
    }

    suspend fun createGroupChat(title: String, members: List<UserProfileEntity>): ChatEntity {
        val current = dao.getCurrentUser()
        val myUid = current?.uid ?: "me"
        val chatId = "group_${UUID.randomUUID().toString().take(8)}"
        val ids = (listOf(myUid) + members.map { it.uid }).joinToString(",")
        val names = (listOf("You") + members.map { it.username.substringBefore(" ") }).joinToString(", ")
        val chat = ChatEntity(
            chatId = chatId,
            title = title.trim(),
            subtitle = names,
            isGroup = true,
            isSecret = false,
            participantIdsCsv = ids,
            participantNamesCsv = names,
            avatarColorHex = 0xFF059669,
            lastMessagePreview = "Group created with ${members.size + 1} members",
            lastMessageTimeMillis = System.currentTimeMillis()
        )
        dao.upsertChat(chat)
        return chat
    }

    suspend fun sendMessage(
        chat: ChatEntity,
        type: MessageType,
        plaintextContent: String,
        mediaUri: String? = null,
        mediaName: String? = null,
        mediaDurationSec: Int = 0,
        replyTo: MessageEntity? = null,
        isForwarded: Boolean = false
    ): Result<MessageEntity> {
        return try {
            val current = dao.getCurrentUser()
            val myUid = current?.uid ?: "me"
            val myName = current?.username ?: "You"
            val now = System.currentTimeMillis()
            val storedContent = if (chat.isSecret && plaintextContent.isNotBlank()) {
                SecretChatCrypto.encrypt(plaintextContent, chat.sessionKeyHex)
            } else {
                plaintextContent
            }
            val expiresAt = if (chat.disappearingSeconds > 0) now + (chat.disappearingSeconds * 1000L) else null
            val message = MessageEntity(
                messageId = "msg_${UUID.randomUUID().toString().take(10)}",
                chatId = chat.chatId,
                senderId = myUid,
                senderName = myName,
                isFromMe = true,
                type = type.name,
                content = storedContent,
                mediaUri = mediaUri,
                mediaName = mediaName,
                mediaDurationSec = mediaDurationSec,
                replyToMessageId = replyTo?.messageId,
                replyToPreview = replyTo?.content?.take(60),
                isForwarded = isForwarded,
                deliveryStatus = DeliveryStatus.SENT.name,
                expiresAtMillis = expiresAt,
                createdAtMillis = now
            )
            dao.upsertMessage(message)

            val preview = when {
                chat.isSecret -> "Encrypted message"
                type == MessageType.IMAGE -> "Photo"
                type == MessageType.VIDEO -> "Video"
                type == MessageType.VOICE -> "Voice message (${mediaDurationSec}s)"
                type == MessageType.DOCUMENT -> mediaName ?: "Document"
                else -> plaintextContent
            }
            dao.upsertChat(
                chat.copy(
                    lastMessagePreview = preview,
                    lastMessageTimeMillis = now
                )
            )
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun simulateDeliveryAndPeerReply(chatId: String, lastUserText: String) {
        delay(450)
        dao.markOutgoingMessagesStatus(chatId, DeliveryStatus.DELIVERED.name)
        delay(650)
        dao.markOutgoingMessagesStatus(chatId, DeliveryStatus.READ.name)
        val chat = dao.getChat(chatId) ?: return
        if (chat.peerId != null) {
            val peer = dao.getUserById(chat.peerId)
            if (peer?.isBlocked == true) return
        }
        dao.setChatTyping(chatId, true)
        delay(1100)
        dao.setChatTyping(chatId, false)

        val now = System.currentTimeMillis()
        val peerName = if (chat.isGroup) "Aarav Sharma" else chat.title
        val peerId = chat.peerId ?: "contact_aarav"
        val replyPlain = when {
            lastUserText.contains("hi", ignoreCase = true) || lastUserText.contains("hello", ignoreCase = true) ->
                "Hey! Great to hear from you on ChatX."
            lastUserText.contains("call", ignoreCase = true) ->
                "Sure! Tap the audio or video call button above anytime."
            chat.isSecret ->
                "Received securely via AES-256-GCM!"
            else ->
                "Got it: \"${lastUserText.take(40)}\""
        }
        val stored = if (chat.isSecret) {
            SecretChatCrypto.encrypt(replyPlain, chat.sessionKeyHex)
        } else {
            replyPlain
        }
        val expiresAt = if (chat.disappearingSeconds > 0) now + (chat.disappearingSeconds * 1000L) else null
        dao.upsertMessage(
            MessageEntity(
                messageId = "msg_${UUID.randomUUID().toString().take(10)}",
                chatId = chat.chatId,
                senderId = peerId,
                senderName = peerName,
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = stored,
                deliveryStatus = DeliveryStatus.READ.name,
                expiresAtMillis = expiresAt,
                createdAtMillis = now
            )
        )
        dao.upsertChat(
            chat.copy(
                lastMessagePreview = if (chat.isSecret) "Encrypted message" else replyPlain,
                lastMessageTimeMillis = now
            )
        )
    }

    suspend fun markChatRead(chatId: String) {
        dao.markChatRead(chatId)
    }

    suspend fun setDisappearingTimer(chatId: String, seconds: Int) {
        dao.setChatDisappearingTimer(chatId, seconds)
    }

    suspend fun deleteMessage(messageId: String) {
        dao.softDeleteMessage(messageId)
    }

    suspend fun clearChatMessages(chatId: String) {
        dao.deleteMessagesForChat(chatId)
        val chat = dao.getChat(chatId) ?: return
        dao.upsertChat(chat.copy(lastMessagePreview = "Messages cleared"))
    }

    suspend fun purgeExpiredContent() {
        val now = System.currentTimeMillis()
        dao.purgeExpiredMessages(now)
        dao.purgeExpiredStatuses(now)
    }

    suspend fun postStatusUpdate(
        type: StatusType,
        caption: String,
        mediaUri: String?,
        backgroundColorHex: Long,
        privacy: StatusPrivacy
    ): Result<Unit> {
        return try {
            val current = dao.getCurrentUser()
            val now = System.currentTimeMillis()
            val status = StatusUpdateEntity(
                statusId = "st_${UUID.randomUUID().toString().take(8)}",
                userId = current?.uid ?: "me",
                userName = current?.username ?: "Me",
                phoneNumber = current?.phoneNumber ?: "",
                isMine = true,
                type = type.name,
                caption = caption,
                mediaUri = mediaUri,
                backgroundColorHex = backgroundColorHex,
                privacy = privacy.name,
                viewCount = 0,
                isViewed = false,
                createdAtMillis = now,
                expiresAtMillis = now + 86_400_000L
            )
            dao.upsertStatus(status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(statusId: String) {
        dao.markStatusViewed(statusId)
    }

    suspend fun deleteStatus(statusId: String) {
        dao.deleteStatus(statusId)
    }

    suspend fun recordCall(
        peerId: String,
        peerName: String,
        peerPhone: String,
        callType: CallType,
        direction: CallDirection,
        durationSeconds: Int
    ) {
        dao.insertCall(
            CallRecordEntity(
                callId = "call_${UUID.randomUUID().toString().take(8)}",
                peerId = peerId,
                peerName = peerName,
                peerPhone = peerPhone,
                callType = callType.name,
                direction = direction.name,
                durationSeconds = durationSeconds,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCallHistory() {
        dao.clearCallHistory()
    }

    suspend fun getStorageStats(): StorageStats {
        val totalMsgs = dao.getTotalMessageCount()
        val mediaMsgs = dao.getMediaMessageCount()
        val voiceDir = File(context.filesDir, "voice_notes")
        val voiceBytes = voiceDir.listFiles()?.sumOf { it.length() } ?: 0L
        val dbFile = context.getDatabasePath("chatx_local.db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        return StorageStats(
            totalMessages = totalMsgs,
            mediaCount = mediaMsgs,
            voiceFolderBytes = voiceBytes,
            databaseBytes = dbBytes
        )
    }

    suspend fun clearMediaCache() {
        val voiceDir = File(context.filesDir, "voice_notes")
        voiceDir.listFiles()?.forEach { it.delete() }
        dao.clearMediaMessages()
    }

    suspend fun askAiAssistant(
        history: List<AiChatMessage>,
        prompt: String,
        language: AppLanguage
    ): Result<AiResponse> {
        return aiRepository.sendMessage(history, prompt, language)
    }

    suspend fun logoutAndResetLocalSession() {
        settings.setOnboardingCompleted(false)
        settings.setBiometricLock(false)
        settings.setTwoStepPinHash("")
    }

    suspend fun deleteAccountAndWipeData() {
        dao.wipeMessages()
        dao.wipeChats()
        dao.wipeStatuses()
        dao.wipeCalls()
        dao.wipeUsers()
        val voiceDir = File(context.filesDir, "voice_notes")
        voiceDir.listFiles()?.forEach { it.delete() }
        settings.clearAll()
    }
}
