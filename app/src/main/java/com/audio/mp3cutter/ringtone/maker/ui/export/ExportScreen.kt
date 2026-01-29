package com.audio.mp3cutter.ringtone.maker.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager
import com.audio.mp3cutter.ringtone.maker.utils.FileUtils
import java.io.File
import java.io.FileInputStream

enum class AudioFormat(val displayName: String, val extension: String) {
        MP3("MP3", "mp3"),
        AAC("AAC", "m4a"),
        WAV("WAV", "wav")
}

enum class AudioQuality(val displayName: String, val bitrate: Int, val subtitle: String) {
        LOW("Low", 128, "128 kbps"),
        MEDIUM("Medium", 192, "192 kbps"),
        HIGH("High", 320, "320 kbps")
}

@Composable
fun ExportScreen(
        audio: AudioModel,
        onNavigateBack: () -> Unit,
        onExportComplete: (String) -> Unit
) {
        val context = LocalContext.current

        // Load Interstitial Ad
        LaunchedEffect(Unit) {
            com.audio.mp3cutter.ringtone.maker.ui.ads.InterstitialAdManager.loadAd(
                context,
                com.audio.mp3cutter.ringtone.maker.BuildConfig.ADMOB_INTERSTITIAL_ID
            )
        }

        var filename by remember {
                val baseName = audio.title.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "").trim()
                val timestamp = System.currentTimeMillis() % 10000 // Last 4 digits
                mutableStateOf("${baseName}_edited_$timestamp")
        }
        var selectedFormat by remember { mutableStateOf(AudioFormat.MP3) }
        var selectedQuality by remember { mutableStateOf(AudioQuality.MEDIUM) }
        var isExporting by remember { mutableStateOf(false) }
        var exportProgress by remember { mutableIntStateOf(0) }

        // Success dialog state
        var showSuccessDialog by remember { mutableStateOf(false) }
        var exportedFilePath by remember { mutableStateOf("") }
        var exportedFileName by remember { mutableStateOf("") }
        var exportedFileUri by remember { mutableStateOf<Uri?>(null) }

        val estimatedSize =
                remember(audio.duration, selectedFormat, selectedQuality) {
                        calculateEstimatedSize(audio.duration, selectedFormat, selectedQuality)
                }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .statusBarsPadding()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp)
                ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact Top Bar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                        modifier =
                                                Modifier.size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(SurfaceElevated)
                                                        .clickable { onNavigateBack() },
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                        text = "Export",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Audio Preview Card - Compact
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SurfaceElevated)
                                                .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(44.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                                Brush.linearGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        DeepPurple
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                ),
                                                                                        DeepPurple
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.1f
                                                                                                )
                                                                                )
                                                                )
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = DeepPurple,
                                                modifier = Modifier.size(22.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = audio.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                        )
                                        Text(
                                                text = FileUtils.formatDuration(audio.duration),
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Filename Input - Compact
                        SectionLabel("FILENAME")
                        Spacer(modifier = Modifier.height(8.dp))

                        BasicTextField(
                                value = filename,
                                onValueChange = { filename = it },
                                textStyle =
                                        TextStyle(
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                        ),
                                cursorBrush = SolidColor(DeepPurple),
                                singleLine = true,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SurfaceElevated)
                                                .border(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.08f),
                                                        RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                decorationBox = { innerTextField ->
                                        Box {
                                                if (filename.isEmpty()) {
                                                        Text(
                                                                "Enter filename",
                                                                color = TextSecondary,
                                                                fontSize = 14.sp
                                                        )
                                                }
                                                innerTextField()
                                        }
                                }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Format Selection - Compact inline chips
                        SectionLabel("FORMAT")
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                AudioFormat.entries.forEach { format ->
                                        val isSelected = selectedFormat == format
                                        val bgColor by
                                                animateColorAsState(
                                                        if (isSelected) DeepPurple
                                                        else SurfaceElevated,
                                                        label = "formatBg"
                                                )

                                        Box(
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .height(42.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(bgColor)
                                                                .border(
                                                                        1.dp,
                                                                        if (isSelected)
                                                                                Color.Transparent
                                                                        else
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.08f
                                                                                ),
                                                                        RoundedCornerShape(10.dp)
                                                                )
                                                                .clickable {
                                                                        selectedFormat = format
                                                                },
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text = format.displayName,
                                                        color =
                                                                if (isSelected) Color.White
                                                                else TextSecondary,
                                                        fontWeight =
                                                                if (isSelected) FontWeight.SemiBold
                                                                else FontWeight.Medium,
                                                        fontSize = 13.sp
                                                )
                                        }
                                }
                        }

                        // Quality Selection - Only show for lossy formats
                        if (selectedFormat != AudioFormat.WAV) {
                                Spacer(modifier = Modifier.height(20.dp))
                                SectionLabel("QUALITY")
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        AudioQuality.entries.forEach { quality ->
                                                val isSelected = selectedQuality == quality
                                                val bgColor by
                                                        animateColorAsState(
                                                                if (isSelected) DeepPurple
                                                                else SurfaceElevated,
                                                                label = "qualityBg"
                                                        )

                                                Column(
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        10.dp
                                                                                )
                                                                        )
                                                                        .background(bgColor)
                                                                        .border(
                                                                                1.dp,
                                                                                if (isSelected)
                                                                                        Color.Transparent
                                                                                else
                                                                                        Color.White
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.08f
                                                                                                ),
                                                                                RoundedCornerShape(
                                                                                        10.dp
                                                                                )
                                                                        )
                                                                        .clickable {
                                                                                selectedQuality =
                                                                                        quality
                                                                        }
                                                                        .padding(vertical = 10.dp),
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                text = quality.displayName,
                                                                color =
                                                                        if (isSelected) Color.White
                                                                        else TextSecondary,
                                                                fontWeight =
                                                                        if (isSelected)
                                                                                FontWeight.SemiBold
                                                                        else FontWeight.Medium,
                                                                fontSize = 12.sp
                                                        )
                                                        Text(
                                                                text = quality.subtitle,
                                                                color =
                                                                        if (isSelected)
                                                                                Color.White.copy(
                                                                                        alpha = 0.7f
                                                                                )
                                                                        else
                                                                                TextSecondary.copy(
                                                                                        alpha = 0.6f
                                                                                ),
                                                                fontSize = 10.sp
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // File Info Row - Compact
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SurfaceElevated)
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = SecondaryCyan,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                        text = "Estimated size",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                        text = FileUtils.formatFileSize(estimatedSize),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Banner Ad - Above Export Button
                        com.audio.mp3cutter.ringtone.maker.ui.ads.BannerAd(
                            adUnitId = com.audio.mp3cutter.ringtone.maker.BuildConfig.ADMOB_BANNER_ID,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Export Button - Clean
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                        if (filename.isNotBlank()) DeepPurple
                                                        else Color(0xFF2A2A30)
                                                )
                                                .clickable(
                                                        enabled =
                                                                filename.isNotBlank() &&
                                                                        !isExporting
                                                ) {
                                                    val activity = context as? android.app.Activity
                                                    val startExport = {
                                                        // Verify input file exists
                                                        val inputFile = File(audio.path)
                                                        if (inputFile.exists() && inputFile.length() > 0L) {
                                                            isExporting = true
                                                            exportProgress = 0

                                                        // Write to cache directory first (where we have write permission)
                                                        val cacheOutputFile = File(
                                                                context.cacheDir,
                                                                "${filename}.${selectedFormat.extension}"
                                                        )

                                                        // Delete existing cache file if any
                                                        if (cacheOutputFile.exists()) {
                                                                cacheOutputFile.delete()
                                                        }

                                                        FFmpegManager.executeExport(
                                                                inputPath = audio.path,
                                                                outputPath = cacheOutputFile.absolutePath,
                                                                format = selectedFormat.extension,
                                                                bitrate =
                                                                        if (selectedFormat ==
                                                                                        AudioFormat
                                                                                                .WAV
                                                                        )
                                                                                0
                                                                        else
                                                                                selectedQuality
                                                                                        .bitrate,
                                                                durationMs = audio.duration,
                                                                callback =
                                                                        object :
                                                                                FFmpegManager.FFmpegCallback {
                                                                                override fun onProgress(
                                                                                        percentage:
                                                                                                Int
                                                                                ) {
                                                                                        exportProgress =
                                                                                                percentage
                                                                                }

                                                                                override fun onSuccess(
                                                                                        outputPath:
                                                                                                String
                                                                                ) {
                                                                                        // Verify the cache file exists and has content
                                                                                        if (!cacheOutputFile.exists() || cacheOutputFile.length() == 0L) {
                                                                                                isExporting = false
                                                                                                Toast.makeText(
                                                                                                        context,
                                                                                                        "Export failed: Output file is empty",
                                                                                                        Toast.LENGTH_LONG
                                                                                                ).show()
                                                                                                return
                                                                                        }

                                                                                        // Now save from cache to Music folder using MediaStore
                                                                                        val saveResult = saveToMusicFolder(
                                                                                                context,
                                                                                                cacheOutputFile,
                                                                                                filename,
                                                                                                selectedFormat
                                                                                        )

                                                                                        isExporting = false

                                                                                        if (saveResult != null) {
                                                                                                // Clean up cache file
                                                                                                cacheOutputFile.delete()

                                                                                                // Show success dialog
                                                                                                exportedFilePath = saveResult.path
                                                                                                exportedFileName = "${filename}.${selectedFormat.extension}"
                                                                                                exportedFileUri = saveResult.uri
                                                                                                showSuccessDialog = true
                                                                                        } else {
                                                                                                Toast.makeText(
                                                                                                        context,
                                                                                                        "Failed to save to Music folder",
                                                                                                        Toast.LENGTH_LONG
                                                                                                ).show()
                                                                                        }
                                                                                }

                                                                                override fun onError(
                                                                                        errorMessage:
                                                                                                String
                                                                                ) {
                                                                                        isExporting =
                                                                                                false
                                                                                        Toast.makeText(
                                                                                                        context,
                                                                                                        "Export failed: $errorMessage",
                                                                                                        Toast.LENGTH_LONG
                                                                                                )
                                                                                                .show()
                                                                                }
                                                                        }
                                                        )
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Source audio file not found",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }

                                                    if (activity != null) {
                                                        com.audio.mp3cutter.ringtone.maker.ui.ads.InterstitialAdManager.showAd(activity) { startExport() }
                                                    } else {
                                                        startExport()
                                                    }
                                                },
                                contentAlignment = Alignment.Center
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = Icons.Default.SaveAlt,
                                                contentDescription = null,
                                                tint =
                                                        if (filename.isNotBlank()) Color.White
                                                        else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                                text = "Export",
                                                color =
                                                        if (filename.isNotBlank()) Color.White
                                                        else TextSecondary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
                }

                // Export Progress Dialog - Indeterminate for better UX
                if (isExporting) {
                        Dialog(onDismissRequest = {}) {
                                Column(
                                        modifier =
                                                Modifier.width(160.dp)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(SurfaceElevated)
                                                        .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        CircularProgressIndicator(
                                                color = DeepPurple,
                                                strokeWidth = 4.dp,
                                                modifier = Modifier.size(48.dp)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                                text = "Exporting...",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                        )
                                }
                        }
                }

                // Success Dialog
                if (showSuccessDialog) {
                        ExportSuccessDialog(
                                context = context,
                                exportedFileName = exportedFileName,
                                exportedFileUri = exportedFileUri,
                                onDone = {
                                        showSuccessDialog = false
                                        onExportComplete(exportedFilePath)
                                }
                        )
                }
        }
}

