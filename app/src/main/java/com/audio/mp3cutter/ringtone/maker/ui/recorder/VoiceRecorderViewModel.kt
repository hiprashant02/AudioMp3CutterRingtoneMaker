package com.audio.mp3cutter.ringtone.maker.ui.recorder

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecorderUiState(
        val recordingState: RecordingState = RecordingState.Idle,
        val elapsedTimeMs: Long = 0L,
        val amplitudes: List<Int> = emptyList(),
        val fullWaveform: List<Int> = emptyList(), // All recorded amplitudes for playback
        val recordingPath: String? = null,
        val errorMessage: String? = null,
        val isPlaying: Boolean = false,
        val playbackProgress: Float = 0f, // 0.0 to 1.0
        val recordingDurationMs: Long = 0L
)

enum class RecordingState {
    Idle, // Ready to record
    Recording, // Currently recording
    Paused, // Recording paused
    Stopped // Recording finished, ready to save/edit
}

@HiltViewModel
class VoiceRecorderViewModel @Inject constructor(@ApplicationContext private val context: Context) :
        ViewModel() {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var playbackProgressJob: Job? = null
    private var recordingStartTime: Long = 0L
    private var pausedTimeAccumulated: Long = 0L
    private var pauseStartTime: Long = 0L
    private var currentRecordingFile: File? = null
    private val allRecordedAmplitudes = mutableListOf<Int>()

    // Number of amplitude samples to show in real-time view
    private val maxAmplitudeSamples = 50

    fun startRecording() {
        try {
            // Create output file
            val outputDir = File(context.cacheDir, "recordings")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            // Initialize MediaRecorder
            mediaRecorder =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(context)
                            } else {
                                @Suppress("DEPRECATION") MediaRecorder()
                            }
                            .apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setAudioSamplingRate(44100)
                                setAudioEncodingBitRate(128000)
                                setOutputFile(outputFile.absolutePath)
                                prepare()
                                start()
                            }

            recordingStartTime = System.currentTimeMillis()
            pausedTimeAccumulated = 0L
            allRecordedAmplitudes.clear()

            _uiState.update {
                it.copy(
                        recordingState = RecordingState.Recording,
                        elapsedTimeMs = 0L,
                        amplitudes = emptyList(),
                        fullWaveform = emptyList(),
                        recordingPath = outputFile.absolutePath,
                        errorMessage = null,
                        playbackProgress = 0f
                )
            }

            startTimer()
            startAmplitudeSampling()
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(
                        recordingState = RecordingState.Idle,
                        errorMessage = "Failed to start recording: ${e.message}"
                )
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            timerJob?.cancel()
            timerJob = null
            amplitudeJob?.cancel()
            amplitudeJob = null

            // Save full waveform and duration
            val duration = _uiState.value.elapsedTimeMs
            val waveform = allRecordedAmplitudes.toList()

            _uiState.update {
                it.copy(
                        recordingState = RecordingState.Stopped,
                        fullWaveform = waveform,
                        recordingDurationMs = duration
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(
                        recordingState = RecordingState.Idle,
                        errorMessage = "Failed to stop recording: ${e.message}"
                )
            }
        }
    }

    fun pauseRecording() {
        try {
            mediaRecorder?.pause()
            pauseStartTime = System.currentTimeMillis()

            timerJob?.cancel()
            timerJob = null
            amplitudeJob?.cancel()
            amplitudeJob = null

            _uiState.update { it.copy(recordingState = RecordingState.Paused) }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(errorMessage = "Failed to pause recording: ${e.message}") }
        }
    }

    fun resumeRecording() {
        try {
            mediaRecorder?.resume()

            // Add the paused duration to accumulated time and reset recording start
            pausedTimeAccumulated += _uiState.value.elapsedTimeMs
            recordingStartTime = System.currentTimeMillis()

            _uiState.update { it.copy(recordingState = RecordingState.Recording) }

            startTimer()
            startAmplitudeSampling()
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(errorMessage = "Failed to resume recording: ${e.message}") }
        }
    }

    fun discardRecording() {
        // Stop playback if playing
        stopPreview()

        // Delete the recording file
        currentRecordingFile?.delete()
        currentRecordingFile = null

        // Reset state
        _uiState.update { RecorderUiState() }
    }

    fun playPreview() {
        val file = currentRecordingFile ?: return
        if (!file.exists()) return

        try {
            stopPreview() // Stop any existing playback

            mediaPlayer =
                    MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        setOnCompletionListener {
                            playbackProgressJob?.cancel()
                            _uiState.update { it.copy(isPlaying = false, playbackProgress = 0f) }
                        }
                        prepare()
                        start()
                    }
            _uiState.update { it.copy(isPlaying = true, playbackProgress = 0f) }
            startPlaybackProgressTracking()
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(errorMessage = "Failed to play recording: ${e.message}") }
        }
    }

    fun stopPreview() {
        try {
            playbackProgressJob?.cancel()
            playbackProgressJob = null

            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _uiState.update { it.copy(isPlaying = false, playbackProgress = 0f) }
        } catch (e: Exception) {
            // Ignore stop errors
        }
    }

    private fun startPlaybackProgressTracking() {
        playbackProgressJob =
                viewModelScope.launch {
                    while (isActive && mediaPlayer != null) {
                        try {
                            val player = mediaPlayer ?: break
                            if (player.isPlaying) {
                                val progress =
                                        player.currentPosition.toFloat() / player.duration.toFloat()
                                _uiState.update {
                                    it.copy(playbackProgress = progress.coerceIn(0f, 1f))
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                        delay(50) // Update 20 times per second
                    }
                }
    }

    fun togglePreview() {
        if (_uiState.value.isPlaying) {
            stopPreview()
        } else {
            playPreview()
        }
    }

    fun getRecordedAudio(): AudioModel? {
        val file = currentRecordingFile ?: return null
        if (!file.exists()) return null

        return AudioModel(
                id = System.currentTimeMillis(),
                title = "Voice Recording",
                artist = "Recorded",
                duration = _uiState.value.elapsedTimeMs,
                size = file.length(),
                path = file.absolutePath,
                contentUri = ""
        )
    }

    fun saveRecording(): String? {
        val sourceFile = currentRecordingFile ?: return null
        if (!sourceFile.exists()) return null

        try {
            // Create output directory in external storage
            val outputDir = File(context.getExternalFilesDir(null), "Recordings")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val timestamp =
                    java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            .format(java.util.Date())

            val outputFile = File(outputDir, "Recording_$timestamp.m4a")
            sourceFile.copyTo(outputFile, overwrite = true)

            // Reset state after saving
            _uiState.update { RecorderUiState() }
            currentRecordingFile = null

            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(errorMessage = "Failed to save recording: ${e.message}") }
            return null
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun startTimer() {
        timerJob =
                viewModelScope.launch {
                    while (isActive) {
                        val elapsed =
                                System.currentTimeMillis() - recordingStartTime +
                                        pausedTimeAccumulated
                        _uiState.update { it.copy(elapsedTimeMs = elapsed) }
                        delay(100) // Update every 100ms for smooth timer
                    }
                }
    }

    private fun startAmplitudeSampling() {
        amplitudeJob =
                viewModelScope.launch {
                    while (isActive) {
                        try {
                            val amplitude = mediaRecorder?.maxAmplitude ?: 0
                            // Normalize amplitude to 0-100 range for visualization
                            val normalizedAmplitude = (amplitude / 327.67).toInt().coerceIn(0, 100)

                            // Store ALL amplitudes for playback waveform
                            allRecordedAmplitudes.add(normalizedAmplitude)

                            _uiState.update { state ->
                                val newAmplitudes =
                                        state.amplitudes.toMutableList().apply {
                                            add(normalizedAmplitude)
                                            // Keep only the last N samples for real-time view
                                            while (size > maxAmplitudeSamples) {
                                                removeAt(0)
                                            }
                                        }
                                state.copy(amplitudes = newAmplitudes)
                            }
                        } catch (e: Exception) {
                            // Ignore amplitude sampling errors
                        }
                        delay(50) // Sample at 20Hz for smooth visualization
                    }
                }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        amplitudeJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
