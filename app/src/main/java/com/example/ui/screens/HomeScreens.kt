package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.CallDirection
import com.example.data.local.CallRecordEntity
import com.example.data.local.CallType
import com.example.data.local.ChatEntity
import com.example.data.local.StatusPrivacy
import com.example.data.local.StatusType
import com.example.data.local.StatusUpdateEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.components.ChatXAvatar
import com.example.ui.components.DeliveryTicks
import com.example.ui.components.formatDurationMinSec
import com.example.ui.components.formatTimeShort
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed
import com.example.ui.theme.SecretBadgeColor
import com.example.ui.theme.UnreadBadgeGreen
import kotlinx.coroutines.delay

enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    GROUPS("Groups"),
    SECRET("Secret")
}

@Composable
fun ChatsTab(
    chats: List<ChatEntity>,
    searchQuery: String,
    selectedChatId: String?,
    onChatClick: (ChatEntity) -> Unit,
    onOpenAiAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalChatXStrings.current
    var activeFilter by remember { mutableStateOf(ChatFilter.ALL) }

    val filtered = remember(chats, searchQuery, activeFilter) {
        chats.filter { chat ->
            val matchesQuery = searchQuery.isBlank() ||
                chat.title.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessagePreview.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (activeFilter) {
                ChatFilter.ALL -> true
                ChatFilter.UNREAD -> chat.unreadCount > 0
                ChatFilter.GROUPS -> chat.isGroup
                ChatFilter.SECRET -> chat.isSecret
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatFilter.entries) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("chat_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Pinned AI Assistant Row
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenAiAssistant)
                    .testTag("ai_assistant_card"),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.chatx_ai_avatar_1795500346776),
                        contentDescription = strings.aiAssistant,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.aiAssistant,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Draft replies, translate Hindi/Odia, or summarize",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No conversations match your filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.chatId }) { chat ->
                val isSelected = selectedChatId == chat.chatId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else Color.Transparent
                        )
                        .clickable { onChatClick(chat) }
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                        .testTag("chat_row_${chat.chatId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatXAvatar(
                        name = chat.title,
                        photoUri = chat.photoUri,
                        colorHex = chat.avatarColorHex,
                        size = 52.dp,
                        isGroup = chat.isGroup,
                        isSecret = chat.isSecret,
                        isOnline = !chat.isGroup && !chat.isSecret
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = chat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chat.isSecret) {
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Encrypted",
                                        tint = SecretBadgeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (chat.disappearingSeconds > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Disappearing timer active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatTimeShort(chat.lastMessageTimeMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (chat.isTyping) {
                                    Text(
                                        text = strings.typing,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    DeliveryTicks(statusName = "READ")
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = chat.lastMessagePreview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (chat.unreadCount > 0) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(UnreadBadgeGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesStatusTab(
    currentUser: UserProfileEntity?,
    statuses: List<StatusUpdateEntity>,
    onPostStatus: (StatusType, String, String?, Long, StatusPrivacy) -> Unit,
    onMarkStatusViewed: (String) -> Unit,
    onDeleteStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalChatXStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var viewingStatus by remember { mutableStateOf<StatusUpdateEntity?>(null) }

    val myStatuses = remember(statuses) { statuses.filter { it.isMine } }
    val recentPeerStatuses = remember(statuses) { statuses.filter { !it.isMine && !it.isViewed } }
    val viewedPeerStatuses = remember(statuses) { statuses.filter { !it.isMine && it.isViewed } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // My Status Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (myStatuses.isNotEmpty()) viewingStatus = myStatuses.first()
                        else showCreateDialog = true
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("my_status_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    ChatXAvatar(
                        name = currentUser?.username ?: "Me",
                        photoUri = currentUser?.photoUri,
                        colorHex = currentUser?.avatarColorHex ?: 0xFF00A884,
                        size = 54.dp
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { showCreateDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add status",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.myStatus, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (myStatuses.isEmpty()) strings.tapToAddStatus
                        else "${myStatuses.size} active • expires in 24h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("create_status_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Create Status")
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }

        if (recentPeerStatuses.isNotEmpty()) {
            item {
                Text(
                    text = strings.recentUpdates,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(recentPeerStatuses, key = { it.statusId }) { st ->
                StatusRow(status = st, isUnviewed = true) {
                    onMarkStatusViewed(st.statusId)
                    viewingStatus = st
                }
            }
        }

        if (viewedPeerStatuses.isNotEmpty()) {
            item {
                Text(
                    text = strings.viewedUpdates,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(viewedPeerStatuses, key = { it.statusId }) { st ->
                StatusRow(status = st, isUnviewed = false) {
                    viewingStatus = st
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStatusDialog(
            onDismiss = { showCreateDialog = false },
            onPost = { type, caption, uri, bg, privacy ->
                onPostStatus(type, caption, uri, bg, privacy)
                showCreateDialog = false
            }
        )
    }

    viewingStatus?.let { st ->
        StatusViewerDialog(
            status = st,
            onDismiss = { viewingStatus = null },
            onDelete = if (st.isMine) {
                {
                    onDeleteStatus(st.statusId)
                    viewingStatus = null
                }
            } else null
        )
    }
}

@Composable
private fun StatusRow(
    status: StatusUpdateEntity,
    isUnviewed: Boolean,
    onClick: () -> Unit
) {
    val hoursLeft = ((status.expiresAtMillis - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(
                    width = 2.5.dp,
                    color = if (isUnviewed) UnreadBadgeGreen else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            ChatXAvatar(
                name = status.userName,
                photoUri = status.mediaUri,
                colorHex = status.backgroundColorHex,
                size = 50.dp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(status.userName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${formatTimeShort(status.createdAtMillis)} • ${hoursLeft}h left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onPost: (StatusType, String, String?, Long, StatusPrivacy) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf(StatusType.TEXT) }
    var bgHex by remember { mutableLongStateOf(0xFF008069L) }
    var privacy by remember { mutableStateOf(StatusPrivacy.ALL_CONTACTS) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUri = uri
            mediaType = StatusType.IMAGE
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Status Update") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Write a status or caption") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_caption_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (mediaUri != null) "Media Selected" else "Photo / Video")
                    }
                }

                Text("Background Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0xFF008069L, 0xFF0F766EL, 0xFF15803DL, 0xFF1E293BL, 0xFF7C3AEDL).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (bgHex == hex) 3.dp else 1.dp,
                                    color = if (bgHex == hex) MaterialTheme.colorScheme.onSurface else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { bgHex = hex }
                        )
                    }
                }

                Text("Status Privacy", style = MaterialTheme.typography.labelMedium)
                StatusPrivacy.entries.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { privacy = p }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = privacy == p, onCheckedChange = { privacy = p })
                        Text(p.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caption.isNotBlank() || mediaUri != null) {
                        onPost(
                            mediaType,
                            caption.ifBlank { "Shared media update" },
                            mediaUri?.toString(),
                            bgHex,
                            privacy
                        )
                    }
                },
                enabled = caption.isNotBlank() || mediaUri != null,
                modifier = Modifier.testTag("post_status_confirm_button")
            ) {
                Text("Post (24h)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StatusViewerDialog(
    status: StatusUpdateEntity,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var progress by remember { mutableIntStateOf(0) }
    LaunchedEffect(status.statusId) {
        for (i in 1..100) {
            delay(50)
            progress = i
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(status.backgroundColorHex))
                .systemBarsPadding()
                .clickable(onClick = onDismiss)
        ) {
            if (!status.mediaUri.isNullOrBlank()) {
                AsyncImage(
                    model = status.mediaUri,
                    contentDescription = "Status media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatXAvatar(
                        name = status.userName,
                        photoUri = null,
                        colorHex = 0xFF064E3B,
                        size = 40.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(status.userName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${formatTimeShort(status.createdAtMillis)} • ${StatusPrivacy.entries.firstOrNull { it.name == status.privacy }?.label ?: "Contacts"}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete status", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
            ) {
                Text(
                    text = status.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${status.viewCount} views", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CallsTab(
    calls: List<CallRecordEntity>,
    contacts: List<UserProfileEntity>,
    onStartCall: (peerId: String, peerName: String, peerPhone: String, CallType) -> Unit,
    onSimulateIncomingCall: (UserProfileEntity, CallType) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalChatXStrings.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        if (contacts.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Test Incoming Call Screen", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Preview the full-screen incoming ring UI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onSimulateIncomingCall(contacts.first(), CallType.VIDEO) },
                            modifier = Modifier.testTag("simulate_incoming_call_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ring")
                        }
                    }
                }
            }
        }

        if (calls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.callsEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onClearHistory) { Text("Clear") }
                }
            }
            items(calls, key = { it.callId }) { call ->
                val isMissed = call.direction == CallDirection.MISSED.name
                val isVideo = call.callType == CallType.VIDEO.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onStartCall(
                                call.peerId,
                                call.peerName,
                                call.peerPhone,
                                if (isVideo) CallType.VIDEO else CallType.AUDIO
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatXAvatar(
                        name = call.peerName,
                        photoUri = null,
                        colorHex = 0xFF008069,
                        size = 48.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = call.peerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isMissed) CallDeclineRed else MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (call.direction) {
                                    CallDirection.INCOMING.name -> Icons.AutoMirrored.Filled.CallReceived
                                    CallDirection.MISSED.name -> Icons.AutoMirrored.Filled.CallMissed
                                    else -> Icons.AutoMirrored.Filled.CallMade
                                },
                                contentDescription = call.direction,
                                tint = if (isMissed) CallDeclineRed else CallAcceptGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${formatTimeShort(call.timestampMillis)} • ${if (call.durationSeconds > 0) formatDurationMinSec(call.durationSeconds) else "Missed"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            onStartCall(
                                call.peerId,
                                call.peerName,
                                call.peerPhone,
                                if (isVideo) CallType.VIDEO else CallType.AUDIO
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Call back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsTab(
    contacts: List<UserProfileEntity>,
    searchQuery: String,
    onAddContact: (String, String) -> Unit,
    onStartChat: (UserProfileEntity, Boolean) -> Unit,
    onStartCall: (UserProfileEntity, CallType) -> Unit,
    onToggleBlock: (UserProfileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalChatXStrings.current
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(contacts, searchQuery) {
        contacts.filter {
            searchQuery.isBlank() ||
                it.username.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("add_contact_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(strings.addContact, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.contactsEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filtered, key = { it.uid }) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartChat(contact, false) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatXAvatar(
                        name = contact.username,
                        photoUri = contact.photoUri,
                        colorHex = contact.avatarColorHex,
                        size = 48.dp,
                        isOnline = contact.isOnline && !contact.isBlocked
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(contact.username, style = MaterialTheme.typography.titleMedium)
                            if (contact.isBlocked) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Blocked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CallDeclineRed
                                )
                            }
                        }
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onStartChat(contact, true) }) {
                        Icon(Icons.Default.Lock, contentDescription = "Secret Chat", tint = SecretBadgeColor)
                    }
                    IconButton(onClick = { onStartCall(contact, CallType.AUDIO) }) {
                        Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onToggleBlock(contact) }) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "Block/Unblock",
                            tint = if (contact.isBlocked) CallDeclineRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addContact) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.testTag("new_contact_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 00000") },
                        singleLine = true,
                        modifier = Modifier.testTag("new_contact_phone_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddContact(name, phone)
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.testTag("save_new_contact_button")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatBottomSheet(
    contacts: List<UserProfileEntity>,
    onDismiss: () -> Unit,
    onStartDirectChat: (UserProfileEntity, Boolean) -> Unit,
    onCreateGroup: (String, List<UserProfileEntity>) -> Unit
) {
    val strings = LocalChatXStrings.current
    var showGroupCreator by remember { mutableStateOf(false) }
    var secretMode by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (!showGroupCreator) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (secretMode) strings.newSecretChat else strings.newChat,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !secretMode,
                        onClick = { secretMode = false },
                        label = { Text("Standard Chat") }
                    )
                    FilterChip(
                        selected = secretMode,
                        onClick = { secretMode = true },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Secret E2EE") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { showGroupCreator = true },
                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.newGroup) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(contacts, key = { it.uid }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStartDirectChat(contact, secretMode)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatXAvatar(
                                name = contact.username,
                                photoUri = contact.photoUri,
                                colorHex = contact.avatarColorHex,
                                size = 44.dp,
                                isSecret = secretMode
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(contact.username, style = MaterialTheme.typography.titleMedium)
                                Text(contact.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            var groupTitle by remember { mutableStateOf("") }
            val selectedIds = remember { mutableStateListOf<String>() }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(strings.newGroup, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_title_input")
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(contacts, key = { it.uid }) { contact ->
                        val checked = contact.uid in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(contact.uid)
                                    else selectedIds.add(contact.uid)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                if (it) selectedIds.add(contact.uid) else selectedIds.remove(contact.uid)
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(contact.username, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showGroupCreator = false }) { Text("Back") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val members = contacts.filter { it.uid in selectedIds }
                            onCreateGroup(groupTitle, members)
                            onDismiss()
                        },
                        enabled = groupTitle.isNotBlank() && selectedIds.isNotEmpty(),
                        modifier = Modifier.testTag("create_group_confirm_button")
                    ) {
                        Text("Create Group")
                    }
                }
            }
        }
    }
}