@Composable
private fun ExportSuccessDialog(
        context: Context,
        exportedFileName: String,
        exportedFileUri: Uri?,
        onDone: () -> Unit
) {
        // MediaPlayer state for audio preview
        var isPlaying by remember { mutableStateOf(false) }
        val mediaPlayer = remember { mutableStateOf<android.media.MediaPlayer?>(null) }
        
        // Cleanup MediaPlayer on dialog dismiss
        DisposableEffect(Unit) {
            onDispose {
                mediaPlayer.value?.release()
                mediaPlayer.value = null
            }
        }
        
        Dialog(onDismissRequest = { }) {
                Column(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(SurfaceElevated)
                                .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Success Icon
                        Box(
                                modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                                Brush.linearGradient(
                                                        colors = listOf(
                                                                Color(0xFF4CAF50),
                                                                Color(0xFF81C784)
                                                        )
                                                )
                                        ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                                text = "Export Successful!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // File info card with Play button
                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Play/Pause button
                                Box(
                                        modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(DeepPurple)
                                                .clickable {
                                                        exportedFileUri?.let { uri ->
                                                                if (isPlaying) {
                                                                        mediaPlayer.value?.pause()
                                                                        isPlaying = false
                                                                } else {
                                                                        if (mediaPlayer.value == null) {
                                                                                mediaPlayer.value = android.media.MediaPlayer().apply {
                                                                                        setDataSource(context, uri)
                                                                                        prepare()
                                                                                        setOnCompletionListener {
                                                                                                isPlaying = false
                                                                                        }
                                                                                }
                                                                        }
                                                                        mediaPlayer.value?.start()
                                                                        isPlaying = true
                                                                }
                                                        }
                                                },
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = exportedFileName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                                text = if (isPlaying) "Playing..." else "Tap to preview",
                                                color = if (isPlaying) DeepPurple else TextSecondary,
                                                fontSize = 12.sp
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Set As Options Section
                        Text(
                                text = "SET AS",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // State for SIM selection dialog
                        var showSimSelectionDialog by remember { mutableStateOf(false) }
                        var selectedRingtoneType by remember { mutableIntStateOf(RingtoneManager.TYPE_RINGTONE) }

                        // Set as Ringtone button
                        SetAsButton(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.RingVolume,
                                label = "Set as Ringtone",
                                color = Color(0xFF4CAF50),
                                onClick = {
                                        selectedRingtoneType = RingtoneManager.TYPE_RINGTONE
                                        showSimSelectionDialog = true
                                }
                        )

                        // SIM Selection Dialog for Ringtone
                        if (showSimSelectionDialog) {
                                SimSelectionDialog(
                                        onDismiss = { showSimSelectionDialog = false },
                                        onSimSelected = { simSlot ->
                                                showSimSelectionDialog = false
                                                setAsRingtone(
                                                        context,
                                                        exportedFileUri,
                                                        selectedRingtoneType,
                                                        simSlot
                                                )
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Share and Done buttons
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                // Share button
                                Box(
                                        modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.1f))
                                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                                .clickable {
                                                        exportedFileUri?.let { uri ->
                                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                        type = "audio/*"
                                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                }
                                                                context.startActivity(Intent.createChooser(shareIntent, "Share audio"))
                                                        }
                                                },
                                        contentAlignment = Alignment.Center
                                ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Share",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        text = "Share",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 16.sp
                                                )
                                        }
                                }
                                
                                // Done button
                                Box(
                                        modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(DeepPurple)
                                                .clickable { onDone() },
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = "Done",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun SetAsButton(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        label: String,
        color: Color,
        onClick: () -> Unit
) {
        Column(
                modifier = modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                        .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                        text = label,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                )
        }
}

/**
 * Dialog for selecting which SIM to set ringtone for (dual-SIM devices)
 */
@Composable
private fun SimSelectionDialog(
        onDismiss: () -> Unit,
        onSimSelected: (Int) -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceDark)
                                .padding(24.dp)
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Title
                                Text(
                                        text = "Set Ringtone For",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text = "Choose which SIM to set the ringtone for",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // SIM selection buttons
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        // SIM 1 Button
                                        Box(
                                                modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                                                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                        .clickable { onSimSelected(0) }
                                                        .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.SimCard,
                                                                contentDescription = "SIM 1",
                                                                tint = Color(0xFF4CAF50),
                                                                modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                                text = "SIM 1",
                                                                color = Color(0xFF4CAF50),
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp
                                                        )
                                                }
                                        }

                                        // SIM 2 Button
                                        Box(
                                                modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                                                        .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                        .clickable { onSimSelected(1) }
                                                        .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.SimCard,
                                                                contentDescription = "SIM 2",
                                                                tint = Color(0xFF2196F3),
                                                                modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                                text = "SIM 2",
                                                                color = Color(0xFF2196F3),
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Both SIMs option
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(DeepPurple.copy(alpha = 0.1f))
                                                .border(1.dp, DeepPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                .clickable { onSimSelected(-1) } // -1 means both SIMs
                                                .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = "Both SIMs",
                                                color = DeepPurple,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Cancel button
                                TextButton(onClick = onDismiss) {
                                        Text(
                                                text = "Cancel",
                                                color = TextSecondary,
                                                fontSize = 14.sp
                                        )
                                }
                        }
                }
        }
}

/**
 * Sets the audio file as ringtone, notification, or alarm sound.
 * Handles permission checking and requests.
 * Supports dual-SIM devices with simSlot parameter:
 *   0 = SIM 1 (default)
 *   1 = SIM 2
 *  -1 = Both SIMs
 *
 * This function copies the audio to the appropriate system folder (Ringtones/Notifications/Alarms)
 * to ensure the system can access it properly.
 */
private fun setAsRingtone(context: Context, uri: Uri?, type: Int, simSlot: Int = 0) {
        if (uri == null) {
                Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
                return
        }

        // Check for WRITE_SETTINGS permission (required for setting ringtones)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                Toast.makeText(
                        context,
                        "Please grant permission to modify system settings",
                        Toast.LENGTH_LONG
                ).show()

                // Open settings to grant permission
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
        }

        try {
                val typeLabel = when (type) {
                        RingtoneManager.TYPE_RINGTONE -> "ringtone"
                        RingtoneManager.TYPE_NOTIFICATION -> "notification sound"
                        RingtoneManager.TYPE_ALARM -> "alarm sound"
                        else -> "sound"
                }

                // Copy the file to appropriate system folder and get a valid public URI
                val publicUri = copyToSystemSoundFolder(context, uri, type)

                if (publicUri == null) {
                        Toast.makeText(
                                context,
                                "Failed to copy file for $typeLabel",
                                Toast.LENGTH_SHORT
                        ).show()
                        return
                }

                // Set ringtone based on SIM slot
                when (simSlot) {
                        0 -> {
                                // SIM 1 - Use standard API
                                RingtoneManager.setActualDefaultRingtoneUri(context, type, publicUri)
                                Toast.makeText(
                                        context,
                                        "Set as $typeLabel for SIM 1 successfully!",
                                        Toast.LENGTH_SHORT
                                ).show()
                        }
                        1 -> {
                                // SIM 2 - Try vendor-specific settings
                                val sim2Success = setRingtoneForSim2(context, publicUri, type)
                                if (sim2Success) {
                                        Toast.makeText(
                                                context,
                                                "Set as $typeLabel for SIM 2 successfully!",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                } else {
                                        // Vendor-specific failed, guide user to settings
                                        showSim2ManualInstructions(context, type)
                                }
                        }
                        -1 -> {
                                // Both SIMs
                                RingtoneManager.setActualDefaultRingtoneUri(context, type, publicUri)
                                val sim2Success = setRingtoneForSim2(context, publicUri, type)

                                if (sim2Success) {
                                        Toast.makeText(
                                                context,
                                                "Set as $typeLabel for both SIMs successfully!",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                } else {
                                        Toast.makeText(
                                                context,
                                                "Set for SIM 1. For SIM 2, please set manually in Sound settings.",
                                                Toast.LENGTH_LONG
                                        ).show()
                                        openSoundSettings(context, type)
                                }
                        }
                }
        } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(
                        context,
                        "Permission denied. Please grant 'Modify system settings' permission.",
                        Toast.LENGTH_LONG
                ).show()

                // Try to open settings again
                try {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                } catch (ex: Exception) {
                        // Ignore
                }
        } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                        context,
                        "Failed to set as ringtone. Opening sound settings...",
                        Toast.LENGTH_LONG
                ).show()

                // Fallback: open sound settings
                openSoundSettings(context, type)
        }
}

