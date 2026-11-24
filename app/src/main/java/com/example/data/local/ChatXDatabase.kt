package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.SecretChatCrypto

@Database(
    entities = [
        UserProfileEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        StatusUpdateEntity::class,
        CallRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChatXDatabase : RoomDatabase() {
    abstract fun chatXDao(): ChatXDao

    companion object {
        @Volatile
        private var INSTANCE: ChatXDatabase? = null

        fun getInstance(context: Context): ChatXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatXDatabase::class.java,
                    "chatx_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object SeedDataHelper {

    suspend fun seedInitialLocalWorkspace(dao: ChatXDao, myUid: String, myPrivateKey: String) {
        val now = System.currentTimeMillis()
        val peer1Keys = SecretChatCrypto.generateEcKeyPair()
        val peer2Keys = SecretChatCrypto.generateEcKeyPair()
        val peer3Keys = SecretChatCrypto.generateEcKeyPair()
        val peer4Keys = SecretChatCrypto.generateEcKeyPair()

        val contacts = listOf(
            UserProfileEntity(
                uid = "contact_aarav",
                phoneNumber = "+91 98765 43210",
                username = "Aarav Sharma",
                bio = "Building Android apps in Kotlin",
                publicKeyBase64 = peer1Keys.publicKeyBase64,
                privateKeyBase64 = peer1Keys.privateKeyBase64,
                isOnline = true,
                lastSeenMillis = now - 60_000L,
                avatarColorHex = 0xFF008069
            ),
            UserProfileEntity(
                uid = "contact_priya",
                phoneNumber = "+91 98234 11223",
                username = "Priya Patel",
                bio = "Design & Typography enthusiast",
                publicKeyBase64 = peer2Keys.publicKeyBase64,
                privateKeyBase64 = peer2Keys.privateKeyBase64,
                isOnline = false,
                lastSeenMillis = now - 1_200_000L,
                avatarColorHex = 0xFF0D9488
            ),
            UserProfileEntity(
                uid = "contact_rohan",
                phoneNumber = "+91 99110 44556",
                username = "Rohan Das",
                bio = "Cuttack -> Bengaluru | Coffee & Code",
                publicKeyBase64 = peer3Keys.publicKeyBase64,
                privateKeyBase64 = peer3Keys.privateKeyBase64,
                isOnline = true,
                lastSeenMillis = now,
                avatarColorHex = 0xFF15803D
            ),
            UserProfileEntity(
                uid = "contact_meera",
                phoneNumber = "+91 97400 77889",
                username = "Meera Nair",
                bio = "Available",
                publicKeyBase64 = peer4Keys.publicKeyBase64,
                privateKeyBase64 = peer4Keys.privateKeyBase64,
                isOnline = false,
                lastSeenMillis = now - 3_600_000L,
                avatarColorHex = 0xFF047857
            )
        )
        dao.upsertUsers(contacts)

        val secretKeyHex = if (myPrivateKey.isNotBlank()) {
            SecretChatCrypto.deriveSharedKeyHex(myPrivateKey, peer2Keys.publicKeyBase64)
        } else {
            SecretChatCrypto.generateFallbackSessionKeyHex()
        }
        val safetyNum = SecretChatCrypto.computeSafetyNumber(secretKeyHex, "contact_priya")

        val chats = listOf(
            ChatEntity(
                chatId = "chat_aarav",
                title = "Aarav Sharma",
                subtitle = "+91 98765 43210",
                isGroup = false,
                isSecret = false,
                peerId = "contact_aarav",
                participantIdsCsv = "$myUid,contact_aarav",
                participantNamesCsv = "You,Aarav Sharma",
                avatarColorHex = 0xFF008069,
                lastMessagePreview = "Welcome to ChatX! Try sending a voice note or photo.",
                lastMessageTimeMillis = now - 120_000L,
                unreadCount = 1
            ),
            ChatEntity(
                chatId = "chat_group_design",
                title = "ChatX Product Team",
                subtitle = "You, Aarav, Priya, Rohan",
                isGroup = true,
                isSecret = false,
                participantIdsCsv = "$myUid,contact_aarav,contact_priya,contact_rohan",
                participantNamesCsv = "You,Aarav,Priya,Rohan",
                avatarColorHex = 0xFF059669,
                lastMessagePreview = "Rohan: Multilingual support in Hindi & Odia is live!",
                lastMessageTimeMillis = now - 900_000L,
                unreadCount = 2
            ),
            ChatEntity(
                chatId = "chat_secret_priya",
                title = "Priya Patel",
                subtitle = "End-to-End Encrypted",
                isGroup = false,
                isSecret = true,
                peerId = "contact_priya",
                participantIdsCsv = "$myUid,contact_priya",
                participantNamesCsv = "You,Priya Patel",
                avatarColorHex = 0xFF0F766E,
                lastMessagePreview = "Encrypted message",
                lastMessageTimeMillis = now - 2_400_000L,
                unreadCount = 0,
                disappearingSeconds = 86400,
                sessionKeyHex = secretKeyHex,
                safetyNumber = safetyNum
            )
        )
        dao.upsertChats(chats)

        val secretCipher1 = SecretChatCrypto.encrypt(
            "This Secret Chat uses real AES-256-GCM encryption with ECDH key exchange.",
            secretKeyHex
        )
        val secretCipher2 = SecretChatCrypto.encrypt(
            "Check our 60-digit Safety Number anytime from the top bar menu!",
            secretKeyHex
        )

        val messages = listOf(
            MessageEntity(
                messageId = "msg_1",
                chatId = "chat_aarav",
                senderId = "contact_aarav",
                senderName = "Aarav Sharma",
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = "Hey! Welcome to ChatX — Connect. Chat. Enjoy.",
                deliveryStatus = DeliveryStatus.READ.name,
                createdAtMillis = now - 300_000L
            ),
            MessageEntity(
                messageId = "msg_2",
                chatId = "chat_aarav",
                senderId = "contact_aarav",
                senderName = "Aarav Sharma",
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = "Welcome to ChatX! Try sending a voice note or photo.",
                deliveryStatus = DeliveryStatus.READ.name,
                createdAtMillis = now - 120_000L
            ),
            MessageEntity(
                messageId = "msg_3",
                chatId = "chat_group_design",
                senderId = "contact_priya",
                senderName = "Priya Patel",
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = "The new emerald & crisp white UI looks super clean!",
                deliveryStatus = DeliveryStatus.READ.name,
                createdAtMillis = now - 1_500_000L
            ),
            MessageEntity(
                messageId = "msg_4",
                chatId = "chat_group_design",
                senderId = "contact_rohan",
                senderName = "Rohan Das",
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = "Multilingual support in Hindi & Odia is live!",
                deliveryStatus = DeliveryStatus.READ.name,
                createdAtMillis = now - 900_000L
            ),
            MessageEntity(
                messageId = "msg_sec_1",
                chatId = "chat_secret_priya",
                senderId = "contact_priya",
                senderName = "Priya Patel",
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = secretCipher1,
                deliveryStatus = DeliveryStatus.READ.name,
                expiresAtMillis = now + 86_400_000L,
                createdAtMillis = now - 2_600_000L
            ),
            MessageEntity(
                messageId = "msg_sec_2",
                chatId = "chat_secret_priya",
                senderId = "contact_priya",
                senderName = "Priya Patel",
                isFromMe = false,
                type = MessageType.TEXT.name,
                content = secretCipher2,
                deliveryStatus = DeliveryStatus.READ.name,
                expiresAtMillis = now + 86_400_000L,
                createdAtMillis = now - 2_400_000L
            )
        )
        dao.upsertMessages(messages)

        val statuses = listOf(
            StatusUpdateEntity(
                statusId = "st_aarav_1",
                userId = "contact_aarav",
                userName = "Aarav Sharma",
                phoneNumber = "+91 98765 43210",
                isMine = false,
                type = StatusType.TEXT.name,
                caption = "Shipping ChatX v1.0 today! Connect. Chat. Enjoy.",
                backgroundColorHex = 0xFF008069,
                privacy = StatusPrivacy.ALL_CONTACTS.name,
                viewCount = 12,
                isViewed = false,
                createdAtMillis = now - 1_800_000L,
                expiresAtMillis = now - 1_800_000L + 86_400_000L
            ),
            StatusUpdateEntity(
                statusId = "st_rohan_1",
                userId = "contact_rohan",
                userName = "Rohan Das",
                phoneNumber = "+91 99110 44556",
                isMine = false,
                type = StatusType.TEXT.name,
                caption = "ଶୁଭ ସନ୍ଧ୍ୟା! Testing ChatX in Odia & Hindi.",
                backgroundColorHex = 0xFF0F766E,
                privacy = StatusPrivacy.ALL_CONTACTS.name,
                viewCount = 8,
                isViewed = false,
                createdAtMillis = now - 3_600_000L,
                expiresAtMillis = now - 3_600_000L + 86_400_000L
            )
        )
        dao.upsertStatuses(statuses)

        dao.insertCall(
            CallRecordEntity(
                callId = "call_seed_1",
                peerId = "contact_aarav",
                peerName = "Aarav Sharma",
                peerPhone = "+91 98765 43210",
                callType = CallType.VIDEO.name,
                direction = CallDirection.INCOMING.name,
                durationSeconds = 185,
                timestampMillis = now - 7_200_000L
            )
        )
        dao.insertCall(
            CallRecordEntity(
                callId = "call_seed_2",
                peerId = "contact_rohan",
                peerName = "Rohan Das",
                peerPhone = "+91 99110 44556",
                callType = CallType.AUDIO.name,
                direction = CallDirection.MISSED.name,
                durationSeconds = 0,
                timestampMillis = now - 18_000_000L
            )
        )
    }
}
