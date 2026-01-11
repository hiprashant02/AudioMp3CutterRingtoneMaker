package com.audio.mp3cutter.ringtone.maker.ui.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import com.audio.mp3cutter.ringtone.maker.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Ambient background glow
        AmbientBackground(isPlaying = uiState.isPlaying)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            EditorTopBar(
                title = uiState.audio?.title ?: "Editor",
                onNavigateBack = onNavigateBack
            )

            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    LoadingState()
                } else if (uiState.waveformData.isNotEmpty()) {
                    Column {
                        WaveformSection(
                            waveformData = uiState.waveformData,
                            progress = if (uiState.duration > 0) {
                                uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                            } else 0f,
                            isPlaying = uiState.isPlaying,
                            selectionStart = uiState.selectionStartProgress,
                            selectionEnd = uiState.selectionEndProgress,
                            duration = uiState.duration,
                            onSeek = { progress ->
                                viewModel.seekToProgress(progress)
                            },
                            onSeekAndPlay = { progress ->
                                viewModel.seekAndPlay(progress)
                            },
                            onSelectionStartChange = { viewModel.updateSelectionStart(it) },
                            onSelectionEndChange = { viewModel.updateSelectionEnd(it) }
                        )

                        // Selection Info & Controls
                        SelectionControls(
                            selectionStartMs = uiState.selectionStartMs,
                            selectionEndMs = uiState.selectionEndMs,
                            selectionDurationMs = uiState.selectionDurationMs,
                            isLoopMode = uiState.isLoopMode,
                            onPlayLoop = { viewModel.playSelectedLoop() },
                            onStopLoop = { viewModel.stopLoopMode() }
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

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val offsetX by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(150.dp)
    ) {
        // Primary purple glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DeepPurple.copy(alpha = if (isPlaying) pulseAlpha else 0.1f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f + offsetX, size.height * 0.3f),
                radius = size.width * 0.6f
            ),
            center = Offset(size.width * 0.3f + offsetX, size.height * 0.3f),
            radius = size.width * 0.6f
        )

        // Secondary cyan glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SecondaryCyan.copy(alpha = if (isPlaying) pulseAlpha * 0.7f else 0.05f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.8f - offsetX, size.height * 0.7f),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * 0.8f - offsetX, size.height * 0.7f),
            radius = size.width * 0.5f
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(44.dp)
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
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MatteSurface),
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

    // Disable auto-scroll - let user control scrolling manually
    // Auto-scroll was causing issues with handle dragging

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(50),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zoom out button
            IconButton(
                onClick = { scale = (scale / 1.5f).coerceAtLeast(1f) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MatteSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom out",
                    tint = if (scale > 1f) Color.White else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Zoom indicator
            Surface(
                color = MatteSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepPurple,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Zoom in button
            IconButton(
                onClick = { scale = (scale * 1.5f).coerceAtMost(8f) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MatteSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom in",
                    tint = if (scale < 8f) Color.White else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Waveform container - clean bordered design
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
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
            
            // Separate speed and callback to prevent LaunchedEffect restart on every drag
            var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
            var autoScrollUpdateCallback by remember { mutableStateOf<((Float) -> Unit)?>(null) }
            var isAutoScrollActive by remember { mutableStateOf(false) }
            
            // Auto-scroll logic - ONLY scrolls view, never updates selection
            LaunchedEffect(autoScrollSpeed != 0f) {
                val wasActive = isAutoScrollActive
                isAutoScrollActive = autoScrollSpeed != 0f
                android.util.Log.d("AutoScrollLoop", "LaunchedEffect STARTED: speed=$autoScrollSpeed, wasActive=$wasActive, nowActive=$isAutoScrollActive")
                
                while (autoScrollSpeed != 0f && isActive) {
                    val speed = autoScrollSpeed
                    val scrolled = scrollState.scrollBy(speed)
                    android.util.Log.d("AutoScrollLoop", "SCROLLING: requested=$speed, actualScrolled=$scrolled, totalScroll=${scrollState.value}, loopActive=$isActive")
                    // NO selection update here - let drag handle it
                    delay(16)
                }
                
                isAutoScrollActive = false
                android.util.Log.d("AutoScrollLoop", "LaunchedEffect ENDED: finalSpeed=$autoScrollSpeed, isActive=$isActive")
            }

            // Horizontally scrollable waveform with integrated selection handles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
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
                    isAutoScrollActive = isAutoScrollActive,
                    onAutoScrollRequest = { speed, updateCallback -> 
                        android.util.Log.d("AutoScrollRequest", "REQUEST: speed=$speed, currentSpeed=$autoScrollSpeed")
                        autoScrollSpeed = speed
                        autoScrollUpdateCallback = updateCallback
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
            }


            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MatteSurface,
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MatteSurface
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hint text
        Text(
            text = "⟷ Drag the handles to select start & end",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f)
        )
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
    isAutoScrollActive: Boolean,
    onAutoScrollRequest: (Float, ((Float) -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProgressState = rememberUpdatedState(progress)
    val density = LocalDensity.current
    val barWidthDp = 3.dp
    val barSpacingDp = 2.dp
    val totalBarWidthDp = barWidthDp + barSpacingDp
    val handleWidth = 28.dp

    // Calculate total width based on data and scale
    val baseBarCount = waveformData.size.coerceIn(100, 500)
    val displayedBars = (baseBarCount * scale).toInt().coerceIn(50, waveformData.size)
    val totalWidth = totalBarWidthDp * displayedBars

    val maxAmplitude = remember(waveformData) {
        waveformData.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    }

    // Sample the waveform data
    val sampledData = remember(waveformData, displayedBars) {
        if (waveformData.isEmpty()) emptyList()
        else {
            val step = waveformData.size.toFloat() / displayedBars
            (0 until displayedBars).map { i ->
                val index = (i * step).toInt().coerceIn(0, waveformData.size - 1)
                waveformData[index]
            }
        }
    }



    // Track current positions with rememberUpdatedState for drag callbacks
    val totalWidthPx = with(density) { totalWidth.toPx() }
    val currentStartPosition by rememberUpdatedState(selectionStart)
    val currentEndPosition by rememberUpdatedState(selectionEnd)


    // Calculate handle time values
    val startTimeMs = (duration * selectionStart).toLong()
    val endTimeMs = (duration * selectionEnd).toLong()

    Column(modifier = modifier.width(totalWidth)) {
        // Waveform with handles
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Waveform canvas (background)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val tapProgress = offset.x / size.width
                            onSeek(tapProgress.coerceIn(0f, 1f))
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val currentX = currentProgressState.value * size.width
                            val touchThreshold = with(density) { 30.dp.toPx() } // Hit target size

                            // ONLY intercept if touching near the playhead
                            if (Math.abs(down.position.x - currentX) < touchThreshold) {
                                var pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == pointerId }
                                    if (change == null || !change.pressed) break
                                    
                                    if (change.position - change.previousPosition != Offset.Zero) {
                                        change.consume() // Consume drag to prevent scrolling
                                        val dragProgress = change.position.x / size.width
                                        onSeek(dragProgress.coerceIn(0f, 1f))
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
                val selectionStartX = selectionStart * size.width
                val selectionEndX = selectionEnd * size.width

                // Subtle GLOW on selected region instead of dark unselected
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SecondaryCyan.copy(alpha = 0.08f),
                            SecondaryCyan.copy(alpha = 0.15f),
                            SecondaryCyan.copy(alpha = 0.08f)
                        )
                    ),
                    topLeft = Offset(selectionStartX, 0f),
                    size = Size(selectionEndX - selectionStartX, size.height)
                )

                // Draw waveform bars
                sampledData.forEachIndexed { index, amplitude ->
                    val normalizedHeight = (amplitude / maxAmplitude).coerceIn(0.05f, 1f)
                    val barHeight = size.height * 0.75f * normalizedHeight

                    val x = index * totalBarWidth
                    val barProgress = index.toFloat() / sampledData.size

                    val isInSelection = barProgress >= selectionStart && barProgress <= selectionEnd
                    val barColor = if (isInSelection) {
                        // Selected region: bright gradient (purple to cyan)
                        Brush.verticalGradient(
                            colors = listOf(DeepPurple, SecondaryCyan),
                            startY = centerY - barHeight / 2,
                            endY = centerY + barHeight / 2
                        )
                    } else {
                        // Outside selection: dim white
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.08f)),
                            startY = centerY - barHeight / 2,
                            endY = centerY + barHeight / 2
                        )
                    }

                    drawRoundRect(
                        brush = barColor,
                        topLeft = Offset(x, centerY - barHeight / 2),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
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
            }

            // Start Handle - positioned on waveform (full height, no time text inside)
            val startOffsetX = (totalWidth - handleWidth) * selectionStart
            Box(
                modifier = Modifier
                    .offset(x = startOffsetX)
                    .width(handleWidth)
                    .fillMaxHeight()
                    .pointerInput(totalWidthPx) {
                        detectDragGestures(
                            onDragEnd = { onAutoScrollRequest(0f, null) },
                            onDragCancel = { onAutoScrollRequest(0f, null) }
                        ) { change, dragAmount ->
                            change.consume()
                            
                            // ALWAYS update selection from drag - this is the ONLY source
                            val dragProgress = dragAmount.x / totalWidthPx
                            val newPos = (currentStartPosition + dragProgress).coerceIn(0f, currentEndPosition - 0.02f)
                            onSelectionStartChange(newPos)
                            onSeekAndPlay(newPos)

                            // Auto-scroll based on FINGER position on screen
                            // But ONLY if there's room to scroll in that direction!
                            val fingerX = change.position.x
                            val threshold = 300f
                            val scrollSpeed = 120f
                            val currentScroll = scrollState.value
                            val maxScroll = scrollState.maxValue

                            android.util.Log.d("AutoScroll", "DRAG EVENT: fingerX=$fingerX, currentScroll=$currentScroll, maxScroll=$maxScroll, viewport=$viewportWidth, isActive=$isAutoScrollActive")

                            if (fingerX < threshold && currentScroll > 0) {
                                // Near left edge AND can scroll left
                                android.util.Log.d("AutoScroll", ">>> REQUESTING LEFT SCROLL (fingerX=$fingerX < $threshold, canScroll=${currentScroll > 0})")
                                onAutoScrollRequest(-scrollSpeed, null)
                           } else if (fingerX > viewportWidth - threshold && currentScroll < maxScroll) {
                                // Near right edge AND can scroll right
                                android.util.Log.d("AutoScroll", ">>> REQUESTING RIGHT SCROLL (fingerX=$fingerX > ${viewportWidth - threshold}, canScroll=${currentScroll < maxScroll})")
                                onAutoScrollRequest(scrollSpeed, null)
                            } else {
                                android.util.Log.d("AutoScroll", ">>> IN SAFE ZONE OR CAN'T SCROLL")
                                onAutoScrollRequest(0f, null)
                            }
                            // Note: No else - keep scrolling once started
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Top section - thin fading line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DeepPurple.copy(alpha = 0.4f),
                                        DeepPurple
                                    )
                                )
                            )
                    )

                    // Center grip ONLY - small draggable pill
                    Box(
                        modifier = Modifier
                            .size(16.dp, 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(DeepPurple, SecondaryCyan)
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // LEFT ARROW icon for start handler
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Start",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bottom section - thin fading line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SecondaryCyan,
                                        SecondaryCyan.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }


            // End Handle - positioned on waveform (full height, no time text inside)
            val endOffsetX = (totalWidth - handleWidth) * selectionEnd
            Box(
                modifier = Modifier
                    .offset(x = endOffsetX)
                    .width(handleWidth)
                    .fillMaxHeight()
                    .pointerInput(totalWidthPx) {
                        detectDragGestures(
                            onDragEnd = { onAutoScrollRequest(0f, null) },
                            onDragCancel = { onAutoScrollRequest(0f, null) }
                        ) { change, dragAmount ->
                            change.consume()
                            
                            // ALWAYS update selection from drag - this is the ONLY source
                            val dragProgress = dragAmount.x / totalWidthPx
                            val newPos = (currentEndPosition + dragProgress).coerceIn(currentStartPosition + 0.02f, 1f)
                            onSelectionEndChange(newPos)

                            // Auto-scroll triggers ONLY on handle screen position
                            // Scroll just moves the view, doesn't update selection
                            val currentHandleX = newPos * totalWidthPx
                            val screenX = currentHandleX - scrollState.value
                            val threshold = 300f
                            val scrollSpeed = 120f

                            if (screenX < threshold) {
                                onAutoScrollRequest(-scrollSpeed, null)
                            } else if (screenX > viewportWidth - threshold) {
                                onAutoScrollRequest(scrollSpeed, null)
                            }
                            // Note: No else - keep scrolling once started
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Top section - thin fading line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DeepPurple.copy(alpha = 0.4f),
                                        DeepPurple
                                    )
                                )
                            )
                    )

                    // Center grip ONLY - small draggable pill
                    Box(
                        modifier = Modifier
                            .size(16.dp, 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(DeepPurple, SecondaryCyan)
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // RIGHT ARROW icon for end handler
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "End",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bottom section - thin fading line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SecondaryCyan,
                                        SecondaryCyan.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }

        // Handle time labels - positioned below the waveform, outside the shaded region
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            // Start handle time label
            val startLabelOffsetX = (totalWidth - handleWidth) * selectionStart
            Text(
                text = FileUtils.formatDuration(startTimeMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = with(LocalDensity.current) { 9.dp.toSp() },
                modifier = Modifier
                    .offset(x = startLabelOffsetX)
                    .width(handleWidth),
                textAlign = TextAlign.Center
            )

            // End handle time label
            val endLabelOffsetX = (totalWidth - handleWidth) * selectionEnd
            Text(
                text = FileUtils.formatDuration(endTimeMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = with(LocalDensity.current) { 9.dp.toSp() },
                modifier = Modifier
                    .offset(x = endLabelOffsetX)
                    .width(handleWidth),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selection time info card
        Surface(
            color = MatteSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
                        text = "Duration: ${FileUtils.formatDuration(selectionDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryCyan,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play loop button
                Button(
                    onClick = { if (isLoopMode) onStopLoop() else onPlayLoop() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoopMode) ErrorRed else DeepPurple
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isLoopMode) Icons.Default.Stop else Icons.Default.Loop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoopMode) "Stop Loop" else "Play Selection",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeChip(
    label: String,
    time: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
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
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = FileUtils.formatDuration(currentPosition),
                style = MaterialTheme.typography.labelLarge,
                color = DeepPurple,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = FileUtils.formatDuration(duration),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Seekable Progress bar
        val progressPercent = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat())
        } else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp) // Larger touch target
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val seekProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(seekProgress)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val seekProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(seekProgress)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                // Progress fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercent)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(DeepPurple, SecondaryCyan)
                            )
                        )
                )
            }

            // Thumb indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.Start)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (progressPercent * 300).dp.coerceAtMost(340.dp)) // Approximate offset
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(SecondaryCyan, DeepPurple)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip backward
            IconButton(
                onClick = { /* TODO: Implement skip backward */ },
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
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepPurple, SecondaryCyan)
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { onPlayPause() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Skip forward
            IconButton(
                onClick = { /* TODO: Implement skip forward */ },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Skip forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .drawBehind {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                DeepPurple,
                                SecondaryCyan,
                                Color.Transparent
                            )
                        ),
                        startAngle = rotation,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
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
