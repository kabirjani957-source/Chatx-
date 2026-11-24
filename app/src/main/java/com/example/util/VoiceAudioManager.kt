package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceAudioManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentRecordFile: File? = null
    private var recordStartMs: Long = 0L

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun startRecording(): Result<File> {
        return try {
            stopRecording(discard = true)
            val voiceDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outFile = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentRecordFile = outFile
            recordStartMs = System.currentTimeMillis()
            Result.success(outFile)
        } catch (e: Exception) {
            Log.e("VoiceAudioManager", "Failed to start recording", e)
            Result.failure(e)
        }
    }

    fun stopRecording(discard: Boolean = false): Pair<String?, Int> {
        val file = currentRecordFile
        val durationSec = ((System.currentTimeMillis() - recordStartMs) / 1000L).toInt().coerceAtLeast(1)
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("VoiceAudioManager", "Stop recording warning: ${e.message}")
            try { recorder?.release() } catch (_: Exception) {}
        } finally {
            recorder = null
            currentRecordFile = null
        }

        if (discard && file != null && file.exists()) {
            file.delete()
            return null to 0
        }
        return if (file != null && file.exists() && file.length() > 0L) {
            file.absolutePath to durationSec
        } else {
            null to 0
        }
    }

    fun togglePlayback(messageId: String, filePath: String?, scope: CoroutineScope) {
        if (_playingMessageId.value == messageId) {
            stopPlayback()
            return
        }
        stopPlayback()
        if (filePath.isNullOrBlank() || !File(filePath).exists()) {
            // Simulated playback progress for seeded sample voice messages without local audio files
            _playingMessageId.value = messageId
            _playbackProgress.value = 0f
            progressJob = scope.launch {
                val steps = 30
                for (i in 1..steps) {
                    delay(120)
                    _playbackProgress.value = i.toFloat() / steps
                }
                stopPlayback()
            }
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            player = mp
            _playingMessageId.value = messageId
            _playbackProgress.value = 0f
            mp.setOnCompletionListener {
                stopPlayback()
            }
            progressJob = scope.launch {
                while (_playingMessageId.value == messageId && player != null) {
                    val dur = player?.duration ?: 1
                    val pos = player?.currentPosition ?: 0
                    _playbackProgress.value = (pos.toFloat() / dur.coerceAtLeast(1)).coerceIn(0f, 1f)
                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAudioManager", "Playback failed", e)
            stopPlayback()
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        } finally {
            player = null
            _playingMessageId.value = null
            _playbackProgress.value = 0f
        }
    }
}
