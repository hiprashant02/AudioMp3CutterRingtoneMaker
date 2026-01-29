package com.audio.mp3cutter.ringtone.maker.ui.merger

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import com.audio.mp3cutter.ringtone.maker.utils.FFmpegManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MergerUiState(
        val selectedAudios: List<AudioModel> = emptyList(),
        val isProcessing: Boolean = false,
        val processingProgress: Int = 0,
        val processingMessage: String = "",
        val errorMessage: String? = null,
        val mergedAudio: AudioModel? = null,
        val showPreviewDialog: Boolean = false,
        val isPlaying: Boolean = false,
        val playbackProgress: Float = 0f,
        val currentPlaybackPosition: Long = 0L
) {
    val canMerge: Boolean
        get() = selectedAudios.size >= 2
    val totalDuration: Long
        get() = selectedAudios.sumOf { it.duration }
}

@HiltViewModel
class AudioMergerViewModel @Inject constructor(@ApplicationContext private val context: Context) :
        ViewModel() {

    private val _uiState = MutableStateFlow(MergerUiState())
    val uiState: StateFlow<MergerUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun addAudio(audio: AudioModel) {
        val currentList = _uiState.value.selectedAudios.toMutableList()
        // Avoid duplicates
        if (currentList.none { it.id == audio.id }) {
            currentList.add(audio)
            _uiState.value = _uiState.value.copy(selectedAudios = currentList)
        }
    }

    fun removeAudio(index: Int) {
        val currentList = _uiState.value.selectedAudios.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedAudios = currentList)
        }
    }

    fun reorderAudios(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.selectedAudios.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.value = _uiState.value.copy(selectedAudios = currentList)
        }
    }

    fun moveUp(index: Int) {
        if (index > 0) {
            reorderAudios(index, index - 1)
        }
    }

    fun moveDown(index: Int) {
        val currentList = _uiState.value.selectedAudios
        if (index < currentList.size - 1) {
            reorderAudios(index, index + 1)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearMergedAudio() {
        _uiState.value = _uiState.value.copy(mergedAudio = null)
    }

    fun mergeAudios() {
        val audios = _uiState.value.selectedAudios
        if (audios.size < 2) {
            _uiState.value =
                    _uiState.value.copy(errorMessage = "Select at least 2 audio files to merge")
            return
        }

        _uiState.value =
                _uiState.value.copy(
                        isProcessing = true,
                        processingProgress = 0,
                        processingMessage = "Merging ${audios.size} audio files..."
                )

        val inputPaths = audios.map { it.path }
        val totalDuration = audios.sumOf { it.duration }
        val outputPath =
                File(context.cacheDir, "merged_${System.currentTimeMillis()}.mp3").absolutePath

        FFmpegManager.executeMerge(
                inputPaths = inputPaths,
                outputPath = outputPath,
                totalDurationMs = totalDuration,
                callback =
                        object : FFmpegManager.FFmpegCallback {
                            override fun onProgress(percentage: Int) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(processingProgress = percentage)
                                }
                            }

                            override fun onSuccess(outputPath: String) {
                                viewModelScope.launch {
                                    handleMergeSuccess(outputPath, totalDuration)
                                }
                            }

                            override fun onError(errorMessage: String) {
                                viewModelScope.launch {
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isProcessing = false,
                                                    errorMessage = "Merge failed: $errorMessage"
                                            )
                                }
                            }
                        }
        )
    }

    private fun handleMergeSuccess(outputPath: String, totalDuration: Long) {
        val file = File(outputPath)
        val mergedAudio =
                AudioModel(
                        id = System.currentTimeMillis(),
                        title = "Merged_Audio_${System.currentTimeMillis()}",
                        artist = "Mashup",
                        duration = totalDuration,
                        size = file.length(),
                        path = outputPath,
                        contentUri = "file://$outputPath"
                )

        _uiState.value = _uiState.value.copy(
            isProcessing = false,
            mergedAudio = mergedAudio,
            showPreviewDialog = true
        )
    }

    // Preview Dialog Controls
    fun dismissPreviewDialog() {
        stopPlayback()
        _uiState.value = _uiState.value.copy(showPreviewDialog = false, mergedAudio = null)
    }

    fun onSaveClicked(): AudioModel? {
        stopPlayback()
        val audio = _uiState.value.mergedAudio
        _uiState.value = _uiState.value.copy(showPreviewDialog = false)
        return audio
    }

    fun onCutterClicked(): AudioModel? {
        stopPlayback()
        val audio = _uiState.value.mergedAudio
        _uiState.value = _uiState.value.copy(showPreviewDialog = false)
        return audio
    }

    // Playback Controls
    fun togglePlayback() {
        val audio = _uiState.value.mergedAudio ?: return
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback(audio)
        }
    }

    private fun startPlayback(audio: AudioModel) {
        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audio.path)
                prepare()
                start()
                setOnCompletionListener {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        playbackProgress = 0f,
                        currentPlaybackPosition = 0L
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isPlaying = true)
            startProgressTracker()
        } catch (e: IOException) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
        stopProgressTracker()
    }

    fun stopPlayback() {
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            playbackProgress = 0f,
            currentPlaybackPosition = 0L
        )
    }

    fun seekTo(fraction: Float) {
        mediaPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val newPosition = (duration * fraction).toInt()
                player.seekTo(newPosition)
                _uiState.value = _uiState.value.copy(
                    playbackProgress = fraction,
                    currentPlaybackPosition = newPosition.toLong()
                )
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        try {
                            val currentPosition = player.currentPosition.toFloat()
                            val duration = player.duration.toFloat()
                            if (duration > 0) {
                                _uiState.value = _uiState.value.copy(
                                    playbackProgress = currentPosition / duration,
                                    currentPlaybackPosition = currentPosition.toLong()
                                )
                            }
                        } catch (e: Exception) {
                            // Handle illegal state
                        }
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }

    /** Process a URI from system file picker and add to list */
    fun processUri(uri: Uri): AudioModel? {
        return try {
            // Get file name from URI
            var fileName = "imported_audio.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            // Copy to cache
            val cacheFile = File(context.cacheDir, "import_${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
            }

            // Get duration
            val duration = getDuration(cacheFile.absolutePath)

            val audio =
                    AudioModel(
                            id = System.currentTimeMillis(),
                            title = fileName.substringBeforeLast("."),
                            artist = "Imported",
                            duration = duration,
                            size = cacheFile.length(),
                            path = cacheFile.absolutePath,
                            contentUri = uri.toString()
                    )

            addAudio(audio)
            audio
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to import: ${e.message}")
            null
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
}
