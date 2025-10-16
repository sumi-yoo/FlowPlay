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

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode: StateFlow<Boolean> = _isShuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    val playerCommand = MutableSharedFlow<PlayerCommand>(replay = 1)

    sealed class PlayerCommand {
        data class Play(val track: Track, val tracks: List<Track>) : PlayerCommand()
        data object TogglePlay : PlayerCommand()
        data object SkipNext : PlayerCommand()
        data object SkipPrevious : PlayerCommand()
        data class Seek(val position: Long) : PlayerCommand()
        data object ToggleShuffle : PlayerCommand()
        data object ToggleRepeat : PlayerCommand()
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
            service.isShuffleMode.collect { _isShuffleMode.value = it }
        }
        viewModelScope.launch {
            service.repeatMode.collect { _repeatMode.value = it }
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

    fun toggleShuffle() {
        playerCommand.tryEmit(PlayerCommand.ToggleShuffle)
    }

    fun toggleRepeat() {
        playerCommand.tryEmit(PlayerCommand.ToggleRepeat)
    }
}