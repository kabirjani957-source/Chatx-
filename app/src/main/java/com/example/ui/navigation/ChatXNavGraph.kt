package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.CallType
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CallOverlayScreen
import com.example.ui.screens.CallsTab
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsTab
import com.example.ui.screens.ContactsTab
import com.example.ui.screens.NewChatBottomSheet
import com.example.ui.screens.QrProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UpdatesStatusTab
import com.example.ui.viewmodel.ChatXViewModel

object ChatXRoutes {
    const val HOME = "home"
    const val CHAT = "chat/{chatId}"
    const val AI_ASSISTANT = "ai_assistant"
    const val QR_PROFILE = "qr_profile"
    const val SETTINGS = "settings"

    fun chatRoute(chatId: String): String = "chat/$chatId"
}

@Composable
fun ChatXAppRoot(
    viewModel: ChatXViewModel,
    onLoggedOutOrDeleted: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ChatXRoutes.HOME
        ) {
            composable(ChatXRoutes.HOME) {
                HomeAdaptiveShell(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onOpenChatFullScreen = { chatId ->
                        viewModel.markChatRead(chatId)
                        navController.navigate(ChatXRoutes.chatRoute(chatId))
                    },
                    onOpenAiAssistant = { navController.navigate(ChatXRoutes.AI_ASSISTANT) },
                    onOpenQrProfile = { navController.navigate(ChatXRoutes.QR_PROFILE) },
                    onOpenSettings = { navController.navigate(ChatXRoutes.SETTINGS) }
                )
            }

            composable(
                route = ChatXRoutes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()
                val chat by viewModel.observeChat(chatId).collectAsStateWithLifecycle(initialValue = null)
                val activeChat = chat ?: chats.firstOrNull { it.chatId == chatId }

                if (activeChat != null) {
                    val messages by viewModel.observeMessages(activeChat)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    LaunchedEffect(activeChat.chatId) {
                        viewModel.markChatRead(activeChat.chatId)
                    }
                    ChatDetailScreen(
                        chat = activeChat,
                        messages = messages,
                        allChats = chats,
                        settings = settings,
                        voiceAudioManager = viewModel.voiceAudioManager,
                        showBackButton = true,
                        onBack = { navController.popBackStack() },
                        onSendText = { txt, reply -> viewModel.sendTextMessage(activeChat, txt, reply) },
                        onSendMedia = { type, uri, name -> viewModel.sendMediaMessage(activeChat, type, uri, name) },
                        onSendVoice = { path, sec -> viewModel.sendVoiceMessage(activeChat, path, sec) },
                        onForwardMessage = { target, msg -> viewModel.forwardMessage(target, msg) },
                        onDeleteMessage = { msgId -> viewModel.deleteMessage(msgId) },
                        onSetDisappearingTimer = { sec -> viewModel.setDisappearingTimer(activeChat.chatId, sec) },
                        onClearChat = { viewModel.clearChat(activeChat.chatId) },
                        onStartCall = { callType ->
                            viewModel.startOutgoingCall(
                                peerId = activeChat.peerId ?: activeChat.chatId,
                                peerName = activeChat.title,
                                peerPhone = activeChat.subtitle,
                                callType = callType
                            )
                        },
                        onToast = { viewModel.showToast(it) }
                    )
                }
            }

            composable(ChatXRoutes.AI_ASSISTANT) {
                AiAssistantScreen(
                    messages = aiMessages,
                    isLoading = aiLoading,
                    onBack = { navController.popBackStack() },
                    onSendPrompt = { viewModel.sendAiPrompt(it) }
                )
            }

            composable(ChatXRoutes.QR_PROFILE) {
                QrProfileScreen(
                    user = currentUser,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ChatXRoutes.SETTINGS) {
                SettingsScreen(
                    user = currentUser,
                    settings = settings,
                    contacts = contacts,
                    storageStats = storageStats,
                    onBack = { navController.popBackStack() },
                    onOpenQrProfile = { navController.navigate(ChatXRoutes.QR_PROFILE) },
                    onUpdateProfile = { name, bio, photo -> viewModel.updateProfile(name, bio, photo) },
                    onSetDarkMode = { viewModel.setDarkMode(it) },
                    onSetThemePreset = { viewModel.setThemePreset(it) },
                    onSetWallpaper = { viewModel.setWallpaper(it) },
                    onSetLanguage = { viewModel.setLanguage(it) },
                    onSetNotifications = { viewModel.setNotificationsEnabled(it) },
                    onSetRingtone = { viewModel.setRingtoneName(it) },
                    onSetVibration = { viewModel.setVibrationEnabled(it) },
                    onSetLastSeenPrivacy = { viewModel.setLastSeenPrivacy(it) },
                    onSetOnlinePrivacy = { viewModel.setOnlinePrivacyEveryone(it) },
                    onToggleBlockContact = { viewModel.toggleBlockContact(it.uid, it.isBlocked) },
                    onSetBiometricLock = { viewModel.setBiometricLock(it) },
                    onSetTwoStepPin = { viewModel.setTwoStepPin(it) },
                    onSetDefaultDisappearing = { viewModel.setDefaultDisappearingSeconds(it) },
                    onRefreshStorage = { viewModel.refreshStorageStats() },
                    onClearMediaCache = { viewModel.clearMediaCache() },
                    onLogout = { viewModel.logout(onLoggedOutOrDeleted) },
                    onDeleteAccount = { viewModel.deleteAccount(onLoggedOutOrDeleted) },
                    onToast = { viewModel.showToast(it) }
                )
            }
        }

        activeCall?.let { call ->
            BackHandler { viewModel.endActiveCall(declined = call.isRingingIncoming) }
            CallOverlayScreen(
                callState = call,
                onAccept = { viewModel.acceptIncomingCall() },
                onDecline = { viewModel.endActiveCall(declined = true) },
                onEndCall = { viewModel.endActiveCall(declined = false) },
                onToggleMute = { viewModel.toggleCallMute() },
                onToggleSpeaker = { viewModel.toggleCallSpeaker() },
                onToggleVideo = { viewModel.toggleCallVideo() }
            )
        }
    }
}

