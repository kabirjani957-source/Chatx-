package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE, DOCUMENT, SYSTEM
}

enum class DeliveryStatus {
    SENDING, SENT, DELIVERED, READ
}

enum class StatusType {
    TEXT, IMAGE, VIDEO
}

enum class StatusPrivacy(val label: String) {
    ALL_CONTACTS("My Contacts"),
    EXCEPT("My Contacts Except..."),
    ONLY_SHARE_WITH("Only Share With...")
}

enum class CallType {
    AUDIO, VIDEO
}

enum class CallDirection {
    INCOMING, OUTGOING, MISSED
}

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val phoneNumber: String,
    val username: String,
    val bio: String = "Hey there! I am using ChatX.",
    val photoUri: String? = null,
    val publicKeyBase64: String = "",
    val privateKeyBase64: String = "",
    val isCurrentUser: Boolean = false,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = true,
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val avatarColorHex: Long = 0xFF00A884
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val title: String,
    val subtitle: String = "",
    val isGroup: Boolean = false,
    val isSecret: Boolean = false,
    val peerId: String? = null,
    val participantIdsCsv: String = "",
    val participantNamesCsv: String = "",
    val avatarColorHex: Long = 0xFF00A884,
    val photoUri: String? = null,
    val lastMessagePreview: String = "",
    val lastMessageTimeMillis: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isTyping: Boolean = false,
    val disappearingSeconds: Int = 0,
    val sessionKeyHex: String = "",
    val safetyNumber: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val type: String = MessageType.TEXT.name,
    val content: String,
    val mediaUri: String? = null,
    val mediaName: String? = null,
    val mediaDurationSec: Int = 0,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val isForwarded: Boolean = false,
    val deliveryStatus: String = DeliveryStatus.SENT.name,
    val isDeleted: Boolean = false,
    val expiresAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "statuses")
data class StatusUpdateEntity(
    @PrimaryKey val statusId: String,
    val userId: String,
    val userName: String,
    val phoneNumber: String,
    val isMine: Boolean = false,
    val type: String = StatusType.TEXT.name,
    val caption: String,
    val mediaUri: String? = null,
    val backgroundColorHex: Long = 0xFF008069,
    val privacy: String = StatusPrivacy.ALL_CONTACTS.name,
    val viewCount: Int = 0,
    val isViewed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + 86_400_000L
)

@Entity(tableName = "calls")
data class CallRecordEntity(
    @PrimaryKey val callId: String,
    val peerId: String,
    val peerName: String,
    val peerPhone: String,
    val callType: String = CallType.AUDIO.name,
    val direction: String = CallDirection.OUTGOING.name,
    val durationSeconds: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis()
)
