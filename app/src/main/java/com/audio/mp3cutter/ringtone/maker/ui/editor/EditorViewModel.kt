package com.audio.mp3cutter.ringtone.maker.ui.editor

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Cache
import linc.com.amplituda.callback.AmplitudaErrorListener
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class EditorUiState(
    val audio: AudioModel? = null,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val waveformData: List<Int> = emptyList(),
    val errorMessage: String? = null,
    // Selection state
    val selectionStartProgress: Float = 0f,
    val selectionEndProgress: Float = 1f,
    val isLoopMode: Boolean = false
) {
    val selectionStartMs: Long get() = (duration * selectionStartProgress).toLong()
    val selectionEndMs: Long get() = (duration * selectionEndProgress).toLong()
    val selectionDurationMs: Long get() = selectionEndMs - selectionStartMs
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val amplituda = Amplituda(context)

    init {
        // Get the audio data from navigation arguments
        val audioJson = savedStateHandle.get<String>("audioJson")
        audioJson?.let { encodedJson ->
            val decodedJson = URLDecoder.decode(encodedJson, StandardCharsets.UTF_8.toString())
            val audio = Gson().fromJson(decodedJson, AudioModel::class.java)
            loadAudio(audio)
        }
    }

    private fun loadAudio(audio: AudioModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                audio = audio,
                duration = audio.duration,
                isLoading = true
            )

            // Extract waveform data
            extractWaveformData(audio.path)

            // Prepare media player
            prepareMediaPlayer(audio.path)
        }
    }

    private suspend fun extractWaveformData(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val result = amplituda.processAudio(path, Cache.withParams(Cache.REUSE))
                    .get(AmplitudaErrorListener {
                        viewModelScope.launch {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Failed to process waveform: ${it.message}"
                            )
                        }
                    })

                val amplitudes = result.amplitudesAsList()

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        waveformData = amplitudes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load waveform: ${e.message}"
                    )
                }
            }
        }
    }

    private fun prepareMediaPlayer(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)
                prepare()

                setOnCompletionListener {
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            currentPosition = 0L
                        )
                    }
                    stopProgressTracking()
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to prepare audio: ${e.message}"
            )
        }
    }

    fun togglePlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                stopProgressTracking()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                player.start()
                startProgressTracking()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _uiState.value = _uiState.value.copy(currentPosition = positionMs)
        }
    }

    fun seekToProgress(progress: Float) {
        val duration = _uiState.value.duration
        val newPosition = (duration * progress).toLong()
        seekTo(newPosition)
    }

    fun seekAndPlay(progress: Float) {
        seekToProgress(progress)
        // Auto-play if not already playing
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                startProgressTracking()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition.toLong()

                        // Check loop boundaries
                        if (_uiState.value.isLoopMode) {
                            val endMs = _uiState.value.selectionEndMs
                            if (currentPos >= endMs) {
                                // Loop back to start
                                val startMs = _uiState.value.selectionStartMs
                                player.seekTo(startMs.toInt())
                                _uiState.value = _uiState.value.copy(currentPosition = startMs)
                            } else {
                                _uiState.value = _uiState.value.copy(currentPosition = currentPos)
                            }
                        } else {
                            // Normal playback - stop at selection end
                            val endMs = _uiState.value.selectionEndMs
                            if (currentPos >= endMs) {
                                // Stop playback at selection end
                                player.pause()
                                player.seekTo(endMs.toInt())
                                _uiState.value = _uiState.value.copy(
                                    currentPosition = endMs,
                                    isPlaying = false
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(currentPosition = currentPos)
                            }
                        }
                    }
                }
                delay(16) // ~60fps for smooth progress
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    // Selection functions
    fun updateSelectionStart(progress: Float) {
        val newStart = progress.coerceIn(0f, _uiState.value.selectionEndProgress)
        _uiState.value = _uiState.value.copy(selectionStartProgress = newStart)
    }

    fun updateSelectionEnd(progress: Float) {
        val newEnd = progress.coerceIn(_uiState.value.selectionStartProgress, 1f)
        _uiState.value = _uiState.value.copy(selectionEndProgress = newEnd)
    }

    fun playSelectedLoop() {
        mediaPlayer?.let { player ->
            // Enable loop mode
            _uiState.value = _uiState.value.copy(isLoopMode = true)

            // Seek to selection start
            val startMs = _uiState.value.selectionStartMs
            player.seekTo(startMs.toInt())
            _uiState.value = _uiState.value.copy(currentPosition = startMs)

            // Start playback
            if (!player.isPlaying) {
                player.start()
                startProgressTracking()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
        }
    }

    fun stopLoopMode() {
        _uiState.value = _uiState.value.copy(isLoopMode = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
