package com.audio.mp3cutter.ringtone.maker.ui.recorder

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import java.util.Locale

// Premium Recorder Colors
private val RecorderPink = Color(0xFFEC4899)
private val RecorderRed = Color(0xFFF43F5E)
private val RecorderOrange = Color(0xFFF97316)
private val RecorderGradient = listOf(RecorderPink, RecorderRed)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderScreen(
        onNavigateBack: () -> Unit,
        onEditRecording: (AudioModel) -> Unit,
        onSaveRecording: (AudioModel) -> Unit,
        viewModel: VoiceRecorderViewModel = hiltViewModel()
) {
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        var hasPermission by remember {
                mutableStateOf(
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                )
        }

        val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        isGranted ->
                        hasPermission = isGranted
                        if (!isGranted) {
                                Toast.makeText(
                                                context,
                                                "Microphone permission required",
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                        }
                }

        LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                }
        }

        Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                // Subtle ambient background
                AmbientBackground(isRecording = uiState.recordingState == RecordingState.Recording)

                Column(
                        modifier =
                                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                ) {
                        // Minimal Top Bar
                        MinimalTopBar(
                                onBack = {
                                        if (uiState.recordingState == RecordingState.Recording ||
                                                        uiState.recordingState ==
                                                                RecordingState.Paused
                                        ) {
                                                viewModel.stopRecording()
                                        }
                                        if (uiState.recordingState == RecordingState.Stopped) {
                                                viewModel.discardRecording()
                                        }
                                        onNavigateBack()
                                }
                        )

                        // Banner Ad
                        com.audio.mp3cutter.ringtone.maker.ui.ads.BannerAd(
                            adUnitId = com.audio.mp3cutter.ringtone.maker.BuildConfig.ADMOB_BANNER_ID,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // Main Content
                        Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Spacer(modifier = Modifier.weight(0.8f))

                                // Timer Display
                                TimerSection(
                                        elapsedTimeMs = uiState.elapsedTimeMs,
                                        recordingState = uiState.recordingState,
                                        isPlaying = uiState.isPlaying
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Waveform
                                WaveformSection(
                                        amplitudes = uiState.amplitudes,
                                        fullWaveform = uiState.fullWaveform,
                                        playbackProgress = uiState.playbackProgress,
                                        isRecording =
                                                uiState.recordingState == RecordingState.Recording,
                                        isPlaying = uiState.isPlaying,
                                        isStopped = uiState.recordingState == RecordingState.Stopped
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Controls Section
                                ControlsSection(
                                        recordingState = uiState.recordingState,
                                        isPlaying = uiState.isPlaying,
                                        hasPermission = hasPermission,
                                        onRequestPermission = {
                                                permissionLauncher.launch(
                                                        Manifest.permission.RECORD_AUDIO
                                                )
                                        },
                                        onStart = { viewModel.startRecording() },
                                        onPause = { viewModel.pauseRecording() },
                                        onResume = { viewModel.resumeRecording() },
                                        onStop = { viewModel.stopRecording() },
                                        onTogglePlay = { viewModel.togglePreview() },
                                        onEdit = {
                                                viewModel.stopPreview()
                                                viewModel.getRecordedAudio()?.let {
                                                        onEditRecording(it)
                                                }
                                        },
                                        onSave = {
                                                viewModel.stopPreview()
                                                viewModel.getRecordedAudio()?.let { audio ->
                                                        onSaveRecording(audio)
                                                }
                                        },
                                        onDiscard = { viewModel.discardRecording() }
                                )

                                Spacer(modifier = Modifier.height(32.dp))
                        }
                }
        }
}

@Composable
private fun AmbientBackground(isRecording: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "ambient")
        val pulseScale by
                infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(2000, easing = EaseInOutCubic),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "pulse"
                )

        Box(modifier = Modifier.fillMaxSize()) {
                // Top right glow
                Box(
                        modifier =
                                Modifier.size(200.dp)
                                        .offset(x = 150.dp, y = (-50).dp)
                                        .blur(80.dp)
                                        .background(PrimaryPurple.copy(alpha = 0.06f), CircleShape)
                )

                // Recording glow (center)
                if (isRecording) {
                        Box(
                                modifier =
                                        Modifier.size(300.dp)
                                                .scale(pulseScale)
                                                .align(Alignment.Center)
                                                .offset(y = (-80).dp)
                                                .blur(100.dp)
                                                .background(
                                                        brush =
                                                                Brush.radialGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        RecorderPink
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.15f
                                                                                                ),
                                                                                        Color.Transparent
                                                                                )
                                                                )
                                                )
                        )
                }

                // Bottom left glow
                Box(
                        modifier =
                                Modifier.size(150.dp)
                                        .align(Alignment.BottomStart)
                                        .offset(x = (-40).dp, y = 40.dp)
                                        .blur(60.dp)
                                        .background(SecondaryCyan.copy(alpha = 0.04f), CircleShape)
                )
        }
}

