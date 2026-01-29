package com.audio.mp3cutter.ringtone.maker.ui.merger

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import com.audio.mp3cutter.ringtone.maker.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMergerScreen(
        onNavigateBack: () -> Unit,
        onAddSong: () -> Unit,
        onSaveAudio: (AudioModel) -> Unit,
        onCutAudio: (AudioModel) -> Unit,
        viewModel: AudioMergerViewModel = hiltViewModel()
) {
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsState()


        LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                }
        }

        if (uiState.isProcessing) {
                PremiumProcessingDialog(
                        message = uiState.processingMessage,
                        progress = uiState.processingProgress
                )
        }

        // Preview Dialog after merge completes
        if (uiState.showPreviewDialog && uiState.mergedAudio != null) {
                MergedAudioPreviewDialog(
                        audio = uiState.mergedAudio!!,
                        isPlaying = uiState.isPlaying,
                        playbackProgress = uiState.playbackProgress,
                        currentPosition = uiState.currentPlaybackPosition,
                        onPlayPause = { viewModel.togglePlayback() },
                        onSeek = { viewModel.seekTo(it) },
                        onDismiss = { viewModel.dismissPreviewDialog() },
                        onSave = {
                                viewModel.onSaveClicked()?.let { audio ->
                                        onSaveAudio(audio)
                                }
                        },
                        onCut = {
                                viewModel.onCutterClicked()?.let { audio ->
                                        onCutAudio(audio)
                                }
                        }
                )
        }

        Scaffold(
                containerColor = Color.Black,
                topBar = {
                        PremiumTopBar(
                                onNavigateBack = onNavigateBack,
                                songCount = uiState.selectedAudios.size
                        )
                },
                bottomBar = {
                        PremiumMergeBottomBar(
                                canMerge = uiState.canMerge,
                                totalDuration = uiState.totalDuration,
                                songCount = uiState.selectedAudios.size,
                                onMerge = { viewModel.mergeAudios() }
                        )
                }
        ) { paddingValues ->
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color(0xFF121212),
                                                                        Color.Black
                                                                )
                                                )
                                        )
                ) {
                        // Ambient Background Blob
                        Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                        brush =
                                                Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        DeepPurple.copy(
                                                                                alpha = 0.15f
                                                                        ),
                                                                        Color.Transparent
                                                                ),
                                                        center = Offset(size.width, 0f),
                                                        radius = size.width * 0.8f
                                                )
                                )
                                drawCircle(
                                        brush =
                                                Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        SecondaryCyan.copy(
                                                                                alpha = 0.1f
                                                                        ),
                                                                        Color.Transparent
                                                                ),
                                                        center = Offset(0f, size.height * 0.4f),
                                                        radius = size.width * 0.6f
                                                )
                                )
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                                // Banner Ad
                                com.audio.mp3cutter.ringtone.maker.ui.ads.BannerAd(
                                    adUnitId = com.audio.mp3cutter.ringtone.maker.BuildConfig.ADMOB_BANNER_ID,
                                    modifier = Modifier.padding(horizontal = 20.dp).padding(vertical = 8.dp)
                                )

                                // Add Songs Button
                                PremiumAddButton(onClick = onAddSong)

                                Spacer(modifier = Modifier.height(24.dp))

                                // Section Title
                                if (uiState.selectedAudios.isNotEmpty()) {
                                        Text(
                                                text = "Your Mix",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextSecondary,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 20.dp,
                                                                vertical = 8.dp
                                                        )
                                        )
                                }

                                // Songs List
                                if (uiState.selectedAudios.isEmpty()) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        PremiumEmptyState()
                                        Spacer(modifier = Modifier.weight(1f))
                                } else {
                                        PremiumSongList(
                                                audios = uiState.selectedAudios,
                                                onRemove = { viewModel.removeAudio(it) },
                                                onReorder = { from, to ->
                                                        viewModel.reorderAudios(from, to)
                                                }
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun PremiumTopBar(onNavigateBack: () -> Unit, songCount: Int) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                        // Back Button
                        IconButton(
                                onClick = onNavigateBack,
                                modifier =
                                        Modifier.size(44.dp)
                                                .clip(CircleShape)
                                                .background(MatteSurface.copy(alpha = 0.5f))
                        ) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = "Audio Merger",
                                        style =
                                                MaterialTheme.typography.headlineSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = (-0.5).sp
                                                ),
                                        color = Color.White
                                )
                                Text(
                                        text =
                                                if (songCount > 0) "$songCount tracks selected"
                                                else "Create your masterpiece",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                if (songCount > 0) SecondaryCyan
                                                else TextSecondary.copy(alpha = 0.7f)
                                )
                        }

                        // Pro badge or Icon
                        Box(
                                modifier =
                                        Modifier.clip(RoundedCornerShape(50))
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                DeepPurple,
                                                                                AccentPink
                                                                        )
                                                        )
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                                Text(
                                        text = "STUDIO",
                                        color = Color.White,
                                        style =
                                                MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.sp
                                                )
                                )
                        }
                }
        }
}

