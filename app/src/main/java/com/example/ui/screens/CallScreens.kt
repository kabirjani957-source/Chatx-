package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.CallType
import com.example.ui.components.ChatXAvatar
import com.example.ui.components.formatDurationMinSec
import com.example.ui.i18n.LocalChatXStrings
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed
import com.example.ui.viewmodel.ActiveCallState

@Composable
fun CallOverlayScreen(
    callState: ActiveCallState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit
) {
    val strings = LocalChatXStrings.current
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(callState.callType) {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (callState.callType == CallType.VIDEO &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    val pulseTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF064E3B), Color(0xFF0B141A))
                )
            )
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFA7F3D0), modifier = Modifier.size(14.dp))
                        Text(
                            text = "  Local Call UI • Live WebRTC signaling requires Firebase",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = if (callState.callType == CallType.VIDEO) strings.videoCall else strings.voiceCall,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFA7F3D0)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = callState.peerName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        callState.isRingingIncoming -> strings.incomingCall
                        callState.isConnected -> formatDurationMinSec(callState.durationSeconds)
                        else -> "Calling..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .scale(if (!callState.isConnected) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(Color(0xFF00A884).copy(alpha = 0.25f))
                )
                ChatXAvatar(
                    name = callState.peerName,
                    photoUri = null,
                    colorHex = 0xFF008069,
                    size = 120.dp
                )
            }

            if (callState.isRingingIncoming) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallCircleButton(
                        icon = Icons.Default.CallEnd,
                        label = strings.decline,
                        backgroundColor = CallDeclineRed,
                        testTag = "decline_call_button",
                        onClick = onDecline
                    )
                    CallCircleButton(
                        icon = if (callState.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                        label = strings.accept,
                        backgroundColor = CallAcceptGreen,
                        testTag = "accept_call_button",
                        onClick = onAccept
                    )
                }
            } else {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallCircleButton(
                            icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (callState.isMuted) "Unmute" else "Mute",
                            backgroundColor = if (callState.isMuted) Color.White else Color.White.copy(alpha = 0.2f),
                            iconTint = if (callState.isMuted) Color.Black else Color.White,
                            testTag = "call_mute_button",
                            onClick = onToggleMute
                        )
                        CallCircleButton(
                            icon = if (callState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Video",
                            backgroundColor = if (callState.isVideoEnabled) Color.White else Color.White.copy(alpha = 0.2f),
                            iconTint = if (callState.isVideoEnabled) Color.Black else Color.White,
                            testTag = "call_video_button",
                            onClick = onToggleVideo
                        )
                        CallCircleButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Speaker",
                            backgroundColor = if (callState.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f),
                            iconTint = if (callState.isSpeakerOn) Color.Black else Color.White,
                            testTag = "call_speaker_button",
                            onClick = onToggleSpeaker
                        )
                        CallCircleButton(
                            icon = Icons.Default.CallEnd,
                            label = strings.endCall,
                            backgroundColor = CallDeclineRed,
                            testTag = "end_call_button",
                            onClick = onEndCall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallCircleButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