@Composable
private fun MinimalTopBar(onBack: () -> Unit) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                IconButton(onClick = onBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White.copy(alpha = 0.8f)
                        )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                        text = "Voice Recorder",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                )
        }
}

@Composable
private fun TimerSection(elapsedTimeMs: Long, recordingState: RecordingState, isPlaying: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "timer")
        val dotAlpha by
                infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(600),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "dot"
                )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Status indicator
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                ) {
                        when {
                                recordingState == RecordingState.Recording -> {
                                        Box(
                                                modifier =
                                                        Modifier.size(8.dp)
                                                                .background(
                                                                        RecorderRed.copy(
                                                                                alpha = dotAlpha
                                                                        ),
                                                                        CircleShape
                                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "RECORDING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RecorderPink,
                                                letterSpacing = 2.sp
                                        )
                                }
                                recordingState == RecordingState.Paused -> {
                                        Box(
                                                modifier =
                                                        Modifier.size(8.dp)
                                                                .background(
                                                                        RecorderOrange,
                                                                        CircleShape
                                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "PAUSED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RecorderOrange,
                                                letterSpacing = 2.sp
                                        )
                                }
                                isPlaying -> {
                                        Box(
                                                modifier =
                                                        Modifier.size(8.dp)
                                                                .background(
                                                                        SuccessGreen.copy(
                                                                                alpha = dotAlpha
                                                                        ),
                                                                        CircleShape
                                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "PLAYING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SuccessGreen,
                                                letterSpacing = 2.sp
                                        )
                                }
                                recordingState == RecordingState.Stopped -> {
                                        Text(
                                                text = "READY TO SAVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                letterSpacing = 2.sp
                                        )
                                }
                                else -> {
                                        Text(
                                                text = "READY",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                letterSpacing = 2.sp
                                        )
                                }
                        }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timer
                Text(
                        text = formatTime(elapsedTimeMs),
                        style =
                                MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 56.sp,
                                        fontWeight = FontWeight.Thin,
                                        letterSpacing = 4.sp
                                ),
                        color = Color.White
                )
        }
}

@Composable
private fun WaveformSection(
        amplitudes: List<Int>,
        fullWaveform: List<Int>,
        playbackProgress: Float,
        isRecording: Boolean,
        isPlaying: Boolean,
        isStopped: Boolean
) {
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val waveOffset by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(600, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                ),
                        label = "waveOffset"
                )

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
        ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                        when {
                                // During recording - show live waveform
                                isRecording -> {
                                        val displayAmps =
                                                if (amplitudes.isNotEmpty()) amplitudes
                                                else List(50) { 10 }
                                        drawLiveWaveform(displayAmps, waveOffset)
                                }
                                // Stopped or playing - show full waveform with progress
                                isStopped || isPlaying -> {
                                        val waveform =
                                                if (fullWaveform.isNotEmpty()) {
                                                        // Resample to fit screen (max 60 bars)
                                                        resampleWaveform(fullWaveform, 60)
                                                } else {
                                                        List(60) { 20 }
                                                }
                                        drawPlaybackWaveform(waveform, playbackProgress, isPlaying)
                                }
                                // Idle - show placeholder
                                else -> {
                                        drawIdleWaveform()
                                }
                        }
                }
        }
}

// Resample waveform to target number of bars (use MAX to preserve peaks)
private fun resampleWaveform(source: List<Int>, targetCount: Int): List<Int> {
        if (source.isEmpty()) return List(targetCount) { 10 }
        if (source.size <= targetCount) return source

        val result = mutableListOf<Int>()
        val step = source.size.toFloat() / targetCount

        for (i in 0 until targetCount) {
                val startIdx = (i * step).toInt()
                val endIdx = ((i + 1) * step).toInt().coerceAtMost(source.size)
                // Use MAX instead of average to preserve peaks
                val maxVal = source.subList(startIdx, endIdx).maxOrNull() ?: 0
                result.add(maxVal)
        }
        return result
}

