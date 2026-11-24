package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatXDao {

    // Users & Contacts
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserProfileEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 0 ORDER BY username ASC")
    fun observeContacts(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserProfileEntity>)

    @Query("UPDATE users SET isBlocked = :blocked WHERE uid = :uid")
    suspend fun setUserBlocked(uid: String, blocked: Boolean)

    // Chats
    @Query("SELECT * FROM chats ORDER BY lastMessageTimeMillis DESC")
    fun observeAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChat(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChats(chats: List<ChatEntity>)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun markChatRead(chatId: String)

    @Query("UPDATE chats SET isTyping = :typing WHERE chatId = :chatId")
    suspend fun setChatTyping(chatId: String, typing: Boolean)

    @Query("UPDATE chats SET disappearingSeconds = :seconds WHERE chatId = :chatId")
    suspend fun setChatDisappearingTimer(chatId: String, seconds: Int)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)

    // Messages
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAtMillis ASC")
    fun observeMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE chatId = :chatId AND isFromMe = 1")
    suspend fun markOutgoingMessagesStatus(chatId: String, status: String)

    @Query("UPDATE messages SET isDeleted = 1, content = :deletedText, mediaUri = NULL, mediaName = NULL WHERE messageId = :messageId")
    suspend fun softDeleteMessage(messageId: String, deletedText: String = "This message was deleted")

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("DELETE FROM messages WHERE expiresAtMillis IS NOT NULL AND expiresAtMillis <= :nowMillis")
    suspend fun purgeExpiredMessages(nowMillis: Long)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessageCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE mediaUri IS NOT NULL")
    suspend fun getMediaMessageCount(): Int

    @Query("DELETE FROM messages WHERE mediaUri IS NOT NULL")
    suspend fun clearMediaMessages()

    // Status Updates (24h expiry)
    @Query("SELECT * FROM statuses WHERE expiresAtMillis > :nowMillis ORDER BY isMine DESC, createdAtMillis DESC")
    fun observeActiveStatuses(nowMillis: Long): Flow<List<StatusUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: StatusUpdateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatuses(statuses: List<StatusUpdateEntity>)

    @Query("UPDATE statuses SET isViewed = 1, viewCount = viewCount + 1 WHERE statusId = :statusId AND isViewed = 0")
    suspend fun markStatusViewed(statusId: String)

    @Query("DELETE FROM statuses WHERE expiresAtMillis <= :nowMillis")
    suspend fun purgeExpiredStatuses(nowMillis: Long)

    @Query("DELETE FROM statuses WHERE statusId = :statusId")
    suspend fun deleteStatus(statusId: String)

    // Calls
    @Query("SELECT * FROM calls ORDER BY timestampMillis DESC")
    fun observeCallHistory(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallRecordEntity)

    @Query("DELETE FROM calls")
    suspend fun clearCallHistory()

    // Full Wipe (Logout / Account Deletion)
    @Query("DELETE FROM messages")
    suspend fun wipeMessages()

    @Query("DELETE FROM chats")
    suspend fun wipeChats()

    @Query("DELETE FROM statuses")
    suspend fun wipeStatuses()

    @Query("DELETE FROM calls")
    suspend fun wipeCalls()

    @Query("DELETE FROM users")
    suspend fun wipeUsers()
}
