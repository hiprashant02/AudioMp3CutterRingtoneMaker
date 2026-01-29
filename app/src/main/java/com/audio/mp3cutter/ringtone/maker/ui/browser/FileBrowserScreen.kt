package com.audio.mp3cutter.ringtone.maker.ui.browser

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import com.audio.mp3cutter.ringtone.maker.utils.FileUtils

object PermissionUtils {
        fun getStoragePermission(): String {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }
        }

        fun hasPermission(context: Context): Boolean {
                return androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        getStoragePermission()
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
        onNavigateBack: () -> Unit,
        onAudioSelected: (AudioModel) -> Unit,
        onRecordVoice: () -> Unit = {},
        viewModel: FileBrowserViewModel = hiltViewModel()
) {
        val context = LocalContext.current
        var hasPermission by remember { mutableStateOf(PermissionUtils.hasPermission(context)) }

        val permissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { isGranted ->
                                hasPermission = isGranted
                                if (isGranted) {
                                        // Force a reset to clear 'isLastPage' flag from the failed
                                        // initial load
                                        viewModel.loadAudioFiles(reset = true)
                                }
                        }
                )

        LaunchedEffect(Unit) {
                if (!hasPermission) {
                        permissionLauncher.launch(PermissionUtils.getStoragePermission())
                }
        }

        // Cleanup playback when leaving screen
        DisposableEffect(Unit) { onDispose { viewModel.stopPlayback() } }

        val systemPickerLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent(),
                        onResult = { uri ->
                                uri?.let {
                                        // Process in background (simplified here for brevity,
                                        // ideally use
                                        // LaunchedEffect or ViewModelScope)
                                        // For now, blocking validation to keep flow simple as
                                        // copying small
                                        // audio files is fast
                                        val audio = viewModel.processUri(context, it)
                                        if (audio != null) {
                                                onAudioSelected(audio)
                                        } else {
                                                Toast.makeText(
                                                                context,
                                                                "Failed to import audio",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                        }
                                }
                        }
                )

        Scaffold(
                containerColor = Color.Black, // Pure Black Background
                topBar = {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .background(Color.Black)
                                                .statusBarsPadding()
                                                .padding(vertical = 16.dp)
                        ) {
                                // Custom Top Bar Layout
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Back Button
                                        Box(
                                                modifier =
                                                        Modifier.size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(MatteSurface)
                                                                .clickable { onNavigateBack() },
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Back",
                                                        tint = Color.White
                                                )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Search Field
                                        Box(modifier = Modifier.weight(1f)) {
                                                SearchField(
                                                        query =
                                                                viewModel.searchQuery
                                                                        .collectAsState()
                                                                        .value,
                                                        onQueryChange =
                                                                viewModel::onSearchQueryChanged
                                                )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Sort Button & Menu
                                        Box {
                                                var showSortMenu by remember {
                                                        mutableStateOf(false)
                                                }
                                                val currentSortOption by
                                                        viewModel.sortOption.collectAsState()

                                                Box(
                                                        modifier =
                                                                Modifier.size(40.dp)
                                                                        .clip(CircleShape)
                                                                        .background(MatteSurface)
                                                                        .clickable {
                                                                                showSortMenu = true
                                                                        },
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .Sort, // Ensure
                                                                // correct
                                                                // import
                                                                contentDescription = "Sort",
                                                                tint = Color.White
                                                        )
                                                }

                                                DropdownMenu(
                                                        expanded = showSortMenu,
                                                        onDismissRequest = { showSortMenu = false },
                                                        modifier =
                                                                Modifier.drawBehind {
                                                                        // 1. Base Dark Gradient
                                                                        drawRect(
                                                                                brush =
                                                                                        Brush.verticalGradient(
                                                                                                colors =
                                                                                                        listOf(
                                                                                                                DeepPurple,
                                                                                                                Color(
                                                                                                                        0xFF101018
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                                        // 2. The "Blob" (Glowing
                                                                        // Corner)
                                                                        val blobColor =
                                                                                ElectricBlue.copy(
                                                                                        alpha =
                                                                                                0.25f
                                                                                )
                                                                        drawCircle(
                                                                                brush =
                                                                                        Brush.radialGradient(
                                                                                                colors =
                                                                                                        listOf(
                                                                                                                blobColor,
                                                                                                                Color.Transparent
                                                                                                        ),
                                                                                                center =
                                                                                                        Offset(
                                                                                                                size.width,
                                                                                                                0f
                                                                                                        ),
                                                                                                radius =
                                                                                                        size.width *
                                                                                                                0.7f
                                                                                        ),
                                                                                center =
                                                                                        Offset(
                                                                                                size.width,
                                                                                                0f
                                                                                        ),
                                                                                radius =
                                                                                        size.width *
                                                                                                0.7f
                                                                        )
                                                                }
                                                ) {
                                                        com.audio.mp3cutter.ringtone.maker.data
                                                                .AudioSortOption.values()
                                                                .forEach { option ->
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Text(
                                                                                                text =
                                                                                                        option.displayName,
                                                                                                color =
                                                                                                        if (option ==
                                                                                                                        currentSortOption
                                                                                                        )
                                                                                                                Color.Cyan
                                                                                                        else
                                                                                                                Color.White,
                                                                                                fontWeight =
                                                                                                        if (option ==
                                                                                                                        currentSortOption
                                                                                                        )
                                                                                                                FontWeight
                                                                                                                        .Bold
                                                                                                        else
                                                                                                                FontWeight
                                                                                                                        .Normal
                                                                                        )
                                                                                },
                                                                                onClick = {
                                                                                        viewModel
                                                                                                .onSortOptionChanged(
                                                                                                        option
                                                                                                )
                                                                                        showSortMenu =
                                                                                                false
                                                                                }
                                                                        )
                                                                }
                                                }
                                        }
                                }
                        }
                }
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        if (hasPermission) {
                                val audioList by viewModel.audioList.collectAsState()
                                val currentPlaying by viewModel.currentPlayingAudio.collectAsState()
                                val isLoading by viewModel.isLoading.collectAsState()
                                val progress by viewModel.playbackProgress.collectAsState()

                                if (isLoading) {
                                        Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { CircularProgressIndicator(color = DeepPurple) }
                                } else {
                                        LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding =
                                                        PaddingValues(
                                                                horizontal = 20.dp,
                                                                vertical = 24.dp
                                                        ), // More top padding
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                // Record Voice Option
                                                item {
                                                        RecordVoiceCard(onClick = onRecordVoice)
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                }

                                                // Prominent Import Option
                                                item {
                                                        ImportAudioCard(
                                                                onClick = {
                                                                        systemPickerLauncher.launch(
                                                                                "audio/*"
                                                                        )
                                                                }
                                                        )
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                }

                                                if (audioList.isEmpty() && !isLoading
                                                ) { // Show empty only if not loading initial
                                                        item {
                                                                Box(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                top =
                                                                                                        48.dp
                                                                                        ),
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Column(
                                                                                horizontalAlignment =
                                                                                        Alignment
                                                                                                .CenterHorizontally
                                                                        ) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .MusicNote,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        tint =
                                                                                                TextSecondary
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.3f
                                                                                                        ),
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        64.dp
                                                                                                )
                                                                                )
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.height(
                                                                                                        16.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                "No audio files found",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyLarge,
                                                                                        color =
                                                                                                TextSecondary
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.5f
                                                                                                        )
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                } else {
                                                        items(audioList.size) { index ->
                                                                val audio = audioList[index]
                                                                val isPlaying =
                                                                        currentPlaying?.id ==
                                                                                audio.id

                                                                // Pagination Trigger
                                                                if (index == audioList.lastIndex) {
                                                                        LaunchedEffect(Unit) {
                                                                                viewModel
                                                                                        .loadNextPage()
                                                                        }
                                                                }

                                                                MinimalAudioItem(
                                                                        audio = audio,
                                                                        isPlaying = isPlaying,
                                                                        progress =
                                                                                if (isPlaying)
                                                                                        progress
                                                                                else 0f,
                                                                        onPlayPause = {
                                                                                viewModel
                                                                                        .togglePlayback(
                                                                                                audio
                                                                                        )
                                                                        },
                                                                        onSeek = {
                                                                                viewModel.seekTo(it)
                                                                        },
                                                                        onSelect = {
                                                                                // Stop playback
                                                                                // before navigating
                                                                                // to editor
                                                                                viewModel
                                                                                        .stopPlayback()
                                                                                Toast.makeText(
                                                                                                context,
                                                                                                "Selected: ${audio.title}",
                                                                                                Toast.LENGTH_SHORT
                                                                                        )
                                                                                        .show()
                                                                                onAudioSelected(
                                                                                        audio
                                                                                )
                                                                        }
                                                                )
                                                        }

                                                        // Loading More Indicator
                                                        item {
                                                                val isLoadingMore by
                                                                        viewModel.isLoadingMore
                                                                                .collectAsState()
                                                                if (isLoadingMore) {
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                CircularProgressIndicator(
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        24.dp
                                                                                                ),
                                                                                        color =
                                                                                                DeepPurple,
                                                                                        strokeWidth =
                                                                                                2.dp
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } else {
                                ModernPermissionRationale(
                                        onRequestPermission = {
                                                permissionLauncher.launch(
                                                        PermissionUtils.getStoragePermission()
                                                )
                                        }
                                )
                        }
                }
        }
}

