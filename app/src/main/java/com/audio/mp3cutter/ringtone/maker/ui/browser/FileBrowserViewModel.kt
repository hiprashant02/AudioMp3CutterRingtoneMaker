package com.audio.mp3cutter.ringtone.maker.ui.browser

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audio.mp3cutter.ringtone.maker.data.AudioRepository
import com.audio.mp3cutter.ringtone.maker.data.AudioSortOption
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FileBrowserViewModel @Inject constructor(private val repository: AudioRepository) :
        ViewModel() {

    private val _rawAudioList = MutableStateFlow<List<AudioModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentPlayingAudio = MutableStateFlow<AudioModel?>(null)
    val currentPlayingAudio: StateFlow<AudioModel?> = _currentPlayingAudio

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    // Sort State
    private val _sortOption = MutableStateFlow(AudioSortOption.DATE_DESC)
    val sortOption: StateFlow<AudioSortOption> = _sortOption

    private var mediaPlayer: MediaPlayer? = null

    // Pagination State
    private var currentPage = 0
    private val pageSize = 50
    private var isLastPage = false

    // State is now directly driven by the raw list, as the list itself is filtered by the DB
    val audioList: StateFlow<List<AudioModel>> = _rawAudioList

    init {
        loadAudioFiles(reset = true)
    }

    fun loadAudioFiles(reset: Boolean = false) {
        if (reset) {
            currentPage = 0
            isLastPage = false
            // Don't clear list immediately to prevent UI flash ("blinding")
            // _rawAudioList.value = emptyList()

            // Only show full loading spinner if we have no data yet
            if (_rawAudioList.value.isEmpty()) {
                _isLoading.value = true
            }
        }

        if (isLastPage) return

        viewModelScope.launch {
            if (!reset) _isLoadingMore.value = true // Load More spinner for pagination

            // Pass current search query and sort option to repository
            val currentQuery = _searchQuery.value
            val currentSort = _sortOption.value

            val newItems =
                    repository.getAudioChunk(
                            limit = pageSize,
                            offset = currentPage * pageSize,
                            query = currentQuery,
                            sortOption = currentSort
                    )

            if (newItems.size < pageSize) {
                isLastPage = true
            }

            if (reset) {
                // If resetting (search/sort), REPLACE the list
                _rawAudioList.value = newItems
            } else {
                // If appending (pagination), ADD to the list
                val currentList = _rawAudioList.value.toMutableList()
                currentList.addAll(newItems)
                _rawAudioList.value = currentList
            }

            currentPage++

            _isLoading.value = false
            _isLoadingMore.value = false
        }
    }

    fun onSortOptionChanged(option: AudioSortOption) {
        _sortOption.value = option
        loadAudioFiles(reset = true)
    }

    // Debounce job for search
    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query

        // Debounce search to avoid spamming DB
        searchJob?.cancel()
        searchJob =
                viewModelScope.launch {
                    kotlinx.coroutines.delay(300) // 300ms debounce
                    loadAudioFiles(reset = true)
                }
    }

    fun loadNextPage() {
        if (!_isLoading.value && !_isLoadingMore.value && !isLastPage) {
            loadAudioFiles(reset = false)
        }
    }

    fun togglePlayback(audio: AudioModel) {
        if (_currentPlayingAudio.value?.id == audio.id) {
            // Stop playback
            stopPlayback()
        } else {
            // Start new playback
            startPlayback(audio)
        }
    }

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    // ... (existing code)

    fun seekTo(fraction: Float) {
        mediaPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val newPosition = (duration * fraction).toInt()
                player.seekTo(newPosition)
                _playbackProgress.value = fraction
            }
        }
    }

    private fun startPlayback(audio: AudioModel) {
        stopPlayback()
        try {
            mediaPlayer =
                    MediaPlayer().apply {
                        setAudioAttributes(
                                AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .build()
                        )
                        setDataSource(audio.path)
                        prepare()
                        start()
                        setOnCompletionListener { stopPlayback() }
                    }
            _currentPlayingAudio.value = audio
            startProgressTracker()
        } catch (e: IOException) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    fun stopPlayback() {
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentPlayingAudio.value = null
        _playbackProgress.value = 0f
    }

    private var progressJob: kotlinx.coroutines.Job? = null

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob =
                viewModelScope.launch {
                    while (true) {
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                try {
                                    val currentPosition = player.currentPosition.toFloat()
                                    val duration = player.duration.toFloat()
                                    if (duration > 0) {
                                        _playbackProgress.value = currentPosition / duration
                                    }
                                } catch (e: Exception) {
                                    // Handle illegal state possibly
                                }
                            }
                        }
                        kotlinx.coroutines.delay(100) // Update every 100ms
                    }
                }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun processUri(context: android.content.Context, uri: android.net.Uri): AudioModel? {
        return com.audio.mp3cutter.ringtone.maker.utils.FileUtils.getAudioFromUri(context, uri)
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
