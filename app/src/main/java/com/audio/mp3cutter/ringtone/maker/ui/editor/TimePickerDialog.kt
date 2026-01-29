package com.audio.mp3cutter.ringtone.maker.ui.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.audio.mp3cutter.ringtone.maker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TimePickerDialog(
        title: String,
        initialTimeMs: Long,
        maxTimeMs: Long,
        minTimeMs: Long = 0L,
        accentColor: Color = DeepPurple,
        onDismiss: () -> Unit,
        onConfirm: (Long) -> Unit
) {
    // Parse initial time
    val initialMinutes = ((initialTimeMs / 1000) / 60).toInt()
    val initialSeconds = ((initialTimeMs / 1000) % 60).toInt()
    val initialDeciseconds = ((initialTimeMs % 1000) / 100).toInt()

    var minutes by remember { mutableIntStateOf(initialMinutes) }
    var seconds by remember { mutableIntStateOf(initialSeconds) }
    var deciseconds by remember { mutableIntStateOf(initialDeciseconds) }
    var isKeyboardMode by remember { mutableStateOf(false) }

    // Calculate max values
    val maxMinutes = ((maxTimeMs / 1000) / 60).toInt()

    // Calculate current time in ms
    val currentTimeMs =
            remember(minutes, seconds, deciseconds) {
                (minutes * 60 * 1000L) + (seconds * 1000L) + (deciseconds * 100L)
            }

    Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                        Color(0xFF1C1C1E)
                                ) // Refined Dark Background (iOS style)
        ) {
            Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                    )

                    // Keyboard toggle: Subtle
                    IconButton(
                            onClick = { isKeyboardMode = !isKeyboardMode },
                            modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Toggle Input",
                                tint =
                                        if (isKeyboardMode) accentColor
                                        else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isKeyboardMode) {
                    KeyboardTimeInput(
                            minutes = minutes,
                            seconds = seconds,
                            deciseconds = deciseconds,
                            accentColor = accentColor,
                            onMinutesChange = { minutes = it.coerceIn(0, maxMinutes) },
                            onSecondsChange = { seconds = it.coerceIn(0, 59) },
                            onDecisecondsChange = { deciseconds = it.coerceIn(0, 9) }
                    )
                } else {
                    WheelTimePickerContent(
                            minutes = minutes,
                            seconds = seconds,
                            deciseconds = deciseconds,
                            maxMinutes = maxMinutes,
                            accentColor = accentColor,
                            onMinutesChange = { minutes = it },
                            onSecondsChange = { seconds = it },
                            onDecisecondsChange = { deciseconds = it }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White.copy(alpha = 0.6f),
                                            containerColor = Color.Transparent
                                    ),
                            border =
                                    androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color.White.copy(alpha = 0.15f)
                                    )
                    ) { Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium) }

                    // Confirm button - Clean and Solid
                    Button(
                            onClick = {
                                val finalTimeMs = currentTimeMs.coerceIn(minTimeMs, maxTimeMs)
                                onConfirm(finalTimeMs)
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = Color.White
                                    ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) { Text("Set Time", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                }
            }
        }
    }
}

@Composable
private fun WheelTimePickerContent(
        minutes: Int,
        seconds: Int,
        deciseconds: Int,
        maxMinutes: Int,
        accentColor: Color,
        onMinutesChange: (Int) -> Unit,
        onSecondsChange: (Int) -> Unit,
        onDecisecondsChange: (Int) -> Unit
) {
    Box(
            modifier = Modifier.fillMaxWidth().height(140.dp), // Reduced height
            contentAlignment = Alignment.Center
    ) {
        // Selection Indicator (Subtle background)
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(40.dp) // Reduced height
                                .background(
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                )
        )

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Minutes
            WheelPicker(
                    value = minutes,
                    range = 0..maxMinutes.coerceAtLeast(59),
                    onValueChange = onMinutesChange,
                    modifier = Modifier.weight(1f)
            )

            // Separator
            Text(
                    text = ":",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 2.dp).offset(y = (-2).dp)
            )

            // Seconds
            WheelPicker(
                    value = seconds,
                    range = 0..59,
                    onValueChange = onSecondsChange,
                    modifier = Modifier.weight(1f)
            )

            // Separator (Dot)
            Text(
                    text = ".",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp).offset(y = (-2).dp)
            )

            // Deciseconds
            WheelPicker(
                    value = deciseconds,
                    range = 0..9,
                    onValueChange = onDecisecondsChange,
                    modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelPicker(
        value: Int,
        range: IntRange,
        onValueChange: (Int) -> Unit,
        modifier: Modifier = Modifier
) {
    val items = range.toList()
    val listState =
            rememberLazyListState(
                    initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, items.size - 1)
            )
    val coroutineScope = rememberCoroutineScope()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Track scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            val newValue = items.getOrElse(centerIndex) { value }
            if (newValue != value) {
                onValueChange(newValue)
            }
        }
    }

    LaunchedEffect(value) {
        val targetIndex = items.indexOf(value).coerceIn(0, items.size - 1)
        if (listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(modifier = modifier.height(140.dp), contentAlignment = Alignment.Center) {
        LazyColumn(
                state = listState,
                flingBehavior = snapFlingBehavior,
                contentPadding =
                        PaddingValues(
                                vertical = 50.dp
                        ), // Height of center area is 40dp, so padding is (140-40)/2 = 50
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = listState.firstVisibleItemIndex == index

                // Visual properties
                val opacity = if (isSelected) 1f else 0.35f
                val fontSize = if (isSelected) 22.sp else 18.sp // Reduced font sizes
                val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

                Box(
                        modifier =
                                Modifier.height(40.dp) // Item height matching center area
                                        .clickable {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(index)
                                            }
                                            onValueChange(item)
                                        },
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = String.format("%02d", item),
                            fontSize = fontSize,
                            color = Color.White.copy(alpha = opacity),
                            fontWeight = fontWeight,
                            textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardTimeInput(
        minutes: Int,
        seconds: Int,
        deciseconds: Int,
        accentColor: Color,
        onMinutesChange: (Int) -> Unit,
        onSecondsChange: (Int) -> Unit,
        onDecisecondsChange: (Int) -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth().height(100.dp), // Reduced height
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
    ) {
        TimeInputField2(value = minutes, onValueChange = onMinutesChange, accentColor = accentColor)
        Text(
                ":",
                fontSize = 24.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 6.dp)
        )
        TimeInputField2(value = seconds, onValueChange = onSecondsChange, accentColor = accentColor)
        Text(
                ".",
                fontSize = 24.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
        )
        TimeInputField2(
                value = deciseconds,
                onValueChange = onDecisecondsChange,
                accentColor = accentColor,
                maxValue = 9
        )
    }
}

@Composable
private fun TimeInputField2(
        value: Int,
        onValueChange: (Int) -> Unit,
        accentColor: Color,
        maxValue: Int = 59
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
            value = textValue,
            onValueChange = { newValue: String ->
                if (newValue.isEmpty() || newValue.all { char -> char.isDigit() }) {
                    textValue = newValue.take(2)
                    newValue.toIntOrNull()?.let { intVal: Int ->
                        onValueChange(intVal.coerceIn(0, maxValue))
                    }
                }
            },
            textStyle =
                    TextStyle(
                            fontSize = 24.sp, // Reduced font size
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                    ),
            singleLine = true,
            keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors =
                    OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            cursorColor = accentColor,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.Transparent
                    ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(64.dp).height(56.dp) // Reduced dimensions
    )
}