// ... SearchField (Same as before) ...
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MatteSurface) // Matte Pill
                                .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle =
                                        LocalTextStyle.current.copy(
                                                color = Color.White,
                                                fontSize = 16.sp
                                        ),
                                cursorBrush = SolidColor(DeepPurple),
                                decorationBox = { innerTextField ->
                                        if (query.isEmpty()) {
                                                Text(
                                                        text = "Search",
                                                        color = TextSecondary.copy(alpha = 0.5f),
                                                        fontSize = 16.sp
                                                )
                                        }
                                        innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                        )
                }
        }
}

@Composable
fun MinimalAudioItem(
        audio: AudioModel,
        isPlaying: Boolean,
        progress: Float,
        onPlayPause: () -> Unit,
        onSeek: (Float) -> Unit,
        onSelect: () -> Unit
) {
        // Animate background color change when playing
        val backgroundColor by
                animateColorAsState(
                        targetValue =
                                if (isPlaying) Color(0xFF383848)
                                else Color.Transparent, // Highlighting
                        label = "bgColor"
                )

        // Interaction state for slider
        var isDragging by remember { mutableStateOf(false) }
        var dragProgress by remember { mutableFloatStateOf(0f) }

        // The value to display on the slider: drag value if dragging, else actual progress
        val sliderValue = if (isDragging) dragProgress else progress

        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .clickable { onSelect() }
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                .animateContentSize()
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        // Play/Pause Action
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (isPlaying) DeepPurple
                                                        else Color.White.copy(alpha = 0.05f)
                                                )
                                                .clickable { onPlayPause() },
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector =
                                                if (isPlaying) Icons.Default.Pause
                                                else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = if (isPlaying) Color.White else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Title & Info
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = audio.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                                if (isPlaying) Color.White
                                                else Color.White.copy(alpha = 0.9f),
                                        fontWeight =
                                                if (isPlaying) FontWeight.Bold
                                                else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )

                                if (!isPlaying) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text =
                                                        "${audio.artist} • ${FileUtils.formatDuration(audio.duration)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary.copy(alpha = 0.7f),
                                                maxLines = 1
                                        )
                                }
                        }

                        if (isPlaying) {
                                Spacer(modifier = Modifier.width(12.dp))
                                WaveformVisualizer(isPlaying = true)
                        }
                }

                // Progress Bar (Only visible when playing) - Custom gradient design
                if (isPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                // Current Time
                                Text(
                                        text =
                                                FileUtils.formatDuration(
                                                        (audio.duration * sliderValue).toLong()
                                                ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepPurple,
                                        modifier = Modifier.width(40.dp)
                                )

                                // Custom Gradient Seekbar (matching editor screen)
                                Box(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .height(24.dp) // Larger touch target
                                                        .pointerInput(Unit) {
                                                                detectTapGestures { offset ->
                                                                        val seekProgress =
                                                                                (offset.x /
                                                                                                size.width)
                                                                                        .coerceIn(
                                                                                                0f,
                                                                                                1f
                                                                                        )
                                                                        isDragging = false
                                                                        onSeek(seekProgress)
                                                                }
                                                        }
                                                        .pointerInput(Unit) {
                                                                detectHorizontalDragGestures(
                                                                        onDragStart = {
                                                                                isDragging = true
                                                                        },
                                                                        onDragEnd = {
                                                                                isDragging = false
                                                                                onSeek(dragProgress)
                                                                        },
                                                                        onHorizontalDrag = {
                                                                                change,
                                                                                _ ->
                                                                                change.consume()
                                                                                val seekProgress =
                                                                                        (change.position
                                                                                                        .x /
                                                                                                        size.width)
                                                                                                .coerceIn(
                                                                                                        0f,
                                                                                                        1f
                                                                                                )
                                                                                dragProgress =
                                                                                        seekProgress
                                                                        }
                                                                )
                                                        },
                                        contentAlignment = Alignment.Center
                                ) {
                                        // Track background
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(
                                                                        Color.White.copy(
                                                                                alpha = 0.15f
                                                                        )
                                                                )
                                        ) {
                                                // Progress fill with gradient
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth(sliderValue)
                                                                        .fillMaxHeight()
                                                                        .background(
                                                                                Brush.horizontalGradient(
                                                                                        colors =
                                                                                                listOf(
                                                                                                        DeepPurple,
                                                                                                        SecondaryCyan
                                                                                                )
                                                                                )
                                                                        )
                                                )
                                        }

                                        // Thumb indicator
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .wrapContentWidth(Alignment.Start)
                                        ) {
                                                BoxWithConstraints {
                                                        val maxWidthPx =
                                                                with(LocalDensity.current) {
                                                                        maxWidth.toPx()
                                                                }
                                                        Box(
                                                                modifier =
                                                                        Modifier.offset(
                                                                                        x =
                                                                                                ((sliderValue *
                                                                                                                maxWidthPx) /
                                                                                                                LocalDensity
                                                                                                                        .current
                                                                                                                        .density)
                                                                                                        .dp
                                                                                                        .coerceAtLeast(
                                                                                                                0.dp
                                                                                                        )
                                                                                                        .coerceAtMost(
                                                                                                                maxWidth -
                                                                                                                        16.dp
                                                                                                        )
                                                                                )
                                                                                .size(16.dp)
                                                                                .clip(CircleShape)
                                                                                .background(
                                                                                        Brush.radialGradient(
                                                                                                colors =
                                                                                                        listOf(
                                                                                                                SecondaryCyan,
                                                                                                                DeepPurple
                                                                                                        )
                                                                                        )
                                                                                )
                                                        )
                                                }
                                        }
                                }

                                Text(
                                        text = FileUtils.formatDuration(audio.duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.width(40.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                        }
                }
        }
}