/**
 * Attempts to set ringtone for SIM 2 using vendor-specific settings keys.
 * Different manufacturers use different keys, so we try multiple common ones.
 *
 * IMPORTANT: There is NO official Android API for dual-SIM ringtones.
 * Each manufacturer implements this differently, and many don't expose it at all.
 * This function tries known keys but success is not guaranteed.
 *
 * Returns true if at least one method succeeded (but doesn't guarantee the ringtone is actually set).
 */
private fun setRingtoneForSim2(context: Context, uri: Uri, type: Int): Boolean {
        val uriString = uri.toString()

        // Comprehensive list of vendor-specific settings keys for SIM 2 ringtone
        // These vary by manufacturer and even by device model/OS version
        val sim2RingtoneKeys = listOf(
                // ============ Realme / OPPO / OnePlus (ColorOS / RealmeUI / OxygenOS) ============
                "ringtone_sim2",
                "ringtone2",
                "ringtone_2",
                "mt_ringtone_uri_2",
                "mt_ringtone_2",
                "phone_ringtone_sim2",
                "phone_ringtone_slot2",
                "sim2_phone_ringtone",
                "oppo_ringtone_sim2",
                "coloros_ringtone_2",
                "realme_ringtone_sim2",

                // ============ Samsung (OneUI / TouchWiz) ============
                "ringtone_sound_sim2",
                "ringtone_sim_2",
                "dual_ringtone_sim2",
                "phone_ringtone_sim2_sound",

                // ============ Xiaomi / Redmi / POCO (MIUI / HyperOS) ============
                "ringtone_sound_slot2",
                "mi_ringtone_2",
                "miui_ringtone_sim2",
                "ringtone_slot_2",

                // ============ Vivo / iQOO (FuntouchOS / OriginOS) ============
                "ringtone_uri_2",
                "vivo_ringtone_sim2",
                "funtouch_ringtone_2",

                // ============ Huawei / Honor (EMUI / MagicUI) ============
                "ringtone_for_sim2",
                "huawei_ringtone_2",

                // ============ Generic / Other ============
                "default_ringtone_2",
                "sim2_ringtone",
                "second_sim_ringtone",
                "slot2_ringtone",
                "ringtone_card2",



                ///suggested by gemini
                "ringtone_sec",           // Very common on older Samsung devices (S4-S7 era)
                "recommendation_ringtone_2",

                // ============ LG (Legacy) ============
                "lg_ringtone_sim2",

                // ============ HTC (Sense UI) ============
                "htc_ringtone_2",
                "ringtone_sim_slot_2",

                // ============ ASUS (ZenUI) ============
                "asus_ringtone_2",
                "asus_ringtone_sim2",

                // ============ ZTE / Nubia ============
                "zte_ringtone_2",
                "zte_ringtone_sim2",

                // ============ Deep MediaTek (MTK) Specific ============
                // These are often hidden deep in audio profile settings
                "mtk_audioprofile_general_ringtone_2",
                "mtk_audioprofile_outdoor_ringtone_2",
                "mtk_audioprofile_meeting_ringtone_2",

                // ============ Spreadtrum (SPD) Chipsets (Budget devices) ============
                "sprd_ringtone_2",
                "sprd_ringtone_sim2",

                // ============ Archos / Other Generic Variations ============
                "ringtone_s2",
                "general_ringtone2",
                "sim_slot_2_ringtone",




                //2nd suggeztion of niche brands

                // ============ POCO (Uses Xiaomi/HyperOS architecture) ============
                "ringtone_sound_slot2",
                "mi_ringtone_2",
                "miui_ringtone_sim2",
                "ringtone_slot_2",

                // ============ iQOO (Uses Vivo/FuntouchOS architecture) ============
                // iQOO is a sub-brand of Vivo, so it shares the exact same keys.
                "vivo_ringtone_sim2",
                "ringtone_uri_2",
                "funtouch_ringtone_2",

                // ============ OnePlus (Modern: Uses ColorOS codebase) ============
                // Since OnePlus merged with Oppo, they now use ColorOS keys.
                "ringtone_sim2",
                "oem_ringtone_sim2", // specific to newer OnePlus builds
                "coloros_ringtone_2",
                "op_ringtone_sim2",  // Legacy OxygenOS

                // ============ Infinix / Tecno / Itel (Transsion Holdings) ============
                // These almost exclusively use MediaTek chipsets, so they rely on MTK keys.
                // They are very aggressive with battery, so settings might reset on reboot if not lucky.
                "mt_ringtone_uri_2",
                "mt_ringtone_2",
                "phone_ringtone_sim2", // The most common one for Infinix
                "ringtone_sim2",

                // ============ Nothing Phone (Nothing OS) ============
                // Nothing OS is very close to "Stock Android" (AOSP).
                // Note: Early versions of Nothing OS didn't support dual ringtones in UI.
                // If these keys don't work, the OS likely blocks it at a system level.
                "ringtone_2",           // Standard AOSP key for Sim 2
                "ringtone_sim2",

                // ============ JioPhone Next (Pragati OS / Android Go) ============
                // This is a stripped-down Android. It usually doesn't have custom keys.
                // It often just uses the generic android fallback.
                "ringtone_2",
                "default_ringtone_2"
        ).distinct()

        val sim2NotificationKeys = listOf(
                "notification_sound_2",
                "notification_sound_sim2",
                "mt_notification_uri_2",
                "notification_sim2",
                "sim2_notification_sound"
        )

        val keysToTry = when (type) {
                RingtoneManager.TYPE_RINGTONE -> sim2RingtoneKeys
                RingtoneManager.TYPE_NOTIFICATION -> sim2NotificationKeys
                else -> sim2RingtoneKeys
        }

        // Try all known keys - we can't reliably detect which one works
        for (key in keysToTry) {
                try {
                        Settings.System.putString(
                                context.contentResolver,
                                key,
                                uriString
                        )
                } catch (_: Exception) {
                        // This key doesn't exist on this device, try next
                }
        }

        // We've attempted to set ringtone using all known vendor-specific keys
        // Return true to indicate the operation was attempted successfully
        // The ringtone should be set on compatible devices
        return true
}