private fun DrawScope.drawLiveWaveform(amplitudes: List<Int>, waveOffset: Float) {
        val barCount = amplitudes.size
        val gapFraction = 0.25f
        val barWidth = (size.width / barCount) * (1f - gapFraction)
        val spacing = (size.width / barCount) * gapFraction
        val maxBarHeight = size.height * 0.95f
        val centerY = size.height / 2

        amplitudes.forEachIndexed { index, amplitude ->
                // Lower floor for more dynamic range
                val baseHeight = (amplitude / 100f).coerceIn(0.05f, 1f)
                val pulse = 0.05f * kotlin.math.sin((waveOffset * 2 * Math.PI).toFloat()) + 1f
                val animatedHeight = (baseHeight * pulse).coerceIn(0.05f, 1f)

                val barHeight = (maxBarHeight * animatedHeight).coerceAtLeast(4f)
                val x = index * (barWidth + spacing)
                val y = centerY - barHeight / 2

                drawRoundRect(
                        color = lerp(RecorderPink, RecorderRed, animatedHeight),
                        topLeft = Offset(x, y),
                        size = Size(barWidth.coerceAtLeast(3f), barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
        }
}

private fun DrawScope.drawPlaybackWaveform(
        waveform: List<Int>,
        progress: Float,
        isPlaying: Boolean
) {
        val barCount = waveform.size
        val gapFraction = 0.2f
        val barWidth = (size.width / barCount) * (1f - gapFraction)
        val spacing = (size.width / barCount) * gapFraction
        val maxBarHeight = size.height * 0.95f
        val centerY = size.height / 2

        val progressIndex = (progress * barCount).toInt()

        waveform.forEachIndexed { index, amplitude ->
                // Much lower floor (0.05 = 5%) for visible peaks
                val baseHeight = (amplitude / 100f).coerceIn(0.05f, 1f)
                val barHeight = (maxBarHeight * baseHeight).coerceAtLeast(4f)
                val x = index * (barWidth + spacing)
                val y = centerY - barHeight / 2

                // Bars before progress are colored, after are dimmed
                val color =
                        when {
                                isPlaying && index <= progressIndex ->
                                        lerp(RecorderPink, RecorderRed, baseHeight)
                                isPlaying -> Color.White.copy(alpha = 0.15f)
                                else ->
                                        lerp(
                                                RecorderPink.copy(alpha = 0.6f),
                                                RecorderRed.copy(alpha = 0.6f),
                                                baseHeight
                                        )
                        }

                drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth.coerceAtLeast(3f), barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
        }

        // Draw progress line when playing
        if (isPlaying && progress > 0f) {
                val lineX = size.width * progress
                drawLine(
                        color = Color.White,
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height),
                        strokeWidth = 2f
                )
        }
}

private fun DrawScope.drawIdleWaveform() {
        val barCount = 50
        val gapFraction = 0.25f
        val barWidth = (size.width / barCount) * (1f - gapFraction)
        val spacing = (size.width / barCount) * gapFraction
        val centerY = size.height / 2

        for (i in 0 until barCount) {
                val barHeight = 8f + (i % 5) * 4f
                val x = i * (barWidth + spacing)
                val y = centerY - barHeight / 2

                drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth.coerceAtLeast(2f), barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
        }
}

@Composable
private fun ControlsSection(
        recordingState: RecordingState,
        isPlaying: Boolean,
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        onStart: () -> Unit,
        onPause: () -> Unit,
        onResume: () -> Unit,
        onStop: () -> Unit,
        onTogglePlay: () -> Unit,
        onEdit: () -> Unit,
        onSave: () -> Unit,
        onDiscard: () -> Unit
) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
                when (recordingState) {
                        RecordingState.Idle -> {
                                // Single record button
                                MainRecordButton(
                                        isRecording = false,
                                        onClick = {
                                                if (hasPermission) onStart()
                                                else onRequestPermission()
                                        }
                                )
                                Text(
                                        text = "Tap to record",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                )
                        }
                        RecordingState.Recording -> {
                                // Pause and Stop buttons
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Stop button (left)
                                        SmallControlButton(
                                                icon = Icons.Default.Stop,
                                                onClick = onStop,
                                                tint = RecorderRed,
                                                backgroundColor = RecorderRed.copy(alpha = 0.15f)
                                        )

                                        // Main Pause button (center)
                                        MainRecordButton(isRecording = true, onClick = onPause)

                                        // Spacer for symmetry
                                        Spacer(modifier = Modifier.size(44.dp))
                                }
                        }
                        RecordingState.Paused -> {
                                // Resume and Stop buttons
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        SmallControlButton(
                                                icon = Icons.Default.Stop,
                                                onClick = onStop,
                                                tint = RecorderRed,
                                                backgroundColor = RecorderRed.copy(alpha = 0.15f)
                                        )

                                        PausedResumeButton(onClick = onResume)

                                        Spacer(modifier = Modifier.size(44.dp))
                                }
                        }
                        RecordingState.Stopped -> {
                                // Playback and action buttons
                                PostRecordingControls(
                                        isPlaying = isPlaying,
                                        onTogglePlay = onTogglePlay,
                                        onEdit = onEdit,
                                        onSave = onSave,
                                        onDiscard = onDiscard
                                )
                        }
                }
        }
}

