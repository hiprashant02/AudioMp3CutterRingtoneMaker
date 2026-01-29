package com.audio.mp3cutter.ringtone.maker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.ui.theme.*

@Composable
fun HomeScreen(
    onOpenAudio: () -> Unit,
    onRecordAudio: () -> Unit,
    onCutAudio: () -> Unit,
    onMergeAudio: () -> Unit,
    onConvertAudio: () -> Unit,
    onSetRingtone: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onProjectClick: (AudioModel) -> Unit = {},
    onSeeAllProjects: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        containerColor = Color.Black // Pure Black Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            
            // Header with About icon
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audio Editor",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Professional Tools",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                    
                    // About icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MatteSurface)
                            .clickable { onAboutClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Solid Gradient Hero Card
            item {
                SolidHeroCard(onClick = onOpenAudio)
            }

            // Tools Section
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                         text = "TOOLS",
                         style = MaterialTheme.typography.labelMedium,
                         color = TextSecondary,
                         letterSpacing = 1.5.sp,
                         fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Row 1: Cutter, Merger, Recorder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernToolCard(
                            title = "Cutter",
                            subtitle = "Trim",
                            icon = Icons.Default.ContentCut,
                            iconGradient = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
                            onClick = onCutAudio,
                            modifier = Modifier.weight(1f)
                        )
                        ModernToolCard(
                            title = "Merger",
                            subtitle = "Join",
                            icon = Icons.Rounded.GraphicEq,
                            iconGradient = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
                            onClick = onMergeAudio,
                            modifier = Modifier.weight(1f)
                        )
                        ModernToolCard(
                            title = "Recorder",
                            subtitle = "Voice",
                            icon = Icons.Default.Mic,
                            iconGradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
                            onClick = onRecordAudio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Row 2: Converter, Ringtone, Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernToolCard(
                            title = "Convert",
                            subtitle = "Format",
                            icon = Icons.Default.Transform,
                            iconGradient = listOf(Color(0xFFF59E0B), Color(0xFFF97316)),
                            onClick = onConvertAudio,
                            modifier = Modifier.weight(1f)
                        )
                        ModernToolCard(
                            title = "Ringtone",
                            subtitle = "Set",
                            icon = Icons.Default.RingVolume,
                            iconGradient = listOf(Color(0xFF10B981), Color(0xFF34D399)),
                            onClick = onSetRingtone,
                            modifier = Modifier.weight(1f)
                        )
                        ModernToolCard(
                            title = "Share",
                            subtitle = "App",
                            icon = Icons.Default.Share,
                            iconGradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                            onClick = onShareApp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Row 3: Rate Us (centered)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        ModernToolCard(
                            title = "Rate Us",
                            subtitle = "🤍",
                            icon = Icons.Default.Star,
                            iconGradient = listOf(Color(0xFFEAB308), Color(0xFFFACC15)),
                            onClick = onRateApp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            // Recents Section
             item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                             text = "RECENT PROJECTS",
                             style = MaterialTheme.typography.labelMedium,
                             color = TextSecondary,
                             letterSpacing = 1.5.sp,
                             fontWeight = FontWeight.Bold
                        )
                        if (uiState.recentProjects.isNotEmpty()) {
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepPurple,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onSeeAllProjects() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                     
                    if (uiState.isLoading) {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = DeepPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (uiState.recentProjects.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF161618))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                                .clickable { onOpenAudio() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No projects yet. Start creating!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        // Recent Projects List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.recentProjects.take(5).forEach { project ->
                                RecentProjectItem(
                                    audio = project,
                                    onClick = { onProjectClick(project) }
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
private fun RecentProjectItem(
    audio: AudioModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Title & Duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDuration(audio.duration),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        
        // Arrow
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun SolidHeroCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DeepPurple, ElectricBlue) // Rich Solid Gradient
                )
            )
            .clickable { onClick() }
            .padding(24.dp)
    ) {
         // Subtle Decoration
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(100.dp)
                .offset(x = 20.dp, y = (-20).dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Default.Add,
                     contentDescription = null,
                     tint = Color.White,
                     modifier = Modifier.size(24.dp)
                 )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "New Project",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to open audio file",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Icon(
                     imageVector = Icons.Default.ArrowForward,
                     contentDescription = null,
                     tint = Color.White,
                     modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ModernToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconGradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(0.95f)
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .clickable { onClick() }
            .padding(12.dp) // Reduced padding
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Gradient Icon Box
            Box(
                modifier = Modifier
                    .size(36.dp) // Reduced from 42.dp
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(iconGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp) // Reduced from 22.dp
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall, // Smaller style
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, // Explicit smaller size
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp, // Explicit smaller size
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Subtle Arrow Top Right
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.1f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(14.dp) // Reduced size
        )
    }
}