@Composable
private fun PremiumAddButton(onClick: () -> Unit) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceElevated)
                                .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onClick() }
                                .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Icon with accent background
                Box(
                        modifier =
                                Modifier.size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DeepPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = DeepPurple,
                                modifier = Modifier.size(22.dp)
                        )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Text
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Add Songs",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                        )
                        Text(
                                text = "Browse library or record",
                                color = TextSecondary,
                                fontSize = 12.sp
                        )
                }

                // Arrow
                Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                )
        }
}

@Composable
private fun PremiumSongList(
        audios: List<AudioModel>,
        onRemove: (Int) -> Unit,
        onReorder: (Int, Int) -> Unit
) {
        val listState = rememberLazyListState()
        val density = LocalDensity.current

        // Drag state
        var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }

        // Item height (80dp + 12dp spacing)
        val itemHeightPx = with(density) { 92.dp.toPx() }

        LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                itemsIndexed(audios, key = { _, audio -> audio.id }) { index, audio ->
                        val isDragging = draggedItemIndex == index

                        // Calculate visual offset for this item
                        val offsetY =
                                when {
                                        isDragging -> dragOffset
                                        draggedItemIndex != null -> {
                                                val draggedIdx = draggedItemIndex!!
                                                val draggedTargetIdx =
                                                        (draggedIdx +
                                                                        (dragOffset / itemHeightPx)
                                                                                .toInt())
                                                                .coerceIn(0, audios.lastIndex)

                                                when {
                                                        // Current item needs to move up (dragged
                                                        // item moving down past it)
                                                        index in
                                                                (draggedIdx +
                                                                        1)..draggedTargetIdx ->
                                                                -itemHeightPx
                                                        // Current item needs to move down (dragged
                                                        // item moving up past it)
                                                        index in
                                                                draggedTargetIdx until draggedIdx ->
                                                                itemHeightPx
                                                        else -> 0f
                                                }
                                        }
                                        else -> 0f
                                }

                        DraggableSongItem(
                                index = index,
                                audio = audio,
                                isDragging = isDragging,
                                offsetY = offsetY,
                                onRemove = { onRemove(index) },
                                onDragStart = { draggedItemIndex = index },
                                onDrag = { delta -> dragOffset += delta },
                                onDragEnd = {
                                        draggedItemIndex?.let { fromIdx ->
                                                val toIdx =
                                                        (fromIdx +
                                                                        (dragOffset / itemHeightPx)
                                                                                .toInt())
                                                                .coerceIn(0, audios.lastIndex)
                                                if (fromIdx != toIdx) {
                                                        onReorder(fromIdx, toIdx)
                                                }
                                        }
                                        draggedItemIndex = null
                                        dragOffset = 0f
                                }
                        )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
        }
}

@Composable
private fun DraggableSongItem(
        index: Int,
        audio: AudioModel,
        isDragging: Boolean,
        offsetY: Float,
        onRemove: () -> Unit,
        onDragStart: () -> Unit,
        onDrag: (Float) -> Unit,
        onDragEnd: () -> Unit
) {
        val elevation by
                animateDpAsState(targetValue = if (isDragging) 8.dp else 0.dp, label = "elevation")
        val scale by
                animateFloatAsState(targetValue = if (isDragging) 1.02f else 1f, label = "scale")

        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .graphicsLayer {
                                        translationY = offsetY
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = elevation.toPx()
                                }
                                .height(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                        if (isDragging) SurfaceElevated.copy(alpha = 0.95f)
                                        else SurfaceElevated
                                )
                                .border(
                                        width = if (isDragging) 1.5.dp else 1.dp,
                                        color =
                                                if (isDragging) DeepPurple.copy(alpha = 0.5f)
                                                else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(20.dp)
                                )
                                .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                                onDragStart = { onDragStart() },
                                                onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        onDrag(dragAmount.y)
                                                },
                                                onDragEnd = { onDragEnd() },
                                                onDragCancel = { onDragEnd() }
                                        )
                                }
                                .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Drag Handle (visual indicator)
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint =
                                        if (isDragging) DeepPurple
                                        else TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                        )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Album Art Placeholder
                Box(
                        modifier =
                                Modifier.size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                                Brush.linearGradient(
                                                        colors =
                                                                if (index % 2 == 0)
                                                                        listOf(
                                                                                Color(0xFF2E2E3A),
                                                                                Color(0xFF1C1C22)
                                                                        )
                                                                else
                                                                        listOf(
                                                                                Color(0xFF333340),
                                                                                Color(0xFF222228)
                                                                        )
                                                )
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = if (index % 2 == 0) DeepPurple else SecondaryCyan,
                                modifier = Modifier.size(28.dp).alpha(0.8f)
                        )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = audio.title,
                                style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                        ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text =
                                        "${audio.artist} • ${FileUtils.formatDuration(audio.duration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f),
                                maxLines = 1
                        )
                }

                // Delete Action
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove",
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                        )
                }
        }
}

