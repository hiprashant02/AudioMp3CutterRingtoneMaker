package com.audio.mp3cutter.ringtone.maker.ui.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import com.audio.mp3cutter.ringtone.maker.utils.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onNavigateBack: () -> Unit, viewModel: EditorViewModel = hiltViewModel()) {
        val uiState by viewModel.uiState.collectAsState()

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Ambient background glow
                AmbientBackground(isPlaying = uiState.isPlaying)

                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        // Top Bar
                        EditorTopBar(
                                title = uiState.audio?.title ?: "Editor",
                                onNavigateBack = onNavigateBack
                        )

                        // Main Content
                        Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                        ) {
                                if (uiState.isLoading) {
                                        LoadingState()
                                } else if (uiState.waveformData.isNotEmpty()) {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .verticalScroll(
                                                                        rememberScrollState()
                                                                )
                                        ) {
                                                // Compact Time Controls with +/- buttons (MOVED
                                                // ABOVE waveform)
                                                CompactTimeControls(
                                                        selectionStartMs = uiState.selectionStartMs,
                                                        selectionEndMs = uiState.selectionEndMs,
                                                        selectionDurationMs =
                                                                uiState.selectionDurationMs,
                                                        onStartTimeAdjust = { deltaMs ->
                                                                val newStartMs =
                                                                        (uiState.selectionStartMs +
                                                                                        deltaMs)
                                                                                .coerceIn(
                                                                                        0,
                                                                                        uiState.selectionEndMs
                                                                                )
                                                                val newProgress =
                                                                        newStartMs.toFloat() /
                                                                                uiState.duration
                                                                                        .toFloat()
                                                                viewModel.updateSelectionStart(
                                                                        newProgress
                                                                )
                                                        },
                                                        onEndTimeAdjust = { deltaMs ->
                                                                val newEndMs =
                                                                        (uiState.selectionEndMs +
                                                                                        deltaMs)
                                                                                .coerceIn(
                                                                                        uiState.selectionStartMs,
                                                                                        uiState.duration
                                                                                )
                                                                val newProgress =
                                                                        newEndMs.toFloat() /
                                                                                uiState.duration
                                                                                        .toFloat()
                                                                viewModel.updateSelectionEnd(
                                                                        newProgress
                                                                )
                                                        }
                                                )

                                                WaveformSection(
                                                        waveformData = uiState.waveformData,
                                                        progress =
                                                                if (uiState.duration > 0) {
                                                                        uiState.currentPosition
                                                                                .toFloat() /
                                                                                uiState.duration
                                                                                        .toFloat()
                                                                } else 0f,
                                                        isPlaying = uiState.isPlaying,
                                                        selectionStart =
                                                                uiState.selectionStartProgress,
                                                        selectionEnd = uiState.selectionEndProgress,
                                                        duration = uiState.duration,
                                                        onSeek = { progress ->
                                                                viewModel.seekToProgress(progress)
                                                        },
                                                        onSeekAndPlay = { progress ->
                                                                viewModel.seekAndPlay(progress)
                                                        },
                                                        onSelectionStartChange = {
                                                                viewModel.updateSelectionStart(it)
                                                        },
                                                        onSelectionEndChange = {
                                                                viewModel.updateSelectionEnd(it)
                                                        }
                                                )

                                                // Audio Controls Row (Volume, Fade, Speed, Loop)
                                                AudioControlsRow(
                                                        isLoopMode = uiState.isLoopMode,
                                                        onVolumeClick = { /* TODO: Show volume dialog */
                                                        },
                                                        onFadeClick = { /* TODO: Show fade dialog */
                                                        },
                                                        onSpeedClick = { /* TODO: Show speed dialog */
                                                        },
                                                        onLoopToggle = {
                                                                if (uiState.isLoopMode)
                                                                        viewModel.stopLoopMode()
                                                                else viewModel.playSelectedLoop()
                                                        }
                                                )
                                        }
                                } else {
                                        EmptyWaveformState()
                                }
                        }

                        // Bottom Controls
                        BottomPlayerControls(
                                currentPosition = uiState.currentPosition,
                                duration = uiState.duration,
                                isPlaying = uiState.isPlaying,
                                onPlayPause = { viewModel.togglePlayback() },
                                onSeek = { progress -> viewModel.seekToProgress(progress) },
                                audioTitle = uiState.audio?.title ?: "",
                                audioArtist = uiState.audio?.artist ?: ""
                        )
                }
        }
}

@Composable
private fun AmbientBackground(isPlaying: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "ambient")

        val pulseAlpha by
                infiniteTransition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.25f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "pulse"
                )

        val offsetX by
                infiniteTransition.animateFloat(
                        initialValue = -100f,
                        targetValue = 100f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(8000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "offsetX"
                )

        Canvas(modifier = Modifier.fillMaxSize().blur(150.dp)) {
                // Primary purple glow
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        DeepPurple.copy(
                                                                alpha =
                                                                        if (isPlaying) pulseAlpha
                                                                        else 0.1f
                                                        ),
                                                        Color.Transparent
                                                ),
                                        center =
                                                Offset(
                                                        size.width * 0.3f + offsetX,
                                                        size.height * 0.3f
                                                ),
                                        radius = size.width * 0.6f
                                ),
                        center = Offset(size.width * 0.3f + offsetX, size.height * 0.3f),
                        radius = size.width * 0.6f
                )

                // Secondary cyan glow
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        SecondaryCyan.copy(
                                                                alpha =
                                                                        if (isPlaying)
                                                                                pulseAlpha * 0.7f
                                                                        else 0.05f
                                                        ),
                                                        Color.Transparent
                                                ),
                                        center =
                                                Offset(
                                                        size.width * 0.8f - offsetX,
                                                        size.height * 0.7f
                                                ),
                                        radius = size.width * 0.5f
                                ),
                        center = Offset(size.width * 0.8f - offsetX, size.height * 0.7f),
                        radius = size.width * 0.5f
                )
        }
}