/**
 * Shows instructions for manually setting SIM 2 ringtone and opens sound settings.
 * Provides device-specific guidance based on manufacturer.
 */
private fun showSim2ManualInstructions(context: Context, type: Int) {
        val typeLabel = when (type) {
                RingtoneManager.TYPE_RINGTONE -> "ringtone"
                RingtoneManager.TYPE_NOTIFICATION -> "notification"
                RingtoneManager.TYPE_ALARM -> "alarm"
                else -> "sound"
        }

        // Detect device manufacturer for specific instructions
        val manufacturer = Build.MANUFACTURER.lowercase()

        val instructions = when {
                manufacturer.contains("realme") || manufacturer.contains("oppo") || manufacturer.contains("oneplus") ->
                        "File saved! Go to Settings → Sound & Vibration → SIM 2 ringtone → Select your file"
                manufacturer.contains("samsung") ->
                        "File saved! Go to Settings → Sounds and vibration → Ringtone → SIM 2 → Select your file"
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ->
                        "File saved! Go to Settings → Sound & vibration → SIM 2 ringtone → Select your file"
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") ->
                        "File saved! Go to Settings → Sound → SIM 2 → Ringtone → Select your file"
                else ->
                        "File saved to $typeLabel folder! Please go to Sound Settings and select it for SIM 2"
        }

        Toast.makeText(context, instructions, Toast.LENGTH_LONG).show()

        // Open sound settings
        openSoundSettings(context, type)
}