@Composable
private fun PremiumEmptyState() {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
        ) {
                // Animated Pulses
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by
                        infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.1f,
                                animationSpec =
                                        infiniteRepeatable(
                                                animation =
                                                        tween(2000, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                        ),
                                label = "scale"
                        )
                val alpha by
                        infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 0.05f,
                                animationSpec =
                                        infiniteRepeatable(
                                                animation =
                                                        tween(2000, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                        ),
                                label = "alpha"
                        )

                Box(contentAlignment = Alignment.Center) {
                        Box(
                                modifier =
                                        Modifier.size(160.dp)
                                                .scale(scale)
                                                .clip(CircleShape)
                                                .background(DeepPurple.copy(alpha = alpha))
                        )
                        Box(
                                modifier =
                                        Modifier.size(120.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                MatteSurface,
                                                                                Color.Black
                                                                        )
                                                        )
                                                )
                                                .border(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.1f),
                                                        CircleShape
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(48.dp)
                                )
                        }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                        text = "Your Studio is Empty",
                        style =
                                MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                        color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                        text =
                                "Start adding tracks to blend them together.\nCreate something unique!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                )
        }
}

@Composable
private fun PremiumMergeBottomBar(
        canMerge: Boolean,
        totalDuration: Long,
        songCount: Int,
        onMerge: () -> Unit
) {
        if (songCount > 0) {
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                        // Glass effect background for floating bar
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(80.dp)
                                                .shadow(
                                                        16.dp,
                                                        RoundedCornerShape(24.dp),
                                                        spotColor = Color.Black
                                                )
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(MatteSurface.copy(alpha = 0.9f))
                                                .border(
                                                        1.dp,
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color.White.copy(
                                                                                        alpha = 0.1f
                                                                                ),
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.02f
                                                                                )
                                                                        )
                                                        ),
                                                        RoundedCornerShape(24.dp)
                                                )
                        ) {
                                Row(
                                        modifier =
                                                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Column {
                                                Text(
                                                        text = "Total Duration",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                )
                                                Text(
                                                        text =
                                                                FileUtils.formatDuration(
                                                                        totalDuration
                                                                ),
                                                        style =
                                                                MaterialTheme.typography.titleMedium
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color = Color.White
                                                )
                                        }

                                        Button(
                                                onClick = onMerge,
                                                enabled = canMerge,
                                                shape = RoundedCornerShape(16.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = Color.Transparent,
                                                                disabledContainerColor =
                                                                        Color.Transparent
                                                        ),
                                                contentPadding =
                                                        PaddingValues(
                                                                0.dp
                                                        ), // Reset padding to use Box
                                                modifier =
                                                        Modifier.height(48.dp)
                                                                .width(140.dp)
                                                                .shadow(
                                                                        if (canMerge) 12.dp
                                                                        else 0.dp,
                                                                        RoundedCornerShape(16.dp),
                                                                        spotColor =
                                                                                DeepPurple.copy(
                                                                                        alpha = 0.5f
                                                                                )
                                                                )
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxSize()
                                                                        .background(
                                                                                if (canMerge)
                                                                                        DeepPurple
                                                                                else
                                                                                        Color(
                                                                                                0xFF2A2A30
                                                                                        )
                                                                        ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Merge,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                if (canMerge)
                                                                                        Color.White
                                                                                else
                                                                                        TextSecondary
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.5f
                                                                                                ),
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(8.dp)
                                                                )
                                                                Text(
                                                                        text = "MERGE",
                                                                        fontWeight =
                                                                                FontWeight.Bold,
                                                                        color =
                                                                                if (canMerge)
                                                                                        Color.White
                                                                                else
                                                                                        TextSecondary
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.5f
                                                                                                ),
                                                                        letterSpacing = 1.sp
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun PremiumProcessingDialog(message: String, progress: Int) {
        Dialog(onDismissRequest = { /* Cannot dismiss */}) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(SurfaceElevated)
                                        .border(
                                                1.dp,
                                                Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(32.dp)
                                        )
                                        .padding(32.dp),
                        contentAlignment = Alignment.Center
                ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Spinning gradient ring
                                val infiniteTransition =
                                        rememberInfiniteTransition(label = "processing")
                                val rotation by
                                        infiniteTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 360f,
                                                animationSpec =
                                                        infiniteRepeatable(
                                                                animation =
                                                                        tween(
                                                                                2000,
                                                                                easing =
                                                                                        LinearEasing
                                                                        )
                                                        ),
                                                label = "rotation"
                                        )

                                Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Canvas(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .scale(1.2f)
                                                                .rotate(rotation)
                                        ) {
                                                drawCircle(
                                                        brush =
                                                                Brush.sweepGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        Color.Transparent,
                                                                                        DeepPurple,
                                                                                        AccentPink,
                                                                                        Color.Transparent
                                                                                )
                                                                ),
                                                        radius = size.width / 2,
                                                        style =
                                                                androidx.compose.ui.graphics
                                                                        .drawscope.Stroke(
                                                                        width = 12f,
                                                                        cap =
                                                                                androidx.compose.ui
                                                                                        .graphics
                                                                                        .StrokeCap
                                                                                        .Round
                                                                )
                                                )
                                        }

                                        Icon(
                                                imageVector = Icons.Default.AutoFixHigh,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                        text = "Mixing Your Audio",
                                        style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = Color.White
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Progress
                                Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        text = "Processing",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                )
                                                Text(
                                                        text = "$progress%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AccentPink,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                                progress = { progress / 100f },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(3.dp)),
                                                color = AccentPink,
                                                trackColor = Color(0xFF2A2A30),
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun MergedAudioPreviewDialog(
        audio: AudioModel,
        isPlaying: Boolean,
        playbackProgress: Float,
        currentPosition: Long,
        onPlayPause: () -> Unit,
        onSeek: (Float) -> Unit,
        onDismiss: () -> Unit,
        onSave: () -> Unit,
        onCut: () -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(SurfaceElevated)
                                .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(32.dp)
                                )
                                .padding(24.dp)
                ) {
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                // Success Icon
                                Box(
                                        modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors = listOf(DeepPurple, AccentPink)
                                                        )
                                                ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(40.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Merge Complete!",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text = "Preview your merged audio",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Audio Info Card
                                Row(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MatteSurface)
                                                .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Box(
                                                modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                                Brush.linearGradient(
                                                                        colors = listOf(
                                                                                DeepPurple.copy(alpha = 0.3f),
                                                                                DeepPurple.copy(alpha = 0.1f)
                                                                        )
                                                                )
                                                        ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Rounded.MusicNote,
                                                        contentDescription = null,
                                                        tint = DeepPurple,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        text = audio.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                        text = "${audio.artist} • ${FileUtils.formatDuration(audio.duration)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Playback Progress
                                Column(modifier = Modifier.fillMaxWidth()) {
                                        Slider(
                                                value = playbackProgress,
                                                onValueChange = { onSeek(it) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = SliderDefaults.colors(
                                                        thumbColor = DeepPurple,
                                                        activeTrackColor = DeepPurple,
                                                        inactiveTrackColor = Color(0xFF2A2A30)
                                                )
                                        )

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        text = FileUtils.formatDuration(currentPosition),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                )
                                                Text(
                                                        text = FileUtils.formatDuration(audio.duration),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Play/Pause Button
                                Box(
                                        modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(DeepPurple)
                                                .clickable { onPlayPause() },
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                // Action Buttons
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        // Cutter Button
                                        Button(
                                                onClick = onCut,
                                                modifier = Modifier
                                                        .weight(1f)
                                                        .height(52.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                        containerColor = MatteSurface
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.1f)
                                                )
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.ContentCut,
                                                        contentDescription = null,
                                                        tint = SecondaryCyan,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        text = "Cut",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }

                                        // Save Button
                                        Button(
                                                onClick = onSave,
                                                modifier = Modifier
                                                        .weight(1f)
                                                        .height(52.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                        containerColor = DeepPurple
                                                )
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Save,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        text = "Save",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }
                                }
                        }
                }
        }
}

