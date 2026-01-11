package com.audio.mp3cutter.ringtone.maker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.audio.mp3cutter.ringtone.maker.ui.HomeScreen
import com.audio.mp3cutter.ringtone.maker.ui.WorkInProgressScreen
import com.audio.mp3cutter.ringtone.maker.ui.editor.EditorScreen
import com.audio.mp3cutter.ringtone.maker.ui.theme.AudioMp3CutterRingtoneMakerTheme
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioMp3CutterRingtoneMakerTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        HomeScreen(
                            onOpenAudio = { navController.navigate("browser") },
                            onRecordAudio = { navController.navigate("recorder") },
                            onCutAudio = { navController.navigate("browser") },
                            onMergeAudio = { navController.navigate("merger") },
                            onConvertAudio = { navController.navigate("converter") }
                        )
                    }
                    composable("browser") {
                        com.audio.mp3cutter.ringtone.maker.ui.browser.FileBrowserScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onAudioSelected = { audio ->
                                // Navigate to editor with audio data
                                val audioJson = Gson().toJson(audio)
                                val encodedJson = URLEncoder.encode(audioJson, StandardCharsets.UTF_8.toString())
                                navController.navigate("editor/$encodedJson")
                            }
                        )
                    }
                    composable(
                        route = "editor/{audioJson}",
                        arguments = listOf(
                            navArgument("audioJson") { type = NavType.StringType }
                        )
                    ) {
                        EditorScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("recorder") { WorkInProgressScreen("Voice Recorder") }
                    composable("merger") { WorkInProgressScreen("Audio Merger") }
                    composable("converter") { WorkInProgressScreen("Audio Converter") }
                }
            }
        }
    }
}