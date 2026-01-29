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
                                                        onOpenAudio = {
                                                                navController.navigate("browser")
                                                        },
                                                        onRecordAudio = {
                                                                navController.navigate("recorder")
                                                        },
                                                        onCutAudio = {
                                                                navController.navigate("browser")
                                                        },
                                                        onMergeAudio = {
                                                                navController.navigate("merger")
                                                        },
                                                        onConvertAudio = {
                                                                navController.navigate("converterBrowser")
                                                        },
                                                        onSetRingtone = {
                                                                navController.navigate("ringtoneBrowser")
                                                        },
                                                        onShareApp = {
                                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                        type = "text/plain"
                                                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Audio Editor - MP3 Cutter & Ringtone Maker")
                                                                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out Audio Editor - the best MP3 cutter & ringtone maker!\n\nhttps://play.google.com/store/apps/details?id=${packageName}")
                                                                }
                                                                startActivity(android.content.Intent.createChooser(shareIntent, "Share App"))
                                                        },
                                                        onRateApp = {
                                                                try {
                                                                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")))
                                                                } catch (e: android.content.ActivityNotFoundException) {
                                                                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                                                                }
                                                        },
                                                        onProjectClick = { audio ->
                                                                // Navigate to editor with the project
                                                                val audioJson = Gson().toJson(audio)
                                                                val encodedJson = URLEncoder.encode(audioJson, StandardCharsets.UTF_8.toString())
                                                                navController.navigate("editor/$encodedJson")
                                                        },
                                                        onSeeAllProjects = {
                                                                navController.navigate("projects")
                                                        },
                                                        onAboutClick = {
                                                                navController.navigate("about")
                                                        }
                                                )
                                        }
                                        composable("about") {
                                                com.audio.mp3cutter.ringtone.maker.ui.about.AboutScreen(
                                                        onNavigateBack = {
                                                                navController.popBackStack()
                                                        },
                                                        onPrivacyPolicy = {
                                                                navController.navigate("privacyPolicy")
                                                        }
                                                )
                                        }
                                        composable("privacyPolicy") {
                                                com.audio.mp3cutter.ringtone.maker.ui.about.PrivacyPolicyScreen(
                                                        onNavigateBack = {
                                                                navController.popBackStack()
                                                        }
                                                )
                                        }
                                        composable("projects") {
                                                com.audio.mp3cutter.ringtone.maker.ui.projects.ProjectsScreen(
                                                        onNavigateBack = {
                                                                navController.popBackStack()
                                                        },
                                                        onProjectClick = { audio ->
                                                                val audioJson = Gson().toJson(audio)
                                                                val encodedJson = URLEncoder.encode(audioJson, StandardCharsets.UTF_8.toString())
                                                                navController.navigate("editor/$encodedJson")
                                                        }
                                                )
                                        }
                                        composable("browser") {
                                                com.audio.mp3cutter.ringtone.maker.ui.browser
                                                        .FileBrowserScreen(
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onAudioSelected = { audio ->
                                                                        // Navigate to editor with
                                                                        // audio data
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "editor/$encodedJson"
                                                                        )
                                                                },
                                                                onRecordVoice = {
                                                                        navController.navigate(
                                                                                "recorder"
                                                                        )
                                                                }
                                                        )
                                        }
                                        composable(
                                                route = "editor/{audioJson}",
                                                arguments =
                                                        listOf(
                                                                navArgument("audioJson") {
                                                                        type = NavType.StringType
                                                                }
                                                        )
                                        ) {
                                                EditorScreen(
                                                        onNavigateBack = {
                                                                navController.popBackStack()
                                                        },
                                                        onSave = { audio ->
                                                                val audioJson = Gson().toJson(audio)
                                                                val encodedJson =
                                                                        URLEncoder.encode(
                                                                                audioJson,
                                                                                StandardCharsets
                                                                                        .UTF_8
                                                                                        .toString()
                                                                        )
                                                                navController.navigate(
                                                                        "export/$encodedJson"
                                                                )
                                                        }
                                                )
                                        }
                                        composable("recorder") {
                                                com.audio.mp3cutter.ringtone.maker.ui.recorder
                                                        .VoiceRecorderScreen(
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onEditRecording = { audio ->
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "editor/$encodedJson"
                                                                        )
                                                                },
                                                                onSaveRecording = { audio ->
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "export/$encodedJson"
                                                                        )
                                                                }
                                                        )
                                        }
                                        composable("merger") { backStackEntry ->
                                                // Observe audio returned from browser
                                                val selectedAudioJson =
                                                        backStackEntry.savedStateHandle.get<String>(
                                                                "selectedAudioForMerger"
                                                        )

                                                val mergerViewModel:
                                                        com.audio.mp3cutter.ringtone.maker.ui.merger.AudioMergerViewModel =
                                                        androidx.hilt.navigation.compose
                                                                .hiltViewModel()

                                                // Add returned audio to merger
                                                androidx.compose.runtime.LaunchedEffect(
                                                        selectedAudioJson
                                                ) {
                                                        selectedAudioJson?.let { json ->
                                                                val audio =
                                                                        Gson().fromJson(
                                                                                        json,
                                                                                        com.audio
                                                                                                        .mp3cutter
                                                                                                        .ringtone
                                                                                                        .maker
                                                                                                        .data
                                                                                                        .model
                                                                                                        .AudioModel::class
                                                                                                .java
                                                                                )
                                                                mergerViewModel.addAudio(audio)
                                                                backStackEntry.savedStateHandle
                                                                        .remove<String>(
                                                                                "selectedAudioForMerger"
                                                                        )
                                                        }
                                                }

                                                com.audio.mp3cutter.ringtone.maker.ui.merger
                                                        .AudioMergerScreen(
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onAddSong = {
                                                                        navController.navigate(
                                                                                "browserForMerger"
                                                                        )
                                                                },
                                                                onSaveAudio = { audio ->
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "export/$encodedJson"
                                                                        )
                                                                },
                                                                onCutAudio = { audio ->
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "editor/$encodedJson"
                                                                        )
                                                                },
                                                                viewModel = mergerViewModel
                                                        )
                                        }
                                        composable("browserForMerger") {
                                                com.audio.mp3cutter.ringtone.maker.ui.browser
                                                        .FileBrowserScreen(
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onAudioSelected = { audio ->
                                                                        // Return audio to merger
                                                                        // screen
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        navController
                                                                                .previousBackStackEntry
                                                                                ?.savedStateHandle
                                                                                ?.set(
                                                                                        "selectedAudioForMerger",
                                                                                        audioJson
                                                                                )
                                                                        navController.popBackStack()
                                                                },
                                                                onRecordVoice = {
                                                                        navController.navigate(
                                                                                "recorder"
                                                                        )
                                                                }
                                                        )
                                        }
                                        composable(
                                                route = "export/{audioJson}",
                                                arguments =
                                                        listOf(
                                                                navArgument("audioJson") {
                                                                        type = NavType.StringType
                                                                }
                                                        )
                                        ) { backStackEntry ->
                                                val audioJson =
                                                        backStackEntry.arguments?.getString(
                                                                "audioJson"
                                                        )
                                                                ?: ""
                                                val audio =
                                                        Gson().fromJson(
                                                                        java.net.URLDecoder.decode(
                                                                                audioJson,
                                                                                StandardCharsets
                                                                                        .UTF_8
                                                                                        .toString()
                                                                        ),
                                                                        com.audio.mp3cutter.ringtone
                                                                                        .maker.data
                                                                                        .model
                                                                                        .AudioModel::class
                                                                                .java
                                                                )
                                                com.audio.mp3cutter.ringtone.maker.ui.export
                                                        .ExportScreen(
                                                                audio = audio,
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onExportComplete = {
                                                                        navController.popBackStack(
                                                                                "home",
                                                                                inclusive = false
                                                                        )
                                                                }
                                                        )
                                        }
                                        composable("converterBrowser") {
                                                com.audio.mp3cutter.ringtone.maker.ui.browser
                                                        .FileBrowserScreen(
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onAudioSelected = { audio ->
                                                                        // Navigate directly to export for conversion
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "export/$encodedJson"
                                                                        )
                                                                },
                                                                onRecordVoice = {
                                                                        navController.navigate(
                                                                                "recorder"
                                                                        )
                                                                }
                                                        )
                                        }
                                        composable("ringtoneBrowser") {
                                                com.audio.mp3cutter.ringtone.maker.ui.browser
                                                        .FileBrowserScreen(
                                                                onNavigateBack = {
                                                                        navController.popBackStack()
                                                                },
                                                                onAudioSelected = { audio ->
                                                                        // Navigate to ringtone picker screen
                                                                        val audioJson =
                                                                                Gson().toJson(audio)
                                                                        val encodedJson =
                                                                                URLEncoder.encode(
                                                                                        audioJson,
                                                                                        StandardCharsets
                                                                                                .UTF_8
                                                                                                .toString()
                                                                                )
                                                                        navController.navigate(
                                                                                "ringtonePicker/$encodedJson"
                                                                        )
                                                                },
                                                                onRecordVoice = {
                                                                        navController.navigate(
                                                                                "recorder"
                                                                        )
                                                                }
                                                        )
                                        }
                                        composable(
                                                route = "ringtonePicker/{audioJson}",
                                                arguments = listOf(
                                                        navArgument("audioJson") {
                                                                type = NavType.StringType
                                                        }
                                                )
                                        ) { backStackEntry ->
                                                val audioJson = backStackEntry.arguments?.getString("audioJson") ?: ""
                                                val audio = Gson().fromJson(
                                                        java.net.URLDecoder.decode(
                                                                audioJson,
                                                                StandardCharsets.UTF_8.toString()
                                                        ),
                                                        com.audio.mp3cutter.ringtone.maker.data.model.AudioModel::class.java
                                                )
                                                com.audio.mp3cutter.ringtone.maker.ui.ringtone.RingtonePickerScreen(
                                                        audio = audio,
                                                        onNavigateBack = {
                                                                navController.popBackStack()
                                                        },
                                                        onComplete = {
                                                                navController.popBackStack("home", inclusive = false)
                                                        }
                                                )
                                        }
                                }
                        }
                }
        }
}
