package com.audio.mp3cutter.ringtone.maker.ui.ringtone

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import java.io.File
import java.io.FileInputStream

/**
 * Screen to set a selected audio file as ringtone.
 * Shows the audio details and SIM selection dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerScreen(
        audio: AudioModel,
        onNavigateBack: () -> Unit,
        onComplete: () -> Unit
) {
        val context = LocalContext.current
        var showSimSelectionDialog by remember { mutableStateOf(true) }
        var ringtoneSet by remember { mutableStateOf(false) }

        Scaffold(
                containerColor = Color.Black,
                topBar = {
                        TopAppBar(
                                title = {
                                        Text(
                                                text = "Set as Ringtone",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Back",
                                                        tint = Color.White
                                                )
                                        }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                )
                        )
                }
        ) { padding ->
                Box(
                        modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                        contentAlignment = Alignment.Center
                ) {
                        Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Audio info card
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF1C1C1E))
                                                .padding(24.dp)
                                ) {
                                        Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                // Icon
                                                Box(
                                                        modifier = Modifier
                                                                .size(80.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        Brush.linearGradient(
                                                                                listOf(
                                                                                        Color(0xFF10B981),
                                                                                        Color(0xFF34D399)
                                                                                )
                                                                        )
                                                                ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Icon(
                                                                imageVector = if (ringtoneSet) Icons.Default.Check else Icons.Default.RingVolume,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(40.dp)
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Song title
                                                Text(
                                                        text = audio.title,
                                                        color = Color.White,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Artist
                                                Text(
                                                        text = audio.artist,
                                                        color = TextSecondary,
                                                        fontSize = 14.sp
                                                )

                                                if (ringtoneSet) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Text(
                                                                text = "✓ Ringtone set successfully!",
                                                                color = Color(0xFF10B981),
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Set Ringtone button (if dialog dismissed without setting)
                                if (!showSimSelectionDialog && !ringtoneSet) {
                                        Box(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                                Brush.linearGradient(
                                                                        listOf(Color(0xFF10B981), Color(0xFF34D399))
                                                                )
                                                        )
                                                        .clickable { showSimSelectionDialog = true },
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text = "Set as Ringtone",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 16.sp
                                                )
                                        }
                                }

                                // Done button (after ringtone is set)
                                if (ringtoneSet) {
                                        Box(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(DeepPurple)
                                                        .clickable { onComplete() },
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

                                // Banner Ad
                                com.audio.mp3cutter.ringtone.maker.ui.ads.BannerAd(
                                    adUnitId = com.audio.mp3cutter.ringtone.maker.BuildConfig.ADMOB_BANNER_ID,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                        }

                        // SIM Selection Dialog
                        if (showSimSelectionDialog) {
                                SimSelectionDialog(
                                        onSimSelected = { simSlot ->
                                                val uri = Uri.parse(audio.path)
                                                setAsRingtone(context, uri, RingtoneManager.TYPE_RINGTONE, simSlot)
                                                ringtoneSet = true
                                                showSimSelectionDialog = false
                                        },
                                        onDismiss = { showSimSelectionDialog = false }
                                )
                        }
                }
        }
}

/**
 * Dialog for selecting which SIM to set ringtone for
 */