@Composable
private fun HomeAdaptiveShell(
    viewModel: ChatXViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenChatFullScreen: (String) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenQrProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalChatXStrings.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val calls by viewModel.calls.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var selectedTabletChatId by rememberSaveable { mutableStateOf<String?>(null) }

    val tabItems = listOf(
        Triple(strings.tabChats, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
        Triple(strings.tabUpdates, Icons.Filled.Update, Icons.Outlined.Update),
        Triple(strings.tabCalls, Icons.Filled.Call, Icons.Outlined.Call),
        Triple(strings.tabContacts, Icons.Filled.Contacts, Icons.Outlined.Contacts)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { searchExpanded = !searchExpanded },
                                modifier = Modifier.testTag("home_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                            }
                            IconButton(
                                onClick = onOpenAiAssistant,
                                modifier = Modifier.testTag("home_ai_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = strings.aiAssistant, tint = Color.White)
                            }
                            IconButton(
                                onClick = onOpenQrProfile,
                                modifier = Modifier.testTag("home_qr_button")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = strings.qrProfile, tint = Color.White)
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("home_settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = Color.White)
                            }
                        }

                        if (searchExpanded) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(strings.searchHint) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { searchQuery = ""; searchExpanded = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                                    .testTag("home_search_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar {
                        tabItems.forEachIndexed { idx, (label, filled, outlined) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == idx) filled else outlined,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("bottom_tab_$idx")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 3) {
                    FloatingActionButton(
                        onClick = { showNewChatSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("new_chat_fab")
                    ) {
                        Icon(Icons.Default.AddComment, contentDescription = strings.newChat)
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail {
                        Spacer(Modifier.weight(0.1f))
                        tabItems.forEachIndexed { idx, (label, filled, outlined) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(if (selectedTab == idx) filled else outlined, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    VerticalDivider(thickness = 0.5.dp)
                }

                val listPaneModifier = if (isExpanded && selectedTab == 0) {
                    Modifier.width(340.dp).fillMaxHeight()
                } else {
                    Modifier.weight(1f).fillMaxHeight()
                }

                Box(modifier = listPaneModifier) {
                    when (selectedTab) {
                        0 -> ChatsTab(
                            chats = chats,
                            searchQuery = searchQuery,
                            selectedChatId = if (isExpanded) selectedTabletChatId else null,
                            onChatClick = { chat ->
                                if (isExpanded) {
                                    selectedTabletChatId = chat.chatId
                                    viewModel.markChatRead(chat.chatId)
                                } else {
                                    onOpenChatFullScreen(chat.chatId)
                                }
                            },
                            onOpenAiAssistant = onOpenAiAssistant
                        )
                        1 -> UpdatesStatusTab(
                            currentUser = currentUser,
                            statuses = statuses,
                            onPostStatus = { type, caption, uri, bg, privacy ->
                                viewModel.postStatus(type, caption, uri, bg, privacy)
                            },
                            onMarkStatusViewed = { viewModel.markStatusViewed(it) },
                            onDeleteStatus = { viewModel.deleteStatus(it) }
                        )
                        2 -> CallsTab(
                            calls = calls,
                            contacts = contacts,
                            onStartCall = { id, name, phone, type ->
                                viewModel.startOutgoingCall(id, name, phone, type)
                            },
                            onSimulateIncomingCall = { peer, type ->
                                viewModel.triggerIncomingCall(peer, type)
                            },
                            onClearHistory = { viewModel.clearCallHistory() }
                        )
                        3 -> ContactsTab(
                            contacts = contacts,
                            searchQuery = searchQuery,
                            onAddContact = { name, phone -> viewModel.addContact(name, phone) },
                            onStartChat = { peer, isSecret ->
                                viewModel.startDirectChat(peer, isSecret) { chatId ->
                                    if (isExpanded) {
                                        selectedTab = 0
                                        selectedTabletChatId = chatId
                                    } else {
                                        onOpenChatFullScreen(chatId)
                                    }
                                }
                            },
                            onStartCall = { peer, type ->
                                viewModel.startOutgoingCall(peer.uid, peer.username, peer.phoneNumber, type)
                            },
                            onToggleBlock = { viewModel.toggleBlockContact(it.uid, it.isBlocked) }
                        )
                    }
                }

                // Tablet List-Detail Right Pane for Chats
                if (isExpanded && selectedTab == 0) {
                    VerticalDivider(thickness = 0.5.dp)
                    val detailChat = chats.firstOrNull { it.chatId == selectedTabletChatId } ?: chats.firstOrNull()
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (detailChat != null) {
                            val messages by viewModel.observeMessages(detailChat)
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            ChatDetailScreen(
                                chat = detailChat,
                                messages = messages,
                                allChats = chats,
                                settings = settings,
                                voiceAudioManager = viewModel.voiceAudioManager,
                                showBackButton = false,
                                onBack = {},
                                onSendText = { txt, reply -> viewModel.sendTextMessage(detailChat, txt, reply) },
                                onSendMedia = { type, uri, name -> viewModel.sendMediaMessage(detailChat, type, uri, name) },
                                onSendVoice = { path, sec -> viewModel.sendVoiceMessage(detailChat, path, sec) },
                                onForwardMessage = { target, msg -> viewModel.forwardMessage(target, msg) },
                                onDeleteMessage = { msgId -> viewModel.deleteMessage(msgId) },
                                onSetDisappearingTimer = { sec -> viewModel.setDisappearingTimer(detailChat.chatId, sec) },
                                onClearChat = { viewModel.clearChat(detailChat.chatId) },
                                onStartCall = { callType ->
                                    viewModel.startOutgoingCall(
                                        peerId = detailChat.peerId ?: detailChat.chatId,
                                        peerName = detailChat.title,
                                        peerPhone = detailChat.subtitle,
                                        callType = callType
                                    )
                                },
                                onToast = { viewModel.showToast(it) }
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a chat to start messaging", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (showNewChatSheet) {
            NewChatBottomSheet(
                contacts = contacts,
                onDismiss = { showNewChatSheet = false },
                onStartDirectChat = { peer, isSecret ->
                    viewModel.startDirectChat(peer, isSecret) { chatId ->
                        if (isExpanded) {
                            selectedTab = 0
                            selectedTabletChatId = chatId
                        } else {
                            onOpenChatFullScreen(chatId)
                        }
                    }
                },
                onCreateGroup = { title, members ->
                    viewModel.createGroupChat(title, members) { chatId ->
                        if (isExpanded) {
                            selectedTab = 0
                            selectedTabletChatId = chatId
                        } else {
                            onOpenChatFullScreen(chatId)
                        }
                    }
                }
            )
        }
    }
}
