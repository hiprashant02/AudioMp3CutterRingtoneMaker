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
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
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
        val isLoopMode: Boolean = false,
        // Processing State
        val isProcessing: Boolean = false,
        val processingProgress: Int = 0,
        val processingMessage: String = "",
        // Undo/Redo State
        val canUndo: Boolean = false,
        val canRedo: Boolean = false
) {
    val selectionStartMs: Long
        get() = (duration * selectionStartProgress).toLong()
    val selectionEndMs: Long
        get() = (duration * selectionEndProgress).toLong()
    val selectionDurationMs: Long
        get() = selectionEndMs - selectionStartMs
}

@HiltViewModel
class EditorViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, @ApplicationContext private val context: Context) :
        ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val amplituda = Amplituda(context)

    // History Stacks
    private val undoStack = ArrayDeque<AudioModel>()
    private val redoStack = ArrayDeque<AudioModel>()

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
            _uiState.value =
                    _uiState.value.copy(audio = audio, duration = audio.duration, isLoading = true)

            // Extract waveform data
            extractWaveformData(audio.path)

            // Prepare media player
            prepareMediaPlayer(audio.path)

            // Set initial selection to fit viewport
            setInitialSelection()
        }
    }

    private suspend fun extractWaveformData(path: String) {
        withContext(Dispatchers.IO) {
            try {
                // Clear Amplituda cache periodically or on new load to prevent buildup
                // invalidating just this one doesn't exist, so we rely on system

                val result =
                        amplituda
                                .processAudio(path, Cache.withParams(Cache.REUSE))
                                .get(
                                        AmplitudaErrorListener {
                                            viewModelScope.launch {
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                errorMessage =
                                                                        "Waveform error: ${it.message}"
                                                        )
                                            }
                                        }
                                )

                val amplitudes = result.amplitudesAsList()

                withContext(Dispatchers.Main) {
                    _uiState.value =
                            _uiState.value.copy(waveformData = amplitudes, isLoading = false)
                }
            } catch (e: Exception) {
                // Catch generic exceptions and OOM
                handleWaveformError(e)
            } catch (e: OutOfMemoryError) {
                // Explicitly handle OOM
                System.gc() // Suggest GC
                handleWaveformError(Exception("Out of Memory processing waveform"))
            }
        }
    }

    private suspend fun handleWaveformError(e: Throwable) {
        withContext(Dispatchers.Main) {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load waveform: ${e.message}"
                    )
        }
        // Try to clear cache if things go wrong
        try {
            amplituda.clearCache()
        } catch (ignore: Exception) {}
    }

    private fun prepareMediaPlayer(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer =
                    MediaPlayer().apply {
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
                                _uiState.value =
                                        _uiState.value.copy(isPlaying = false, currentPosition = 0L)
                            }
                            stopProgressTracking()
                        }
                    }
        } catch (e: Exception) {
            _uiState.value =
                    _uiState.value.copy(errorMessage = "Failed to prepare audio: ${e.message}")
        }
    }

    fun togglePlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                stopProgressTracking()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                // Constrain to selection range before playing
                val currentPos = _uiState.value.currentPosition
                val startMs = _uiState.value.selectionStartMs
                val endMs = _uiState.value.selectionEndMs

                if (currentPos < startMs || currentPos >= endMs) {
                    player.seekTo(startMs.toInt())
                    _uiState.value = _uiState.value.copy(currentPosition = startMs)
                }

                player.start()
                startProgressTracking()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            val startMs = _uiState.value.selectionStartMs
            val endMs = _uiState.value.selectionEndMs
            val constrainedPos = positionMs.coerceIn(startMs, endMs)

            player.seekTo(constrainedPos.toInt())
            _uiState.value = _uiState.value.copy(currentPosition = constrainedPos)
        }
    }

    fun seekToProgress(progress: Float) {
        val duration = _uiState.value.duration
        val positionMs = (duration * progress).toLong()
        val startMs = _uiState.value.selectionStartMs
        val endMs = _uiState.value.selectionEndMs
        val constrainedPos = positionMs.coerceIn(startMs, endMs)

        seekTo(constrainedPos)
    }

    fun seekAndPlay(progress: Float) {
        // Calculate and constrain position
        val duration = _uiState.value.duration
        val positionMs = (duration * progress).toLong()
        val startMs = _uiState.value.selectionStartMs
        val endMs = _uiState.value.selectionEndMs
        val constrainedPos = positionMs.coerceIn(startMs, endMs)

        seekTo(constrainedPos)

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
        progressJob =
                viewModelScope.launch {
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
                                        _uiState.value =
                                                _uiState.value.copy(currentPosition = startMs)
                                    } else {
                                        _uiState.value =
                                                _uiState.value.copy(currentPosition = currentPos)
                                    }
                                } else {
                                    // Normal playback - stop at selection end
                                    val endMs = _uiState.value.selectionEndMs
                                    if (currentPos >= endMs) {
                                        // Stop playback at selection end
                                        player.pause()
                                        player.seekTo(endMs.toInt())
                                        _uiState.value =
                                                _uiState.value.copy(
                                                        currentPosition = endMs,
                                                        isPlaying = false
                                                )
                                    } else {
                                        _uiState.value =
                                                _uiState.value.copy(currentPosition = currentPos)
                                    }
                                }
                            }
                        }
                        delay(16) // ~60fps for smooth progress
                    }
                }
    }

    private fun setInitialSelection() {
        val durationSeconds = _uiState.value.duration / 1000f

        // For songs > 30s, set initial selection with offset from left edge
        // Start at 5% and end at 35% (30% width) for visual breathing room
        // For shorter songs, keep full selection (both handlers visible anyway)
        if (durationSeconds > 30f) {
            val initialStartProgress = 0.05f // 5% offset from left
            val selectionWidth = 0.30f // 30% selection width
            val initialEndProgress = initialStartProgress + selectionWidth

            _uiState.value =
                    _uiState.value.copy(
                            selectionStartProgress = initialStartProgress,
                            selectionEndProgress = initialEndProgress
                    )
        }
        // For short songs, keep default 0% to 100%
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

    // Undo/Redo Logic
    private fun saveToHistory() {
        val currentAudio = _uiState.value.audio ?: return
        undoStack.addLast(currentAudio)
        redoStack.clear()
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _uiState.value =
                _uiState.value.copy(
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty()
                )
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val currentAudio = _uiState.value.audio
        if (currentAudio != null) {
            redoStack.addLast(currentAudio)
        }

        val previousAudio = undoStack.removeLast()
        loadAudio(previousAudio) // Will re-extract waveform
        updateUndoRedoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val currentAudio = _uiState.value.audio
        if (currentAudio != null) {
            undoStack.addLast(currentAudio)
        }

        val nextAudio = redoStack.removeLast()
        loadAudio(nextAudio)
        updateUndoRedoState()
    }

    // Feature: Trim (Keep Selected)
    fun trimAudio() {
        val currentAudio = _uiState.value.audio ?: return

        // Save history before processing
        saveToHistory()

        val startMs = _uiState.value.selectionStartMs
        val endMs = _uiState.value.selectionEndMs

        // Stop playback
        if (_uiState.value.isPlaying) {
            togglePlayback()
        }

        _uiState.value =
                _uiState.value.copy(
                        isProcessing = true,
                        processingMessage = "Trimming Audio...",
                        processingProgress = 0
                )

        val outputPath =
                File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp3").absolutePath

        com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.executeTrim(
                inputPath = currentAudio.path,
                outputPath = outputPath,
                startMs = startMs,
                endMs = endMs,
                durationMs = endMs - startMs, // Approx duration of output
                callback =
                        object :
                                com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.FFmpegCallback {
                            override fun onProgress(percentage: Int) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(processingProgress = percentage)
                                }
                            }

                            override fun onSuccess(outputPath: String) {
                                handleProcessingSuccess(outputPath)
                            }

                            override fun onError(errorMessage: String) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isProcessing = false,
                                                    errorMessage = errorMessage
                                            )
                                }
                            }
                        }
        )
    }

    // Feature: Cut (Delete Selected)
    fun cutAudio() {
        val currentAudio = _uiState.value.audio ?: return

        // Save history before processing
        saveToHistory()

        val startMs = _uiState.value.selectionStartMs
        val endMs = _uiState.value.selectionEndMs
        val totalDuration = _uiState.value.duration

        // Stop playback
        if (_uiState.value.isPlaying) {
            togglePlayback()
        }

        _uiState.value =
                _uiState.value.copy(
                        isProcessing = true,
                        processingMessage = "Cutting Audio...",
                        processingProgress = 0
                )

        val outputPath =
                File(context.cacheDir, "cut_${System.currentTimeMillis()}.mp3").absolutePath

        com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.executeCut(
                inputPath = currentAudio.path,
                outputPath = outputPath,
                cutStartMs = startMs,
                cutEndMs = endMs,
                totalDurationMs = totalDuration,
                callback =
                        object :
                                com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.FFmpegCallback {
                            override fun onProgress(percentage: Int) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(processingProgress = percentage)
                                }
                            }

                            override fun onSuccess(outputPath: String) {
                                handleProcessingSuccess(outputPath)
                            }

                            override fun onError(errorMessage: String) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isProcessing = false,
                                                    errorMessage = errorMessage
                                            )
                                }
                            }
                        }
        )
    }

    // Feature: Volume
    fun changeVolume(volumeMultiplier: Float) {
        val currentAudio = _uiState.value.audio ?: return

        // Save history before processing
        saveToHistory()

        // Stop playback
        if (_uiState.value.isPlaying) {
            togglePlayback()
        }

        _uiState.value =
                _uiState.value.copy(
                        isProcessing = true,
                        processingMessage = "Adjusting Volume...",
                        processingProgress = 0
                )

        val outputPath =
                File(context.cacheDir, "vol_${System.currentTimeMillis()}.mp3").absolutePath
        val duration = _uiState.value.duration

        com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.executeVolume(
                inputPath = currentAudio.path,
                outputPath = outputPath,
                volumeMultiplier = volumeMultiplier,
                durationMs = duration,
                callback =
                        object :
                                com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.FFmpegCallback {
                            override fun onProgress(percentage: Int) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(processingProgress = percentage)
                                }
                            }

                            override fun onSuccess(outputPath: String) {
                                handleProcessingSuccess(outputPath)
                            }

                            override fun onError(errorMessage: String) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isProcessing = false,
                                                    errorMessage = errorMessage
                                            )
                                }
                            }
                        }
        )
    }

    // Feature: Speed
    fun changeSpeed(speedMultiplier: Float) {
        val currentAudio = _uiState.value.audio ?: return

        // Save history before processing
        saveToHistory()

        // Stop playback
        if (_uiState.value.isPlaying) {
            togglePlayback()
        }

        _uiState.value =
                _uiState.value.copy(
                        isProcessing = true,
                        processingMessage = "Changing Speed...",
                        processingProgress = 0
                )

        val outputPath =
                File(context.cacheDir, "speed_${System.currentTimeMillis()}.mp3").absolutePath
        val duration = _uiState.value.duration

        com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.executeSpeed(
                inputPath = currentAudio.path,
                outputPath = outputPath,
                speedMultiplier = speedMultiplier,
                durationMs = duration,
                callback =
                        object :
                                com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.FFmpegCallback {
                            override fun onProgress(percentage: Int) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(processingProgress = percentage)
                                }
                            }

                            override fun onSuccess(outputPath: String) {
                                handleProcessingSuccess(outputPath)
                            }

                            override fun onError(errorMessage: String) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isProcessing = false,
                                                    errorMessage = errorMessage
                                            )
                                }
                            }
                        }
        )
    }

    // Feature: Fade In/Out
    fun applyFade(fadeInMs: Long, fadeOutMs: Long) {
        val currentAudio = _uiState.value.audio ?: return

        if (fadeInMs == 0L && fadeOutMs == 0L) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please set a fade duration")
            return
        }

        // Save history before processing
        saveToHistory()

        // Stop playback
        if (_uiState.value.isPlaying) {
            togglePlayback()
        }

        _uiState.value =
                _uiState.value.copy(
                        isProcessing = true,
                        processingMessage = "Applying Fade Effect...",
                        processingProgress = 0
                )

        val outputPath =
                File(context.cacheDir, "fade_${System.currentTimeMillis()}.mp3").absolutePath
        val duration = _uiState.value.duration

        com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.executeFade(
                inputPath = currentAudio.path,
                outputPath = outputPath,
                fadeInDurationMs = fadeInMs,
                fadeOutDurationMs = fadeOutMs,
                totalDurationMs = duration,
                callback =
                        object :
                                com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager.FFmpegCallback {
                            override fun onProgress(percentage: Int) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(processingProgress = percentage)
                                }
                            }

                            override fun onSuccess(outputPath: String) {
                                handleProcessingSuccess(outputPath)
                            }

                            override fun onError(errorMessage: String) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isProcessing = false,
                                                    errorMessage = errorMessage
                                            )
                                }
                            }
                        }
        )
    }

    private fun handleProcessingSuccess(outputPath: String) {
        viewModelScope.launch {
            // Create a new AudioModel for the edited file
            val newFile = File(outputPath)
            val newDuration = getDuration(outputPath)

            val newAudio =
                    _uiState.value.audio?.copy(
                            path = outputPath,
                            duration = newDuration,
                            size = newFile.length(),
                            title = "Edited_${_uiState.value.audio?.title}"
                    )

            if (newAudio != null) {
                // Reset state and load new audio
                _uiState.value =
                        _uiState.value.copy(
                                isProcessing = false,
                                audio = newAudio,
                                duration = newDuration,
                                selectionStartProgress = 0f,
                                selectionEndProgress = 1f,
                                currentPosition = 0L,
                                errorMessage = null
                        )

                // Reload player and waveform
                loadAudio(newAudio)
            } else {
                _uiState.value =
                        _uiState.value.copy(
                                isProcessing = false,
                                errorMessage = "Failed to load edited file"
                        )
            }
        }
    }

    private fun getDuration(path: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val time =
                    retriever.extractMetadata(
                            android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            amplituda.clearCache()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
