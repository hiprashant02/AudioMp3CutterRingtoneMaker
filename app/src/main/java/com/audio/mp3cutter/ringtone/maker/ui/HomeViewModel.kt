package com.audio.mp3cutter.ringtone.maker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audio.mp3cutter.ringtone.maker.data.AudioRepository
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<AudioModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRecentProjects()
    }

    fun loadRecentProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val projects = audioRepository.getRecentProjects(limit = 5)
                _uiState.value = _uiState.value.copy(
                    recentProjects = projects,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refreshProjects() {
        loadRecentProjects()
    }
}