@Composable
private fun EditorTopBar(title: String, onNavigateBack: () -> Unit) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Back button
                Box(
                        modifier =
                                Modifier.size(44.dp)
                                        .clip(CircleShape)
                                        .background(MatteSurface)
                                        .pointerInput(Unit) {
                                                detectTapGestures { onNavigateBack() }
                                        },
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                        )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Audio Editor",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                        )
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }

                // Options menu placeholder
                Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MatteSurface),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                        )
                }
        }
}

@Composable
private fun WaveformSection(
        waveformData: List<Int>,
        progress: Float,
        isPlaying: Boolean,
        selectionStart: Float,
        selectionEnd: Float,
        duration: Long,
        onSeek: (Float) -> Unit,
        onSeekAndPlay: (Float) -> Unit,
        onSelectionStartChange: (Float) -> Unit,
        onSelectionEndChange: (Float) -> Unit
) {
        var scale by remember { mutableFloatStateOf(1f) }
        val scrollState = rememberScrollState()

        // Auto-scroll state management
        var isAutoScrolling by remember { mutableStateOf(false) }
        var autoScrollDirection by remember { mutableIntStateOf(0) } // -1=left, 0=none, 1=right
        var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
        var autoScrollHandleType by remember { mutableIntStateOf(0) } // 0=none, 1=start, 2=end

        // Track last scroll value to calculate delta
        var lastScrollValue by remember { mutableFloatStateOf(0f) }

        // Auto-scroll coroutine - continuously scrolls while dragging near edges
        // Also adjusts handle position to keep it synchronized with scroll
        LaunchedEffect(isAutoScrolling, autoScrollDirection, autoScrollSpeed) {
                while (isAutoScrolling && autoScrollDirection != 0) {
                        val scrollBefore = scrollState.value.toFloat()
                        val scrollAmount = autoScrollSpeed * autoScrollDirection

                        // Use immediate scroll (not animated) for responsive, lag-free updates
                        scrollState.scrollBy(scrollAmount)

                        // Calculate actual scroll delta
                        val scrollDelta = scrollState.value.toFloat() - scrollBefore
                        lastScrollValue = scrollDelta

                        delay(16) // ~60fps
                }
        }

        val animatedProgress by
                animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(50),
                        label = "progress"
                )

        Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp), // Reduced from 16dp to 12dp
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Main waveform container
                BoxWithConstraints(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MatteSurface.copy(alpha = 0.3f))
                                        .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(16.dp)
                                        )
                ) {
                        val viewportWidth = constraints.maxWidth.toFloat()

                        // Horizontally scrollable waveform with integrated selection handles
                        Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                                WaveformWithSelection(
                                        waveformData = waveformData,
                                        progress = animatedProgress,
                                        scale = scale,
                                        selectionStart = selectionStart,
                                        selectionEnd = selectionEnd,
                                        duration = duration,
                                        onSeek = onSeek,
                                        onSeekAndPlay = onSeekAndPlay,
                                        onSelectionStartChange = onSelectionStartChange,
                                        onSelectionEndChange = onSelectionEndChange,
                                        scrollState = scrollState,
                                        viewportWidth = viewportWidth,
                                        onAutoScrollUpdate = {
                                                isScrolling,
                                                direction,
                                                speed,
                                                handleType ->
                                                isAutoScrolling = isScrolling
                                                autoScrollDirection = direction
                                                autoScrollSpeed = speed
                                                autoScrollHandleType = handleType
                                        },
                                        modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp)
                                )
                        }

                        Box(
                                modifier =
                                        Modifier.align(Alignment.CenterStart)
                                                .width(24.dp)
                                                .fillMaxHeight()
                                                .background(
                                                        Brush.horizontalGradient(
                                                                colors =
                                                                        listOf(
                                                                                MatteSurface,
                                                                                Color.Transparent
                                                                        )
                                                        )
                                                )
                        )

                        Box(
                                modifier =
                                        Modifier.align(Alignment.CenterEnd)
                                                .width(24.dp)
                                                .fillMaxHeight()
                                                .background(
                                                        Brush.horizontalGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color.Transparent,
                                                                                MatteSurface
                                                                        )
                                                        )
                                                )
                        )
                }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Zoom controls - OUTSIDE waveform, at bottom right
        Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Zoom out
                IconButton(
                        onClick = { scale = (scale / 1.5f).coerceAtLeast(1f) },
                        modifier = Modifier.size(28.dp)
                ) {
                        Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom out",
                                tint = if (scale > 1f) Color.White else TextSecondary,
                                modifier = Modifier.size(16.dp)
                        )
                }

                // Zoom indicator
                Text(
                        text = "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Zoom in
                IconButton(
                        onClick = { scale = (scale * 1.5f).coerceAtMost(8f) },
                        modifier = Modifier.size(28.dp)
                ) {
                        Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom in",
                                tint = if (scale < 8f) Color.White else TextSecondary,
                                modifier = Modifier.size(16.dp)
                        )
                }
        }
}

