package com.sumi.flowplay.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sumi.flowplay.data.model.TrackDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val exoPlayer: ExoPlayer
) : ViewModel() {

    private val _trackList = MutableStateFlow<List<TrackDto>>(emptyList())
    val trackList: StateFlow<List<TrackDto>> = _trackList.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val currentTrack: StateFlow<TrackDto?> = _currentIndex.map { idx ->
        _trackList.value.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private var playbackJob: Job? = null

    init {
        playbackJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = exoPlayer.currentPosition
                delay(200)
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                skipNext()
            }
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    fun play(track: TrackDto, tracks: List<TrackDto>) {
        _trackList.value = tracks
        _currentIndex.value = tracks.indexOf(track).takeIf { it >= 0 } ?: 0
        playCurrent()
    }

    private fun playCurrent() {
        val track = currentTrack.value ?: return
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(track.streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun skipNext() {
        val list = _trackList.value
        if (list.isEmpty()) return
        _currentIndex.value = (_currentIndex.value + 1) % list.size
        playCurrent()
    }

    fun skipPrevious() {
        val list = _trackList.value
        if (list.isEmpty()) return
        _currentIndex.value = if (_currentIndex.value - 1 < 0) list.size - 1 else _currentIndex.value - 1
        playCurrent()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }
}