@Composable
fun WaveformVisualizer(isPlaying: Boolean) {
        Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp)
        ) {
                repeat(5) { index ->
                        // Random-ish animation for each bar
                        val infiniteTransition = rememberInfiniteTransition(label = "bar_$index")
                        val heightPercent by
                                infiniteTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec =
                                                infiniteRepeatable(
                                                        animation =
                                                                tween(
                                                                        300 + (index * 50),
                                                                        easing = FastOutSlowInEasing
                                                                ),
                                                        repeatMode = RepeatMode.Reverse
                                                ),
                                        label = "height"
                                )

                        Box(
                                modifier =
                                        Modifier.width(3.dp)
                                                .fillMaxHeight(
                                                        if (isPlaying) heightPercent else 0.1f
                                                )
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(DeepPurple)
                        )
                }
        }
}

// ... ModernPermissionRationale (Same as before) ...
@Composable
fun ModernPermissionRationale(onRequestPermission: () -> Unit) {
        Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                // Large Gradient Icon
                Box(
                        modifier =
                                Modifier.size(100.dp)
                                        .clip(CircleShape)
                                        .background(
                                                Brush.linearGradient(
                                                        listOf(DeepPurple, ElectricBlue)
                                                )
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                        )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                        text = "Access Your Music",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                        text =
                                "To start editing, we need permission to view the audio files on your device.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))

                // Full Width Button
                Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(56.dp)
                                        .background(
                                                Brush.linearGradient(
                                                        listOf(DeepPurple, ElectricBlue)
                                                ),
                                                RoundedCornerShape(28.dp)
                                        ),
                        contentPadding =
                                PaddingValues() // Remove default padding for gradient background
                ) {
                        Text(
                                text = "Grant Access",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                        )
                }
        }
}

@Composable
fun ImportAudioCard(onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                        brush =
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        Color(0xFF2563EB),
                                                                        Color(0xFF06B6D4)
                                                                ) // Electric Blue -> Cyan
                                                )
                                )
                                .clickable { onClick() }
                                .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                                Text(
                                        text = "Import from Device",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = "Pick audio from other folders",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                                imageVector =
                                        Icons.AutoMirrored.Filled
                                                .ArrowBack, // Using Arrow for "Go" indication,
                                // rotated
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp).rotate(180f)
                        )
                }
        }
}

@Composable
fun RecordVoiceCard(onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                        brush =
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        Color(0xFFEC4899),
                                                                        Color(0xFFF43F5E)
                                                                ) // Pink -> Red (Recorder colors)
                                                )
                                )
                                .clickable { onClick() }
                                .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                                Text(
                                        text = "Record Voice",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = "Create ringtone from your voice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp).rotate(180f)
                        )
                }
        }
}
