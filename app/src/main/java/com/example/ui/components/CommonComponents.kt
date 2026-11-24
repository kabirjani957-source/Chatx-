package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ChatWallpaper
import com.example.data.local.DeliveryStatus
import com.example.ui.theme.ReadTickBlue
import com.example.ui.theme.SecretBadgeColor
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainerDark
import com.example.ui.theme.WarningContainerLight
import com.example.util.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatXAvatar(
    name: String,
    photoUri: String?,
    colorHex: Long,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    isSecret: Boolean = false,
    isOnline: Boolean = false
) {
    Box(modifier = Modifier.size(size)) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "$name avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initials = remember(name) {
                name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "CX" }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(colorHex)),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = name,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.36f).sp
                    )
                }
            }
        }

        if (isSecret) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.38f)
                    .clip(CircleShape)
                    .background(SecretBadgeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secret encrypted chat",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.22f)
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun DeliveryTicks(statusName: String, modifier: Modifier = Modifier) {
    val status = DeliveryStatus.entries.firstOrNull { it.name == statusName } ?: DeliveryStatus.SENT
    when (status) {
        DeliveryStatus.SENDING -> Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Sending",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(14.dp)
        )
        DeliveryStatus.SENT -> Icon(
            imageVector = Icons.Default.Done,
            contentDescription = "Sent",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(15.dp)
        )
        DeliveryStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Delivered",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(15.dp)
        )
        DeliveryStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Read",
            tint = ReadTickBlue,
            modifier = modifier.size(15.dp)
        )
    }
}

@Composable
fun ServiceNoticeBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isDark) WarningContainerDark else WarningContainerLight,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = WarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
            )
        }
    }
}

@Composable
fun QrCodeCanvas(
    content: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(content) { QrCodeGenerator.encode(content) }
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val n = matrix.size
            val cellSize = size.width / n.toFloat()
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color(0xFF064E3B),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatWallpaperBackground(
    wallpaper: ChatWallpaper,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Box(modifier = modifier.fillMaxSize()) {
        when (wallpaper) {
            ChatWallpaper.DEFAULT_PATTERN -> {
                if (isDark) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B141A))
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.chatx_wallpaper_default_1795500346687),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.85f
                    )
                }
            }
            ChatWallpaper.SOFT_MINT -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF0D1F1A) else Color(0xFFEAF7F0))
                )
            }
            ChatWallpaper.WARM_IVORY -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF1C1917) else Color(0xFFFAF6EE))
                )
            }
            ChatWallpaper.EMERALD_MIST -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF062C22), Color(0xFF0B141A))
                                else listOf(Color(0xFFD1FAE5), Color(0xFFF0FDF4))
                            )
                        )
                )
            }
            ChatWallpaper.MIDNIGHT_SLATE -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickEmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = remember {
        listOf(
            "😀", "😂", "🥰", "😍", "🤩", "😎", "🥳", "🙏",
            "👍", "🔥", "❤️", "✨", "🎉", "💯", "🤝", "👏",
            "😊", "🤔", "😢", "😮", "🚀", "☕", "🎵", "📸"
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

fun formatTimeShort(millis: Long): String {
    val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

fun formatDurationMinSec(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f))
    }
}