@Composable
private fun WaveformWithSelection(
        waveformData: List<Int>,
        progress: Float,
        scale: Float,
        selectionStart: Float,
        selectionEnd: Float,
        duration: Long,
        onSeek: (Float) -> Unit,
        onSeekAndPlay: (Float) -> Unit,
        onSelectionStartChange: (Float) -> Unit,
        onSelectionEndChange: (Float) -> Unit,
        scrollState: ScrollState,
        viewportWidth: Float,
        onAutoScrollUpdate:
                (isScrolling: Boolean, direction: Int, speed: Float, handleType: Int) -> Unit,
        modifier: Modifier = Modifier
) {
        val currentProgressState = rememberUpdatedState(progress)
        val density = LocalDensity.current
        val barWidthDp = 2.dp // Reduced from 3dp for thinner bars
        val barSpacingDp = 0.5.dp // Nearly zero spacing for continuous look
        val totalBarWidthDp = barWidthDp + barSpacingDp
        val handleWidth = 28.dp

        // Edge zone for auto-scroll trigger (80dp from edges)
        val edgeZonePx = with(density) { 80.dp.toPx() }
        val baseScrollSpeed = with(density) { 10.dp.toPx() } // Base speed for auto-scroll

        // Track scroll position to detect scroll changes
        val previousScrollValue = remember { mutableFloatStateOf(scrollState.value.toFloat()) }

        // Calculate total width based on data and scale
        // Drastically reduced sampling for shorter waveforms
        val durationSeconds = (duration / 1000f).toInt()

        // Use 5-8 samples per second instead of 28
        val targetSamplesPerSecond =
                when {
                        durationSeconds < 60 -> 8 // Very short: 8 samples/sec
                        durationSeconds < 300 -> 6 // < 5 min: 6 samples/sec
                        else -> 5 // Longer: 5 samples/sec
                }

        val idealSampleCount =
                (durationSeconds * targetSamplesPerSecond).coerceIn(100, 400) // Cap at 400

        val baseBarCount = idealSampleCount.coerceAtMost(waveformData.size)

        val displayedBars = (baseBarCount * scale).toInt().coerceIn(50, waveformData.size)
        val totalWidth = totalBarWidthDp * displayedBars

        val maxAmplitude =
                remember(waveformData) {
                        waveformData.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                }

        // Sample the waveform data
        val sampledData =
                remember(waveformData, displayedBars) {
                        if (waveformData.isEmpty()) emptyList()
                        else {
                                val step = waveformData.size.toFloat() / displayedBars
                                (0 until displayedBars).map { i ->
                                        val index =
                                                (i * step)
                                                        .toInt()
                                                        .coerceIn(0, waveformData.size - 1)
                                        waveformData[index]
                                }
                        }
                }

        // Local state for instant updates during dragging (bypasses ViewModel)
        var isDraggingStart by remember { mutableStateOf(false) }
        var isDraggingEnd by remember { mutableStateOf(false) }
        var localSelectionStart by remember { mutableFloatStateOf(selectionStart) }
        var localSelectionEnd by remember { mutableFloatStateOf(selectionEnd) }

        // Sync local state with props when not dragging
        LaunchedEffect(selectionStart) {
                if (!isDraggingStart) localSelectionStart = selectionStart
        }
        LaunchedEffect(selectionEnd) { if (!isDraggingEnd) localSelectionEnd = selectionEnd }

        // Use local state during drag, otherwise use props
        val activeSelectionStart = if (isDraggingStart) localSelectionStart else selectionStart
        val activeSelectionEnd = if (isDraggingEnd) localSelectionEnd else selectionEnd

        // Track current positions with rememberUpdatedState for drag callbacks
        val totalWidthPx = with(density) { totalWidth.toPx() }
        val currentStartPosition by rememberUpdatedState(activeSelectionStart)
        val currentEndPosition by rememberUpdatedState(activeSelectionEnd)

        // Calculate handle time values using active selection (updates during drag)
        val startTimeMs = (duration * activeSelectionStart).toLong()
        val endTimeMs = (duration * activeSelectionEnd).toLong()

        Column(modifier = modifier.width(totalWidth)) {
                // Waveform with handles
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Waveform canvas (background)
                        Canvas(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .pointerInput(Unit) {
                                                        detectTapGestures { offset ->
                                                                val tapProgress =
                                                                        offset.x / size.width
                                                                onSeek(tapProgress.coerceIn(0f, 1f))
                                                        }
                                                }
                                                .pointerInput(Unit) {
                                                        awaitEachGesture {
                                                                val down =
                                                                        awaitFirstDown(
                                                                                requireUnconsumed =
                                                                                        false
                                                                        )
                                                                val currentX =
                                                                        currentProgressState.value *
                                                                                size.width
                                                                val touchThreshold =
                                                                        with(density) {
                                                                                30.dp.toPx()
                                                                        } // Hit target size

                                                                // ONLY intercept if touching near
                                                                // the playhead
                                                                if (Math.abs(
                                                                                down.position.x -
                                                                                        currentX
                                                                        ) < touchThreshold
                                                                ) {
                                                                        var pointerId = down.id
                                                                        while (true) {
                                                                                val event =
                                                                                        awaitPointerEvent()
                                                                                val change =
                                                                                        event.changes
                                                                                                .find {
                                                                                                        it.id ==
                                                                                                                pointerId
                                                                                                }
                                                                                if (change ==
                                                                                                null ||
                                                                                                !change.pressed
                                                                                )
                                                                                        break

                                                                                if (change.position -
                                                                                                change.previousPosition !=
                                                                                                Offset.Zero
                                                                                ) {
                                                                                        change.consume() // Consume drag to prevent
                                                                                        // scrolling
                                                                                        val dragProgress =
                                                                                                change.position
                                                                                                        .x /
                                                                                                        size.width
                                                                                        onSeek(
                                                                                                dragProgress
                                                                                                        .coerceIn(
                                                                                                                0f,
                                                                                                                1f
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                        ) {
                                val barWidth = barWidthDp.toPx()
                                val barSpacing = barSpacingDp.toPx()
                                val totalBarWidth = barWidth + barSpacing
                                val centerY = size.height / 2

                                if (sampledData.isEmpty()) return@Canvas

                                // Clean selection glow - NO dark overlay on unselected
                                val selectionStartX = activeSelectionStart * size.width
                                val selectionEndX = activeSelectionEnd * size.width

                                // Subtle GLOW on selected region instead of dark unselected
                                drawRect(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        SecondaryCyan.copy(
                                                                                alpha = 0.08f
                                                                        ),
                                                                        SecondaryCyan.copy(
                                                                                alpha = 0.15f
                                                                        ),
                                                                        SecondaryCyan.copy(
                                                                                alpha = 0.08f
                                                                        )
                                                                )
                                                ),
                                        topLeft = Offset(selectionStartX, 0f),
                                        size = Size(selectionEndX - selectionStartX, size.height)
                                )

                                // Draw waveform bars
                                sampledData.forEachIndexed { index, amplitude ->
                                        val normalizedHeight =
                                                (amplitude / maxAmplitude).coerceIn(0.05f, 1f)
                                        val barHeight = size.height * 0.75f * normalizedHeight

                                        val x = index * totalBarWidth
                                        val barProgress = index.toFloat() / sampledData.size

                                        val isInSelection =
                                                barProgress >= activeSelectionStart &&
                                                        barProgress <= activeSelectionEnd
                                        val barColor =
                                                if (isInSelection) {
                                                        // Selected region: bright gradient (purple
                                                        // to cyan)
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                DeepPurple,
                                                                                SecondaryCyan
                                                                        ),
                                                                startY = centerY - barHeight / 2,
                                                                endY = centerY + barHeight / 2
                                                        )
                                                } else {
                                                        // Outside selection: dim white
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.15f
                                                                                ),
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.08f
                                                                                )
                                                                        ),
                                                                startY = centerY - barHeight / 2,
                                                                endY = centerY + barHeight / 2
                                                        )
                                                }

                                        drawRoundRect(
                                                brush = barColor,
                                                topLeft = Offset(x, centerY - barHeight / 2),
                                                size = Size(barWidth, barHeight),
                                                cornerRadius =
                                                        CornerRadius(barWidth / 2, barWidth / 2)
                                        )
                                }

                                // Current position indicator - white line with circular blobs
                                val currentPosX = progress * size.width

                                // Top blob - cyan to match bottom
                                drawCircle(
                                        color = SecondaryCyan,
                                        radius = 4.dp.toPx(),
                                        center = Offset(currentPosX, 0f)
                                )

                                // Vertical line
                                drawLine(
                                        color = Color.White,
                                        start = Offset(currentPosX, 0f),
                                        end = Offset(currentPosX, size.height),
                                        strokeWidth = 2.dp.toPx()
                                )

                                // Bottom blob
                                drawCircle(
                                        color = SecondaryCyan,
                                        radius = 4.dp.toPx(),
                                        center = Offset(currentPosX, size.height)
                                )

                                // Time label below the playback position
                                val currentTimeMs = (currentProgressState.value * duration).toLong()
                                val timeText = FileUtils.formatDurationWithDecimal(currentTimeMs)
                                val textPaint =
                                        android.graphics.Paint().apply {
                                                color = SecondaryCyan.toArgb()
                                                textSize = 11.sp.toPx()
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                typeface =
                                                        android.graphics.Typeface.create(
                                                                android.graphics.Typeface.DEFAULT,
                                                                android.graphics.Typeface.BOLD
                                                        )
                                        }
                                drawContext.canvas.nativeCanvas.drawText(
                                        timeText,
                                        currentPosX,
                                        size.height + 16.dp.toPx(),
                                        textPaint
                                )
                        }

                        // Start Handle - positioned on waveform (full height, no time text inside)
                        val startOffsetX = (totalWidth - handleWidth) * activeSelectionStart
                        Box(
                                modifier =
                                        Modifier.offset(x = startOffsetX)
                                                .width(handleWidth)
                                                .fillMaxHeight()
                                                .pointerInput(
                                                        totalWidthPx,
                                                        edgeZonePx,
                                                        baseScrollSpeed,
                                                        viewportWidth
                                                ) {
                                                        detectDragGestures(
                                                                onDragStart = {
                                                                        // Start dragging - use
                                                                        // local state
                                                                        isDraggingStart = true
                                                                        localSelectionStart =
                                                                                currentStartPosition
                                                                        previousScrollValue
                                                                                .floatValue =
                                                                                scrollState.value
                                                                                        .toFloat()
                                                                },
                                                                onDragEnd = {
                                                                        // Commit to ViewModel and
                                                                        // stop auto-scroll
                                                                        isDraggingStart = false
                                                                        onSelectionStartChange(
                                                                                localSelectionStart
                                                                        )
                                                                        onSeekAndPlay(
                                                                                localSelectionStart
                                                                        ) // Seek and play only on
                                                                        // drag end
                                                                        onAutoScrollUpdate(
                                                                                false,
                                                                                0,
                                                                                0f,
                                                                                0
                                                                        )
                                                                },
                                                                onDragCancel = {
                                                                        // Revert and stop
                                                                        isDraggingStart = false
                                                                        localSelectionStart =
                                                                                selectionStart
                                                                        onAutoScrollUpdate(
                                                                                false,
                                                                                0,
                                                                                0f,
                                                                                0
                                                                        )
                                                                }
                                                        ) { change, dragAmount ->
                                                                change.consume()

                                                                // Detect scroll change during drag
                                                                val currentScroll =
                                                                        scrollState.value.toFloat()
                                                                val scrollDelta =
                                                                        currentScroll -
                                                                                previousScrollValue
                                                                                        .floatValue
                                                                previousScrollValue.floatValue =
                                                                        currentScroll

                                                                // Update selection from drag +
                                                                // compensation for scroll
                                                                // When scrolling right (+), handle
                                                                // should also move
                                                                // right (+) to stay in place
                                                                val dragProgress =
                                                                        dragAmount.x / totalWidthPx
                                                                val scrollCompensation =
                                                                        scrollDelta / totalWidthPx
                                                                val combinedProgress =
                                                                        dragProgress +
                                                                                scrollCompensation

                                                                val newPos =
                                                                        (currentStartPosition +
                                                                                        combinedProgress)
                                                                                .coerceIn(
                                                                                        0f,
                                                                                        currentEndPosition
                                                                                )
                                                                localSelectionStart = newPos

                                                                // Calculate handle's screen
                                                                // position (relative to
                                                                // viewport)
                                                                val handlePosInWaveform =
                                                                        newPos * totalWidthPx
                                                                val handleScreenX =
                                                                        handlePosInWaveform -
                                                                                scrollState.value

                                                                // Check if handle is in edge zone
                                                                // and trigger
                                                                // auto-scroll
                                                                when {
                                                                        // Near left edge - scroll
                                                                        // left
                                                                        handleScreenX <
                                                                                edgeZonePx &&
                                                                                scrollState.value >
                                                                                        0 -> {
                                                                                val proximity =
                                                                                        1f -
                                                                                                (handleScreenX /
                                                                                                                edgeZonePx)
                                                                                                        .coerceIn(
                                                                                                                0f,
                                                                                                                1f
                                                                                                        )
                                                                                val speed =
                                                                                        baseScrollSpeed *
                                                                                                proximity
                                                                                                        .coerceIn(
                                                                                                                0.3f,
                                                                                                                1f
                                                                                                        )
                                                                                onAutoScrollUpdate(
                                                                                        true,
                                                                                        -1,
                                                                                        speed,
                                                                                        1
                                                                                ) // 1 = start
                                                                                // handle
                                                                        }
                                                                        // Near right edge - scroll
                                                                        // right
                                                                        handleScreenX >
                                                                                (viewportWidth -
                                                                                        edgeZonePx) &&
                                                                                scrollState.value <
                                                                                        scrollState
                                                                                                .maxValue -> {
                                                                                val proximity =
                                                                                        ((handleScreenX -
                                                                                                        (viewportWidth -
                                                                                                                edgeZonePx)) /
                                                                                                        edgeZonePx)
                                                                                                .coerceIn(
                                                                                                        0f,
                                                                                                        1f
                                                                                                )
                                                                                val speed =
                                                                                        baseScrollSpeed *
                                                                                                proximity
                                                                                                        .coerceIn(
                                                                                                                0.3f,
                                                                                                                1f
                                                                                                        )
                                                                                onAutoScrollUpdate(
                                                                                        true,
                                                                                        1,
                                                                                        speed,
                                                                                        1
                                                                                ) // 1 = start
                                                                                // handle
                                                                        }
                                                                        // In safe zone - no
                                                                        // auto-scroll
                                                                        else -> {
                                                                                onAutoScrollUpdate(
                                                                                        false,
                                                                                        0,
                                                                                        0f,
                                                                                        0
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                },
                                contentAlignment = Alignment.Center
                        ) {
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxHeight()
                                ) {
                                        // Grip at TOP for start handler - small draggable pill
                                        Box(
                                                modifier =
                                                        Modifier.size(16.dp, 40.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                        Brush.verticalGradient(
                                                                                colors =
                                                                                        listOf(
                                                                                                DeepPurple,
                                                                                                SecondaryCyan
                                                                                        )
                                                                        )
                                                                )
                                                                .border(
                                                                        width = 1.5.dp,
                                                                        color =
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.25f
                                                                                ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                // LEFT ARROW icon for start handler
                                                Icon(
                                                        imageVector =
                                                                Icons.Filled.KeyboardArrowLeft,
                                                        contentDescription = "Start",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                        }

                                        // Bottom section - thin fading line (full height)
                                        Box(
                                                modifier =
                                                        Modifier.width(2.dp)
                                                                .weight(1f)
                                                                .background(
                                                                        Brush.verticalGradient(
                                                                                colors =
                                                                                        listOf(
                                                                                                SecondaryCyan,
                                                                                                SecondaryCyan
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.4f
                                                                                                        ),
                                                                                                Color.Transparent
                                                                                        )
                                                                        )
                                                                )
                                        )
                                }
                        }

                        // End Handle - positioned on waveform (full height, no time text inside)
                        val endOffsetX = (totalWidth - handleWidth) * activeSelectionEnd
                        Box(
                                modifier =
                                        Modifier.offset(x = endOffsetX)
                                                .width(handleWidth)
                                                .fillMaxHeight()
                                                .pointerInput(
                                                        totalWidthPx,
                                                        edgeZonePx,
                                                        baseScrollSpeed,
                                                        viewportWidth
                                                ) {
                                                        detectDragGestures(
                                                                onDragStart = {
                                                                        // Start dragging - use
                                                                        // local state
                                                                        isDraggingEnd = true
                                                                        localSelectionEnd =
                                                                                currentEndPosition
                                                                        previousScrollValue
                                                                                .floatValue =
                                                                                scrollState.value
                                                                                        .toFloat()
                                                                },
                                                                onDragEnd = {
                                                                        // Commit to ViewModel and
                                                                        // stop auto-scroll
                                                                        isDraggingEnd = false
                                                                        onSelectionEndChange(
                                                                                localSelectionEnd
                                                                        )
                                                                        onAutoScrollUpdate(
                                                                                false,
                                                                                0,
                                                                                0f,
                                                                                0
                                                                        )
                                                                },
                                                                onDragCancel = {
                                                                        // Revert and stop
                                                                        isDraggingEnd = false
                                                                        localSelectionEnd =
                                                                                selectionEnd
                                                                        onAutoScrollUpdate(
                                                                                false,
                                                                                0,
                                                                                0f,
                                                                                0
                                                                        )
                                                                }
                                                        ) { change, dragAmount ->
                                                                change.consume()

                                                                // Detect scroll change during drag
                                                                val currentScroll =
                                                                        scrollState.value.toFloat()
                                                                val scrollDelta =
                                                                        currentScroll -
                                                                                previousScrollValue
                                                                                        .floatValue
                                                                previousScrollValue.floatValue =
                                                                        currentScroll

                                                                // Update selection from drag +
                                                                // compensation for scroll
                                                                // When scrolling right (+), handle
                                                                // should also move
                                                                // right (+) to stay in place
                                                                val dragProgress =
                                                                        dragAmount.x / totalWidthPx
                                                                val scrollCompensation =
                                                                        scrollDelta / totalWidthPx
                                                                val combinedProgress =
                                                                        dragProgress +
                                                                                scrollCompensation

                                                                val newPos =
                                                                        (currentEndPosition +
                                                                                        combinedProgress)
                                                                                .coerceIn(
                                                                                        currentStartPosition,
                                                                                        1f
                                                                                )
                                                                localSelectionEnd = newPos

                                                                // Calculate handle's screen
                                                                // position (relative to
                                                                // viewport)
                                                                val handlePosInWaveform =
                                                                        newPos * totalWidthPx
                                                                val handleScreenX =
                                                                        handlePosInWaveform -
                                                                                scrollState.value

                                                                // Check if handle is in edge zone
                                                                // and trigger
                                                                // auto-scroll
                                                                when {
                                                                        // Near left edge - scroll
                                                                        // left
                                                                        handleScreenX <
                                                                                edgeZonePx &&
                                                                                scrollState.value >
                                                                                        0 -> {
                                                                                val proximity =
                                                                                        1f -
                                                                                                (handleScreenX /
                                                                                                                edgeZonePx)
                                                                                                        .coerceIn(
                                                                                                                0f,
                                                                                                                1f
                                                                                                        )
                                                                                val speed =
                                                                                        baseScrollSpeed *
                                                                                                proximity
                                                                                                        .coerceIn(
                                                                                                                0.3f,
                                                                                                                1f
                                                                                                        )
                                                                                onAutoScrollUpdate(
                                                                                        true,
                                                                                        -1,
                                                                                        speed,
                                                                                        2
                                                                                ) // 2 = end handle
                                                                        }
                                                                        // Near right edge - scroll
                                                                        // right
                                                                        handleScreenX >
                                                                                (viewportWidth -
                                                                                        edgeZonePx) &&
                                                                                scrollState.value <
                                                                                        scrollState
                                                                                                .maxValue -> {
                                                                                val proximity =
                                                                                        ((handleScreenX -
                                                                                                        (viewportWidth -
                                                                                                                edgeZonePx)) /
                                                                                                        edgeZonePx)
                                                                                                .coerceIn(
                                                                                                        0f,
                                                                                                        1f
                                                                                                )
                                                                                val speed =
                                                                                        baseScrollSpeed *
                                                                                                proximity
                                                                                                        .coerceIn(
                                                                                                                0.3f,
                                                                                                                1f
                                                                                                        )
                                                                                onAutoScrollUpdate(
                                                                                        true,
                                                                                        1,
                                                                                        speed,
                                                                                        2
                                                                                ) // 2 = end handle
                                                                        }
                                                                        // In safe zone - no
                                                                        // auto-scroll
                                                                        else -> {
                                                                                onAutoScrollUpdate(
                                                                                        false,
                                                                                        0,
                                                                                        0f,
                                                                                        0
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                },
                                contentAlignment = Alignment.Center
                        ) {
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxHeight()
                                ) {
                                        // Top section - thin fading line (full height)
                                        Box(
                                                modifier =
                                                        Modifier.width(2.dp)
                                                                .weight(1f)
                                                                .background(
                                                                        Brush.verticalGradient(
                                                                                colors =
                                                                                        listOf(
                                                                                                Color.Transparent,
                                                                                                DeepPurple
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.4f
                                                                                                        ),
                                                                                                DeepPurple
                                                                                        )
                                                                        )
                                                                )
                                        )

                                        // Grip at BOTTOM for end handler - small draggable pill
                                        Box(
                                                modifier =
                                                        Modifier.size(16.dp, 40.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                        Brush.verticalGradient(
                                                                                colors =
                                                                                        listOf(
                                                                                                DeepPurple,
                                                                                                SecondaryCyan
                                                                                        )
                                                                        )
                                                                )
                                                                .border(
                                                                        width = 1.5.dp,
                                                                        color =
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.25f
                                                                                ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                // RIGHT ARROW icon for end handler
                                                Icon(
                                                        imageVector =
                                                                Icons.Filled.KeyboardArrowRight,
                                                        contentDescription = "End",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                        }
                                }
                        }
                }

                // Handle time labels - positioned below the waveform, outside the shaded region
                Box(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                        // Start handle time label
                        val startLabelOffsetX = (totalWidth - handleWidth) * activeSelectionStart
                        Text(
                                text = FileUtils.formatDurationWithDecimal(startTimeMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = with(LocalDensity.current) { 9.dp.toSp() },
                                modifier =
                                        Modifier.offset(x = startLabelOffsetX).width(handleWidth),
                                textAlign = TextAlign.Center
                        )

                        // End handle time label
                        val endLabelOffsetX = (totalWidth - handleWidth) * activeSelectionEnd
                        Text(
                                text = FileUtils.formatDurationWithDecimal(endTimeMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = with(LocalDensity.current) { 9.dp.toSp() },
                                modifier = Modifier.offset(x = endLabelOffsetX).width(handleWidth),
                                textAlign = TextAlign.Center
                        )
                }
        }
}

@Composable
private fun SelectionControls(
        selectionStartMs: Long,
        selectionEndMs: Long,
        selectionDurationMs: Long,
        isLoopMode: Boolean,
        onPlayLoop: () -> Unit,
        onStopLoop: () -> Unit
) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Selection time info card
                Surface(
                        color = MatteSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Column(
                                modifier = Modifier.padding(20.dp), // Increased from 16.dp to 20.dp
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Title
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = Icons.Default.ContentCut,
                                                contentDescription = null,
                                                tint = SecondaryCyan,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "Selection",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                        )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Time range display
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Start time
                                        TimeChip(
                                                label = "Start",
                                                time = FileUtils.formatDuration(selectionStartMs),
                                                color = SuccessGreen
                                        )

                                        // Arrow
                                        Icon(
                                                imageVector =
                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                        )

                                        // End time
                                        TimeChip(
                                                label = "End",
                                                time = FileUtils.formatDuration(selectionEndMs),
                                                color = ErrorRed
                                        )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Duration display
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = SecondaryCyan,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                                text =
                                                        "Duration: ${FileUtils.formatDuration(selectionDurationMs)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = SecondaryCyan,
                                                fontWeight = FontWeight.Medium
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Play loop button
                                Button(
                                        onClick = {
                                                if (isLoopMode) onStopLoop() else onPlayLoop()
                                        },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                if (isLoopMode) ErrorRed
                                                                else DeepPurple
                                                ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Icon(
                                                imageVector =
                                                        if (isLoopMode) Icons.Default.Stop
                                                        else Icons.Default.Loop,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text =
                                                        if (isLoopMode) "Stop Loop"
                                                        else "Play Selection",
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun TimeChip(label: String, time: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                                text = time,
                                style = MaterialTheme.typography.titleMedium,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                }
        }
}

@Composable
private fun CompactTimeControls(
        selectionStartMs: Long,
        selectionEndMs: Long,
        selectionDurationMs: Long,
        onStartTimeAdjust: (Long) -> Unit,
        onEndTimeAdjust: (Long) -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Start Time Control
                TimeControlChip(
                        label = "Start",
                        timeMs = selectionStartMs,
                        color = SuccessGreen,
                        onAdjust = onStartTimeAdjust
                )

                // Duration Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                text = "Duration",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                        )
                        Text(
                                text = FileUtils.formatDurationWithDecimal(selectionDurationMs),
                                style = MaterialTheme.typography.titleSmall,
                                color = SecondaryCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                        )
                }

                // End Time Control
                TimeControlChip(
                        label = "End",
                        timeMs = selectionEndMs,
                        color = ErrorRed,
                        onAdjust = onEndTimeAdjust
                )
        }
}

@Composable
private fun TimeControlChip(label: String, timeMs: Long, color: Color, onAdjust: (Long) -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Time with +/- buttons
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {

                        // Minus button with clear background
                        IconButton(
                                onClick = { onAdjust(-100) }, // -0.1 second
                                modifier =
                                        Modifier.background(
                                                        color = color.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                )
                                                .size(19.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = color,
                                        modifier = Modifier.size(14.dp) // Reduced from 16dp
                                )
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        // Time display
                        Surface(
                                color = color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Text(
                                        text = FileUtils.formatDurationWithDecimal(timeMs),
                                        style =
                                                MaterialTheme.typography
                                                        .labelMedium, // Reduced from labelLarge
                                        color = color,
                                        fontSize = 11.sp, // Smaller for long durations
                                        fontWeight = FontWeight.Bold,
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 5.dp
                                                ) // Reduced padding
                                )
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        // Plus button with clear background
                        IconButton(
                                onClick = { onAdjust(100) }, // +0.1 second
                                modifier =
                                        Modifier.background(
                                                        color = color.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                )
                                                .size(19.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = color,
                                        modifier = Modifier.size(14.dp) // Reduced from 16dp
                                )
                        }
                }
        }
}

@Composable
private fun AudioControlsRow(
        isLoopMode: Boolean,
        onVolumeClick: () -> Unit,
        onFadeClick: () -> Unit,
        onSpeedClick: () -> Unit,
        onLoopToggle: () -> Unit
) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .wrapContentHeight()
                                .padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                ), // Reduced from 16dp/12dp
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
        ) {
                // Volume
                AudioControlButton(
                        icon = Icons.Default.VolumeUp,
                        label = "Volume",
                        onClick = onVolumeClick
                )

                // Fade
                AudioControlButton(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        label = "Fade",
                        onClick = onFadeClick
                )

                // Speed
                AudioControlButton(
                        icon = Icons.Default.Speed,
                        label = "1.0x",
                        onClick = onSpeedClick
                )

                // Loop
                AudioControlButton(
                        icon = if (isLoopMode) Icons.Default.Loop else Icons.Default.Loop,
                        label = "Loop",
                        isActive = isLoopMode,
                        onClick = onLoopToggle
                )
        }
}

@Composable
private fun AudioControlButton(
        icon: ImageVector,
        label: String,
        isActive: Boolean = false,
        onClick: () -> Unit
) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
        ) {
                // Icon with circular background - smaller size
                Box(
                        modifier =
                                Modifier.size(40.dp) // Reduced from 48dp
                                        .background(
                                                color =
                                                        if (isActive) DeepPurple.copy(alpha = 0.5f)
                                                        else Color.White.copy(alpha = 0.08f),
                                                shape = CircleShape
                                        )
                                        .border(
                                                width = 1.5.dp,
                                                color =
                                                        if (isActive) SecondaryCyan
                                                        else Color.White.copy(alpha = 0.15f),
                                                shape = CircleShape
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isActive) SecondaryCyan else Color.White,
                                modifier = Modifier.size(20.dp) // Reduced from 24dp
                        )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Label text - outside of Box so it's not blocked
                Text(
                        text = label,
                        color = if (isActive) SecondaryCyan else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                )
        }
}

@Composable
private fun BottomPlayerControls(
        currentPosition: Long,
        duration: Long,
        isPlaying: Boolean,
        onPlayPause: () -> Unit,
        onSeek: (Float) -> Unit,
        audioTitle: String,
        audioArtist: String
) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.8f),
                                                                Color.Black
                                                        )
                                        )
                                )
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 32.dp, top = 16.dp)
                                .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Playback controls
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Skip backward
                        IconButton(
                                onClick = { /* TODO: Implement skip backward */},
                                modifier = Modifier.size(48.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Skip backward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                )
                        }

                        // Play/Pause button (main action)
                        Box(
                                modifier =
                                        Modifier.size(56.dp) // Reduced from 72dp
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                DeepPurple,
                                                                                SecondaryCyan
                                                                        )
                                                        )
                                                )
                                                .pointerInput(Unit) {
                                                        detectTapGestures { onPlayPause() }
                                                },
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector =
                                                if (isPlaying) Icons.Default.Pause
                                                else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp) // Reduced from 36dp
                                )
                        }

                        // Skip forward
                        IconButton(
                                onClick = { /* TODO: Implement skip forward */},
                                modifier = Modifier.size(40.dp) // Reduced from 48dp
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Skip forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp) // Reduced from 28dp
                                )
                        }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Reduced from 24dp)

                // Now playing info
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = DeepPurple,
                                modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "$audioTitle • $audioArtist",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
private fun LoadingState() {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                // Animated loading indicator
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val rotation by
                        infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec =
                                        infiniteRepeatable(
                                                animation = tween(1500, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                        ),
                                label = "rotation"
                        )

                Box(
                        modifier =
                                Modifier.size(80.dp).drawBehind {
                                        drawArc(
                                                brush =
                                                        Brush.sweepGradient(
                                                                colors =
                                                                        listOf(
                                                                                DeepPurple,
                                                                                SecondaryCyan,
                                                                                Color.Transparent
                                                                        )
                                                        ),
                                                startAngle = rotation,
                                                sweepAngle = 270f,
                                                useCenter = false,
                                                style =
                                                        Stroke(
                                                                width = 4.dp.toPx(),
                                                                cap = StrokeCap.Round
                                                        )
                                        )
                                },
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = DeepPurple,
                                modifier = Modifier.size(32.dp)
                        )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = "Analyzing waveform...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "This may take a moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                )
        }
}

@Composable
private fun EmptyWaveformState() {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
        ) {
                Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = "Couldn't load waveform",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "You can still play the audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                )
        }
}