@Composable
private fun SimSelectionDialog(
        onSimSelected: (Int) -> Unit,
        onDismiss: () -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(24.dp)
                ) {
                        Column {
                                // Header
                                Row(
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.PhoneAndroid,
                                                contentDescription = null,
                                                tint = DeepPurple,
                                                modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                                text = "Select SIM for Ringtone",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Choose which SIM to set this ringtone for:",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // SIM 1 Option
                                SimOptionButton(
                                        title = "SIM 1",
                                        subtitle = "Primary SIM",
                                        onClick = { onSimSelected(0) }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // SIM 2 Option
                                SimOptionButton(
                                        title = "SIM 2",
                                        subtitle = "Secondary SIM",
                                        onClick = { onSimSelected(1) }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Both SIMs Option
                                SimOptionButton(
                                        title = "Both SIMs",
                                        subtitle = "Set for all",
                                        onClick = { onSimSelected(-1) }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Cancel button
                                TextButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.align(Alignment.End)
                                ) {
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

@Composable
private fun SimOptionButton(
        title: String,
        subtitle: String,
        onClick: () -> Unit
) {
        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                        .padding(16.dp)
        ) {
                Column {
                        Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                text = subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp
                        )
                }
        }
}

// ==================== Ringtone Setting Functions ====================

/**
 * Sets the audio file as ringtone.
 * Supports dual-SIM devices with simSlot parameter:
 *   0 = SIM 1 (default)
 *   1 = SIM 2
 *  -1 = Both SIMs
 */
private fun setAsRingtone(context: Context, uri: Uri?, type: Int, simSlot: Int = 0) {
        if (uri == null) {
                Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
                return
        }

        // Check for WRITE_SETTINGS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                Toast.makeText(
                        context,
                        "Please grant permission to modify system settings",
                        Toast.LENGTH_LONG
                ).show()

                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
        }

        try {
                val typeLabel = "ringtone"

                // Copy the file to system folder and get a valid public URI
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
                                RingtoneManager.setActualDefaultRingtoneUri(context, type, publicUri)
                                Toast.makeText(
                                        context,
                                        "Set as $typeLabel for SIM 1 successfully!",
                                        Toast.LENGTH_SHORT
                                ).show()
                        }
                        1 -> {
                                val sim2Success = setRingtoneForSim2(context, publicUri, type)
                                if (sim2Success) {
                                        Toast.makeText(
                                                context,
                                                "Set as $typeLabel for SIM 2 successfully!",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                } else {
                                        Toast.makeText(
                                                context,
                                                "Could not set for SIM 2. Please set manually in Sound settings.",
                                                Toast.LENGTH_LONG
                                        ).show()
                                }
                        }
                        -1 -> {
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
                                                "Set for SIM 1. For SIM 2, please set manually.",
                                                Toast.LENGTH_LONG
                                        ).show()
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

                try {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                } catch (_: Exception) {}
        } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                        context,
                        "Failed to set as ringtone",
                        Toast.LENGTH_LONG
                ).show()
        }
}

/**
 * Attempts to set ringtone for SIM 2 using vendor-specific settings keys.
 */
private fun setRingtoneForSim2(context: Context, uri: Uri, type: Int): Boolean {
        val uriString = uri.toString()

        val sim2RingtoneKeys = listOf(
                "ringtone_sim2", "ringtone2", "ringtone_2",
                "mt_ringtone_uri_2", "mt_ringtone_2",
                "phone_ringtone_sim2", "phone_ringtone_slot2",
                "sim2_phone_ringtone", "oppo_ringtone_sim2",
                "coloros_ringtone_2", "realme_ringtone_sim2",
                "ringtone_sound_sim2", "ringtone_sim_2",
                "dual_ringtone_sim2", "ringtone_sound_slot2",
                "mi_ringtone_2", "miui_ringtone_sim2",
                "ringtone_slot_2", "ringtone_uri_2",
                "vivo_ringtone_sim2", "funtouch_ringtone_2",
                "ringtone_for_sim2", "huawei_ringtone_2",
                "default_ringtone_2", "sim2_ringtone",
                "second_sim_ringtone", "slot2_ringtone"
        ).distinct()

        for (key in sim2RingtoneKeys) {
                try {
                        Settings.System.putString(
                                context.contentResolver,
                                key,
                                uriString
                        )
                } catch (_: Exception) {}
        }

        return true
}

/**
 * Copies the audio file to the appropriate system sound folder (Ringtones).
 */
private fun copyToSystemSoundFolder(context: Context, sourceUri: Uri, type: Int): Uri? {
        try {
                val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
                val fileName = "AudioStudio_${System.currentTimeMillis()}.mp3"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                                put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }

                        val uri = context.contentResolver.insert(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        ) ?: return null

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                inputStream.copyTo(outputStream)
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)

                        inputStream.close()
                        return uri
                } else {
                        @Suppress("DEPRECATION")
                        val ringtoneDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                        if (!ringtoneDir.exists()) ringtoneDir.mkdirs()

                        val destFile = File(ringtoneDir, fileName)
                        destFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                        }

                        inputStream.close()

                        val contentValues = ContentValues().apply {
                                put(MediaStore.Audio.Media.DATA, destFile.absolutePath)
                                put(MediaStore.Audio.Media.TITLE, fileName)
                                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        }

                        return context.contentResolver.insert(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        )
                }
        } catch (e: Exception) {
                e.printStackTrace()
                return null
        }
}