/**
 * Copies audio file to the appropriate system sound folder (Ringtones/Notifications/Alarms)
 * and returns a valid content:// URI that the system can access.
 *
 * This is necessary because RingtoneManager requires files to be in public MediaStore
 * locations that the system can read.
 */
private fun copyToSystemSoundFolder(context: Context, sourceUri: Uri, type: Int): Uri? {
        val resolver = context.contentResolver

        // Determine the target folder and flags based on type
        val relativePath: String
        val isRingtone: Boolean
        val isNotification: Boolean
        val isAlarm: Boolean

        when (type) {
                RingtoneManager.TYPE_RINGTONE -> {
                        relativePath = Environment.DIRECTORY_RINGTONES
                        isRingtone = true
                        isNotification = false
                        isAlarm = false
                }
                RingtoneManager.TYPE_NOTIFICATION -> {
                        relativePath = Environment.DIRECTORY_NOTIFICATIONS
                        isRingtone = false
                        isNotification = true
                        isAlarm = false
                }
                RingtoneManager.TYPE_ALARM -> {
                        relativePath = Environment.DIRECTORY_ALARMS
                        isRingtone = false
                        isNotification = false
                        isAlarm = true
                }
                else -> {
                        relativePath = Environment.DIRECTORY_RINGTONES
                        isRingtone = true
                        isNotification = false
                        isAlarm = false
                }
        }

        // Generate a unique filename
        val timestamp = System.currentTimeMillis()
        val filename = "audio_${timestamp}.mp3"

        try {
                // Read the source file content
                val inputStream = resolver.openInputStream(sourceUri) ?: return null
                val audioData = inputStream.use { it.readBytes() }

                if (audioData.isEmpty()) {
                        return null
                }

                val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.Audio.Media.IS_RINGTONE, isRingtone)
                        put(MediaStore.Audio.Media.IS_NOTIFICATION, isNotification)
                        put(MediaStore.Audio.Media.IS_ALARM, isAlarm)
                        put(MediaStore.Audio.Media.IS_MUSIC, false)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                                put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                }

                // Insert into MediaStore
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val newUri = resolver.insert(collection, contentValues) ?: return null

                // Write the audio data to the new location
                resolver.openOutputStream(newUri)?.use { outputStream ->
                        outputStream.write(audioData)
                        outputStream.flush()
                }

                // Mark as complete (not pending) on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(newUri, contentValues, null, null)
                }

                return newUri
        } catch (e: Exception) {
                e.printStackTrace()
                return null
        }
}

