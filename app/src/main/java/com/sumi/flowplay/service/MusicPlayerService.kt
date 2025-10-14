package com.sumi.flowplay.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sumi.flowplay.data.model.TrackDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerService : Service() {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)  // 리스너 등록
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) skipNext()
            _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        }
    }
    private val binder = LocalBinder()
    private var trackList: List<TrackDto> = emptyList()
    private var currentIndex: Int = 0

    private val _currentTrack = MutableStateFlow<TrackDto?>(null)
    val currentTrack: StateFlow<TrackDto?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

//    private val _currentPosition = MutableStateFlow(0L)
//    val currentPosition: StateFlow<Long> get() = _currentPosition
//
//    private val _duration = MutableStateFlow(0L)
//    val duration: StateFlow<Long> get() = _duration

    override fun onCreate() {
        super.onCreate()
        startUpdatingPosition()
    }

    private fun startUpdatingPosition() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                _currentPosition.value = exoPlayer.currentPosition
                _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                delay(200)
            }
        }
    }

    fun play(track: TrackDto, tracks: List<TrackDto>) {
        trackList = tracks
        currentIndex = tracks.indexOf(track).takeIf { it >= 0 } ?: 0
        Log.d("테스트", "play")
        playCurrent()
    }

    private fun playCurrent() {
        val track = trackList.getOrNull(currentIndex) ?: return
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(track.streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        _isPlaying.value = true
        _currentTrack.value = track
        Log.d("테스트", "playCurrent")
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
        Log.d("테스트", "togglePlayPause")
    }

    fun skipNext() {
        if (trackList.isEmpty()) return
        playCurrent()
        currentIndex = (currentIndex + 1) % trackList.size
        Log.d("테스트", "skipNext")
    }

    fun skipPrevious() {
        if (trackList.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) trackList.size - 1 else currentIndex - 1
        Log.d("테스트", "skipPrevious")
        playCurrent()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        Log.d("테스트", "seekTo")
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        exoPlayer.release()
        super.onDestroy()
    }
}