@Composable
private fun MainRecordButton(isRecording: Boolean, onClick: () -> Unit) {
        val infiniteTransition = rememberInfiniteTransition(label = "record")
        val scale by
                infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isRecording) 1.05f else 1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(800, easing = EaseInOutCubic),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "scale"
                )

        Box(contentAlignment = Alignment.Center) {
                // Outer ring (only when recording)
                if (isRecording) {
                        Box(
                                modifier =
                                        Modifier.size(88.dp)
                                                .scale(scale)
                                                .border(
                                                        2.dp,
                                                        RecorderPink.copy(alpha = 0.4f),
                                                        CircleShape
                                                )
                        )
                }

                // Main button
                Box(
                        modifier =
                                Modifier.size(72.dp)
                                        .scale(if (isRecording) scale else 1f)
                                        .clip(CircleShape)
                                        .background(
                                                brush =
                                                        Brush.linearGradient(
                                                                if (isRecording)
                                                                        listOf(
                                                                                RecorderRed,
                                                                                RecorderPink
                                                                        )
                                                                else RecorderGradient
                                                        )
                                        )
                                        .clickable { onClick() },
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector =
                                        if (isRecording) Icons.Default.Pause else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                        )
                }
        }
}

@Composable
private fun PausedResumeButton(onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.size(72.dp)
                                .clip(CircleShape)
                                .background(
                                        brush =
                                                Brush.linearGradient(
                                                        listOf(RecorderOrange, Color(0xFFEAB308))
                                                )
                                )
                                .clickable { onClick() },
                contentAlignment = Alignment.Center
        ) {
                Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                )
        }
}

@Composable
private fun SmallControlButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        tint: Color,
        backgroundColor: Color
) {
        Box(
                modifier =
                        Modifier.size(44.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .clickable { onClick() },
                contentAlignment = Alignment.Center
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                )
        }
}

@Composable
private fun PostRecordingControls(
        isPlaying: Boolean,
        onTogglePlay: () -> Unit,
        onEdit: () -> Unit,
        onSave: () -> Unit,
        onDiscard: () -> Unit
) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Play button
                Box(
                        modifier =
                                Modifier.size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                                if (isPlaying) RecorderPink.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                                1.dp,
                                                if (isPlaying) RecorderPink.copy(alpha = 0.5f)
                                                else Color.White.copy(alpha = 0.1f),
                                                CircleShape
                                        )
                                        .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector =
                                        if (isPlaying) Icons.Default.Stop
                                        else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isPlaying) RecorderPink else Color.White,
                                modifier = Modifier.size(24.dp)
                        )
                }

                Text(
                        text = if (isPlaying) "Playing" else "Preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Edit button
                        CompactActionButton(
                                text = "Edit",
                                icon = Icons.Default.Edit,
                                gradient = listOf(PrimaryPurple, Color(0xFF6366F1)),
                                onClick = onEdit
                        )

                        // Save button
                        CompactActionButton(
                                text = "Save",
                                icon = Icons.Default.Save,
                                gradient = listOf(SuccessGreen, Color(0xFF059669)),
                                onClick = onSave
                        )
                }

                // Discard text button
                TextButton(onClick = onDiscard) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                                text = "Discard",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary.copy(alpha = 0.7f)
                        )
                }
        }
}

@Composable
private fun CompactActionButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        gradient: List<Color>,
        onClick: () -> Unit
) {
        Box(
                modifier =
                        Modifier.height(44.dp)
                                .widthIn(min = 100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brush = Brush.linearGradient(gradient))
                                .clickable { onClick() }
                                .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                        )
                }
        }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
        return Color(
                red = start.red + (end.red - start.red) * fraction,
                green = start.green + (end.green - start.green) * fraction,
                blue = start.blue + (end.blue - start.blue) * fraction,
                alpha = start.alpha + (end.alpha - start.alpha) * fraction
        )
}

private fun formatTime(elapsedMs: Long): String {
        val totalSeconds = (elapsedMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val deciseconds = ((elapsedMs % 1000) / 100).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d.%01d", minutes, seconds, deciseconds)
}