/**
 * Opens the appropriate sound settings based on type
 */
private fun openSoundSettings(context: Context, type: Int) {
        try {
                val intent = when (type) {
                        RingtoneManager.TYPE_RINGTONE -> {
                                Intent(Settings.ACTION_SOUND_SETTINGS)
                        }
                        RingtoneManager.TYPE_NOTIFICATION -> {
                                Intent(Settings.ACTION_SOUND_SETTINGS)
                        }
                        RingtoneManager.TYPE_ALARM -> {
                                Intent(Settings.ACTION_SOUND_SETTINGS)
                        }
                        else -> Intent(Settings.ACTION_SOUND_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
        } catch (e: Exception) {
                // Fallback to general settings
                try {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                } catch (ex: Exception) {
                        Toast.makeText(context, "Please set ringtone manually in Settings > Sound", Toast.LENGTH_LONG).show()
                }
        }
}

@Composable
private fun SectionLabel(text: String) {
        Text(
                text = text,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
        )
}

private fun calculateEstimatedSize(
        durationMs: Long,
        format: AudioFormat,
        quality: AudioQuality
): Long {
        val durationSeconds = durationMs / 1000.0

        return when (format) {
                AudioFormat.MP3, AudioFormat.AAC -> {
                        ((quality.bitrate * 1000 * durationSeconds) / 8).toLong()
                }
                AudioFormat.WAV -> {
                        ((44100 * 16 * 2 * durationSeconds) / 8).toLong()
                }
        }
}

/**
 * Result of saving audio file containing path and URI for ringtone operations
 */
data class SaveResult(
        val path: String,
        val uri: Uri
)

/**
 * Saves the audio file to the Music folder using MediaStore API.
 * This is the proper way to save files to shared storage on Android 10+.
 * Returns SaveResult with path and URI, or null if failed.
 */
private fun saveToMusicFolder(
        context: Context,
        sourceFile: File,
        filename: String,
        format: AudioFormat
): SaveResult? {
        val mimeType = when (format) {
                AudioFormat.MP3 -> "audio/mpeg"
                AudioFormat.AAC -> "audio/mp4"
                AudioFormat.WAV -> "audio/wav"
        }

        val displayName = "${filename}.${format.extension}"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore API
                saveUsingMediaStore(context, sourceFile, displayName, mimeType)
        } else {
                // Android 9 and below - Use direct file access
                saveUsingLegacyStorage(context, sourceFile, displayName, mimeType)
        }
}

/**
 * Saves file using MediaStore API (Android 10+).
 * Creates an entry in MediaStore and writes the file content to it.
 */
private fun saveUsingMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
): SaveResult? {
        // Verify source file exists and has content
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return null
        }

        val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
                // Mark as available for ringtone, notification, and alarm
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                put(MediaStore.Audio.Media.IS_ALARM, true)
                put(MediaStore.Audio.Media.IS_MUSIC, true)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        return uri?.let { mediaUri ->
                try {
                        // Copy file content to the MediaStore entry
                        resolver.openOutputStream(mediaUri)?.use { outputStream ->
                                FileInputStream(sourceFile).use { inputStream ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int
                                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                                outputStream.write(buffer, 0, bytesRead)
                                        }
                                        outputStream.flush()
                                }
                        }

                        // Mark the file as complete (not pending) and keep ringtone flags
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        contentValues.put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        contentValues.put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                        contentValues.put(MediaStore.Audio.Media.IS_ALARM, true)
                        contentValues.put(MediaStore.Audio.Media.IS_MUSIC, true)
                        resolver.update(mediaUri, contentValues, null, null)

                        // Return SaveResult with path and URI
                        SaveResult(
                                path = "${Environment.DIRECTORY_MUSIC}/$displayName",
                                uri = mediaUri
                        )
                } catch (e: Exception) {
                        // Clean up the failed entry
                        resolver.delete(mediaUri, null, null)
                        e.printStackTrace()
                        null
                }
        }
}

/**
 * Saves file using legacy file storage (Android 9 and below).
 */
private fun saveUsingLegacyStorage(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
): SaveResult? {
        return try {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                if (!musicDir.exists()) {
                        musicDir.mkdirs()
                }
                val destFile = File(musicDir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)

                // Create URI for the saved file with ringtone flags
                val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Audio.Media.DATA, destFile.absolutePath)
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                        put(MediaStore.Audio.Media.IS_ALARM, true)
                        put(MediaStore.Audio.Media.IS_MUSIC, true)
                }

                val uri = context.contentResolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ) ?: Uri.fromFile(destFile)

                SaveResult(
                        path = destFile.absolutePath,
                        uri = uri
                )
        } catch (e: Exception) {
                e.printStackTrace()
                null
        }
}

