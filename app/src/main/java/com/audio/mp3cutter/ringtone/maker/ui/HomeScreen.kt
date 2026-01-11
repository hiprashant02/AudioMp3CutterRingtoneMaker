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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audio.mp3cutter.ringtone.maker.ui.theme.*

@Composable
fun HomeScreen(
    onOpenAudio: () -> Unit,
    onRecordAudio: () -> Unit,
    onCutAudio: () -> Unit,
    onMergeAudio: () -> Unit,
    onConvertAudio: () -> Unit
) {
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
            
            // Header (Clean White)
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Audio Studio",
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
                    
                    // Grid Layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModernToolCard(
                            title = "Cutter",
                            subtitle = "Trim Audio",
                            icon = Icons.Default.ContentCut,
                            iconGradient = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)), // Violet -> Pink
                            onClick = onCutAudio,
                            modifier = Modifier.weight(1f)
                        )
                        ModernToolCard(
                            title = "Merger",
                            subtitle = "Join Clips",
                            icon = Icons.Rounded.GraphicEq,
                            iconGradient = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)), // Cyan -> Blue
                            onClick = onMergeAudio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModernToolCard(
                            title = "Recorder",
                            subtitle = "Record Voice",
                            icon = Icons.Default.Mic,
                            iconGradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), // Pink -> Red
                            onClick = onRecordAudio,
                            modifier = Modifier.weight(1f)
                        )
                        ModernToolCard(
                            title = "Converter",
                            subtitle = "Change Format",
                            icon = Icons.Default.Transform,
                            iconGradient = listOf(Color(0xFFF59E0B), Color(0xFFF97316)), // Amber -> Orange
                            onClick = onConvertAudio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Recents (Modern)
             item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                         text = "RECENT PROJECTS",
                         style = MaterialTheme.typography.labelMedium,
                         color = TextSecondary,
                         letterSpacing = 1.5.sp,
                         fontWeight = FontWeight.Bold
                    )
                     Spacer(modifier = Modifier.height(16.dp))
                     
                     // Playful Empty State
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(100.dp)
                             .clip(RoundedCornerShape(24.dp))
                             .background(Color(0xFF161618)) // Slightly darker than cards
                             .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                             .clickable { },
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
                }
             }
        }
    }
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
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(24.dp))
            .background(MatteSurface)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Gradient Icon Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(iconGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
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
                .size(16.dp)
        )
    }
}
