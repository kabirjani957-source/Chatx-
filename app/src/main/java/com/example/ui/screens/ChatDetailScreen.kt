package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.AppSettings
import com.example.data.local.CallType
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.local.MessageType
import com.example.data.local.PrivacyVisibility
import com.example.ui.components.ChatWallpaperBackground
import com.example.ui.components.ChatXAvatar
import com.example.ui.components.DeliveryTicks
import com.example.ui.components.QuickEmojiPicker
import com.example.ui.components.formatDurationMinSec
import com.example.ui.components.formatTimeShort
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.theme.CallDeclineRed
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OutgoingBubbleDark
import com.example.ui.theme.OutgoingBubbleLight
import com.example.ui.theme.SecretBadgeColor
import com.example.util.VoiceAudioManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: ChatEntity,
    messages: List<MessageEntity>,
    allChats: List<ChatEntity>,
    settings: AppSettings,
    voiceAudioManager: VoiceAudioManager,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendText: (String, MessageEntity?) -> Unit,
    onSendMedia: (MessageType, Uri, String?) -> Unit,
    onSendVoice: (String, Int) -> Unit,
    onForwardMessage: (ChatEntity, MessageEntity) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onSetDisappearingTimer: (Int) -> Unit,
    onClearChat: () -> Unit,
    onStartCall: (CallType) -> Unit,
    onToast: (String) -> Unit
) {
    val strings = LocalChatXStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<MessageEntity?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var inChatQuery by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var messageToForward by remember { mutableStateOf<MessageEntity?>(null) }
    var previewMediaUri by remember { mutableStateOf<String?>(null) }

    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSec by remember { mutableIntStateOf(0) }

    val playingId by voiceAudioManager.playingMessageId.collectAsStateWithLifecycle()
    val playProgress by voiceAudioManager.playbackProgress.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSendMedia(MessageType.IMAGE, uri, "Photo")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSendMedia(MessageType.VIDEO, uri, "Video")
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val fileName = resolveFileName(context, uri) ?: "Document"
            onSendMedia(MessageType.DOCUMENT, uri, fileName)
        }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            voiceAudioManager.startRecording()
                .onSuccess { isRecordingVoice = true; recordingSec = 0 }
                .onFailure { onToast("Unable to start microphone: ${it.localizedMessage}") }
        } else {
            onToast("Microphone permission is needed to record voice messages.")
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            while (isRecordingVoice) {
                delay(1000)
                recordingSec++
            }
        }
    }

    val visibleMessages = remember(messages, inChatQuery) {
        if (inChatQuery.isBlank()) messages
        else messages.filter { it.content.contains(inChatQuery, ignoreCase = true) }
    }

    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showBackButton) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(Modifier.width(12.dp))
                        }
                        ChatXAvatar(
                            name = chat.title,
                            photoUri = chat.photoUri,
                            colorHex = chat.avatarColorHex,
                            size = 40.dp,
                            isGroup = chat.isGroup,
                            isSecret = chat.isSecret
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chat.isSecret) {
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Encrypted",
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            val statusText = when {
                                chat.isTyping -> strings.typing
                                chat.isGroup -> chat.participantNamesCsv
                                !settings.onlinePrivacyEveryone || settings.lastSeenPrivacy == PrivacyVisibility.NOBODY ->
                                    if (chat.isSecret) "End-to-End Encrypted" else chat.subtitle
                                else -> strings.online
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!chat.isGroup) {
                            IconButton(
                                onClick = { onStartCall(CallType.VIDEO) },
                                modifier = Modifier.testTag("chat_video_call_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = strings.videoCall, tint = Color.White)
                            }
                            IconButton(
                                onClick = { onStartCall(CallType.AUDIO) },
                                modifier = Modifier.testTag("chat_audio_call_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = strings.voiceCall, tint = Color.White)
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("chat_more_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Search in chat") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    onClick = { showSearchBar = !showSearchBar; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.disappearingMessages) },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                    onClick = { showDisappearingDialog = true; showMoreMenu = false }
                                )
                                if (chat.isSecret) {
                                    DropdownMenuItem(
                                        text = { Text("Encryption & Safety Number") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        onClick = { showSafetyDialog = true; showMoreMenu = false }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Clear messages") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = { onClearChat(); showMoreMenu = false }
                                )
                            }
                        }
                    }

                    if (showSearchBar) {
                        OutlinedTextField(
                            value = inChatQuery,
                            onValueChange = { inChatQuery = it },
                            placeholder = { Text("Search messages...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { inChatQuery = ""; showSearchBar = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                replyingTo?.let { replyMsg ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(34.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (replyMsg.isFromMe) "You" else replyMsg.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = replyMsg.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyingTo = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                            }
                        }
                    }
                }

                if (showEmojiPicker) {
                    QuickEmojiPicker(
                        onEmojiSelected = { emoji -> inputText += emoji }
                    )
                }

                if (isRecordingVoice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Recording",
                            tint = CallDeclineRed
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Recording voice note • ${formatDurationMinSec(recordingSec)}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                voiceAudioManager.stopRecording(discard = true)
                                isRecordingVoice = false
                            }
                        ) {
                            Text("Cancel")
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    val (path, dur) = voiceAudioManager.stopRecording(discard = false)
                                    isRecordingVoice = false
                                    if (path != null) onSendVoice(path, dur)
                                }
                                .testTag("stop_send_voice_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Stop, contentDescription = "Send voice note", tint = Color.White)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { showEmojiPicker = !showEmojiPicker },
                                    modifier = Modifier.testTag("emoji_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEmotions,
                                        contentDescription = "Emoji",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = { Text(strings.typeMessage) },
                                    maxLines = 4,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("message_composer_input"),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )
                                IconButton(
                                    onClick = { showAttachSheet = true },
                                    modifier = Modifier.testTag("attach_media_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Attach file",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (inputText.isNotBlank()) {
                                        onSendText(inputText, replyingTo)
                                        inputText = ""
                                        replyingTo = null
                                        showEmojiPicker = false
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            voiceAudioManager.startRecording()
                                                .onSuccess { isRecordingVoice = true; recordingSec = 0 }
                                                .onFailure { onToast("Mic error: ${it.localizedMessage}") }
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("send_or_voice_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (inputText.isNotBlank()) "Send" else "Record voice message",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ChatWallpaperBackground(wallpaper = settings.wallpaper)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chat.isSecret) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = SecretBadgeColor.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { showSafetyDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.secretChatNotice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (chat.disappearingSeconds > 0) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Disappearing messages enabled (${chat.disappearingSeconds / 3600}h)",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                items(visibleMessages, key = { it.messageId }) { message ->
                    MessageBubble(
                        message = message,
                        isGroup = chat.isGroup,
                        isPlayingVoice = playingId == message.messageId,
                        voiceProgress = if (playingId == message.messageId) playProgress else 0f,
                        onPlayVoice = {
                            voiceAudioManager.togglePlayback(message.messageId, message.mediaUri, scope)
                        },
                        onMediaClick = { uri ->
                            if (message.type == MessageType.DOCUMENT.name) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    onToast("Document attached: ${message.mediaName ?: "File"}")
                                }
                            } else {
                                previewMediaUri = uri
                            }
                        },
                        onLongPress = { selectedMessageForAction = message }
                    )
                }
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Share Media & Documents", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachOption(
                        icon = Icons.Default.Image,
                        label = "Photo",
                        color = Color(0xFF008069),
                        onClick = {
                            showAttachSheet = false
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    AttachOption(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        color = Color(0xFF0D9488),
                        onClick = {
                            showAttachSheet = false
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    )
                    AttachOption(
                        icon = Icons.Default.Description,
                        label = "Document",
                        color = Color(0xFF047857),
                        onClick = {
                            showAttachSheet = false
                            docPicker.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
    }

    selectedMessageForAction?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("Message Options") },
            text = {
                Column {
                    ListActionRow(Icons.AutoMirrored.Filled.Reply, strings.reply) {
                        replyingTo = msg
                        selectedMessageForAction = null
                    }
                    ListActionRow(Icons.AutoMirrored.Filled.Forward, strings.forward) {
                        messageToForward = msg
                        selectedMessageForAction = null
                    }
                    ListActionRow(Icons.Default.ContentCopy, strings.copy) {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("ChatX Message", msg.content))
                        onToast("Copied to clipboard")
                        selectedMessageForAction = null
                    }
                    if (!msg.isDeleted) {
                        ListActionRow(Icons.Default.Delete, strings.delete, tint = CallDeclineRed) {
                            onDeleteMessage(msg.messageId)
                            selectedMessageForAction = null
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForAction = null }) { Text(strings.cancel) }
            }
        )
    }

    messageToForward?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToForward = null },
            title = { Text("Forward to...") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(allChats, key = { it.chatId }) { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onForwardMessage(target, msg)
                                    messageToForward = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatXAvatar(
                                name = target.title,
                                photoUri = target.photoUri,
                                colorHex = target.avatarColorHex,
                                size = 38.dp,
                                isSecret = target.isSecret
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(target.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageToForward = null }) { Text(strings.cancel) }
            }
        )
    }

    if (showSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyDialog = false },
            title = { Text("End-to-End Encryption") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Messages in this Secret Chat are encrypted on-device with AES-256-GCM using an ECDH P-256 shared secret.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("60-Digit Safety Number:", style = MaterialTheme.typography.labelLarge)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = chat.safetyNumber.ifBlank { "Verified Local Key" },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafetyDialog = false }) { Text("Done") }
            }
        )
    }

    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text(strings.disappearingMessages) },
            text = {
                Column {
                    listOf(0 to "Off", 86400 to "24 hours", 604800 to "7 days", 7776000 to "90 days").forEach { (sec, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetDisappearingTimer(sec)
                                    showDisappearingDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            if (chat.disappearingSeconds == sec) {
                                Text("Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisappearingDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    previewMediaUri?.let { uri ->
        Dialog(
            onDismissRequest = { previewMediaUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { previewMediaUri = null }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Media preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { previewMediaUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    isPlayingVoice: Boolean,
    voiceProgress: Float,
    onPlayVoice: () -> Unit,
    onMediaClick: (String) -> Unit,
    onLongPress: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val bubbleColor = when {
        message.isFromMe && isDark -> OutgoingBubbleDark
        message.isFromMe -> OutgoingBubbleLight
        isDark -> IncomingBubbleDark
        else -> IncomingBubbleLight
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {
                        if (message.type == MessageType.VOICE.name) onPlayVoice()
                        else if (!message.mediaUri.isNullOrBlank()) onMediaClick(message.mediaUri)
                        else onLongPress()
                    },
                    onLongClick = onLongPress
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !message.isFromMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                }

                if (message.isForwarded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Forwarded",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }

                if (!message.replyToPreview.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.replyToPreview,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                when {
                    message.isDeleted -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    message.type == MessageType.IMAGE.name && !message.mediaUri.isNullOrBlank() -> {
                        AsyncImage(
                            model = message.mediaUri,
                            contentDescription = "Shared photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(message.content, style = MaterialTheme.typography.bodyMedium)
                    }

                    message.type == MessageType.VIDEO.name && !message.mediaUri.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF064E3B)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = message.mediaUri,
                                contentDescription = "Shared video",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(message.content, style = MaterialTheme.typography.bodyMedium)
                    }

                    message.type == MessageType.VOICE.name -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(220.dp)
                        ) {
                            IconButton(
                                onClick = onPlayVoice,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingVoice) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause voice note",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                LinearProgressIndicator(
                                    progress = { if (isPlayingVoice) voiceProgress else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatDurationMinSec(message.mediaDurationSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    message.type == MessageType.DOCUMENT.name -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "Document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message.mediaName ?: message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimeShort(message.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message.isFromMe) {
                        Spacer(Modifier.width(4.dp))
                        DeliveryTicks(statusName = message.deliveryStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(54.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ListActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

private fun resolveFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }
}
