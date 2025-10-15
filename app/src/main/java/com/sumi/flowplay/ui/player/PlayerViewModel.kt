package com.sumi.flowplay.ui.player

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.sumi.flowplay.data.model.Track
import com.sumi.flowplay.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    val playerCommand = MutableSharedFlow<PlayerCommand>(replay = 1)

    sealed class PlayerCommand {
        data class Play(val track: Track, val tracks: List<Track>) : PlayerCommand()
        object TogglePlay : PlayerCommand()
        object SkipNext : PlayerCommand()
        object SkipPrevious : PlayerCommand()
        data class Seek(val position: Long) : PlayerCommand()
    }

    @OptIn(UnstableApi::class)
    fun bindService(service: MusicPlayerService) {
        // 서비스 상태를 그대로 구독
        viewModelScope.launch {
            service.currentTrack.collect { _currentTrack.value = it }
        }
        viewModelScope.launch {
            service.isPlaying.collect { _isPlaying.value = it }
        }
        viewModelScope.launch {
            service.currentPosition.collect { _currentPosition.value = it }
        }
        viewModelScope.launch {
            service.duration.collect { _duration.value = it }
        }
    }

    fun play(track: Track, tracks: List<Track>) {
        playerCommand.tryEmit(PlayerCommand.Play(track, tracks))
    }

    fun togglePlayPause() {
        playerCommand.tryEmit(PlayerCommand.TogglePlay)
    }

    fun skipNext() {
        playerCommand.tryEmit(PlayerCommand.SkipNext)
    }

    fun skipPrevious() {
        playerCommand.tryEmit(PlayerCommand.SkipPrevious)
    }

    fun seekTo(position: Long) {
        playerCommand.tryEmit(PlayerCommand.Seek(position))
    }
}