package com.sumi.flowplay.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import com.sumi.flowplay.MainActivity
import com.sumi.flowplay.R
import com.sumi.flowplay.data.datastore.PlayerPreferencesDataStore
import com.sumi.flowplay.data.model.Track
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_playback_channel"

        var instance: MusicPlayerService? = null
            private set
    }

    @Inject lateinit var playerPrefs: PlayerPreferencesDataStore

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var mediaSession: MediaSessionCompat

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) skipNext()
            _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        }
    }

    private var trackList: List<Track> = emptyList()
    private var originalTrackList: List<Track> = emptyList()
    private var currentIndex: Int = 0

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode: StateFlow<Boolean> = _isShuffleMode.asStateFlow()

    private val _isRepeatMode = MutableStateFlow(0)
    val isRepeatMode: StateFlow<Int> = _isRepeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var positionUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        serviceScope.launch {
            playerPrefs.shuffleMode.collect { value ->
                _isShuffleMode.value = value
            }
            playerPrefs.repeatMode.collect { value ->
                _isRepeatMode.value = value
            }
        }
        createMediaSession()
        startUpdatingPosition()
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    togglePlayPause()
                    showForegroundNotification()
                }
                override fun onPause() {
                    togglePlayPause()
                    showForegroundNotification()
                }
                override fun onSkipToNext() {
                    skipNext()
                    showForegroundNotification()
                }
                override fun onSkipToPrevious() {
                    skipPrevious()
                    showForegroundNotification()
                }
                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                    showForegroundNotification()
                }
                override fun onCustomAction(action: String?, extras: Bundle?) {
                    when (action) {
                        "ACTION_TOGGLE_SHUFFLE" -> {
                            toggleShuffle()
                            showForegroundNotification()
                        }
                        "ACTION_TOGGLE_REPEAT" -> {
                            if (_isRepeatMode.value == 2) _isRepeatMode.value = 0;
                            else _isRepeatMode.value++
                            showForegroundNotification()
                        }
                    }
                }
            })
        }
    }

    private fun updatePlaybackState(state: Int) {
        val track = trackList.getOrNull(currentIndex) ?: return
        updateMediaMetadata(track)

        val shuffleAction = PlaybackStateCompat.CustomAction.Builder(
            "ACTION_TOGGLE_SHUFFLE", "Shuffle",
            if (_isShuffleMode.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        ).build()

        val repeatAction = PlaybackStateCompat.CustomAction.Builder(
            "ACTION_TOGGLE_REPEAT", "Repeat",
            if (_isRepeatMode.value == 1) {
                R.drawable.ic_repeat_on
            } else if (_isRepeatMode.value == 2) {
                R.drawable.ic_repeat_one_on
            } else {
                R.drawable.ic_repeat
            }
        ).build()

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, exoPlayer.currentPosition, 1.0f)
            .setBufferedPosition(exoPlayer.bufferedPosition)
            .addCustomAction(shuffleAction)
            .addCustomAction(repeatAction)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun showForegroundNotification() {
        val track = _currentTrack.value ?: return

        val notificationIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shuffleAction = NotificationCompat.Action(
            if (_isShuffleMode.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle,
            "Shuffle",
            PendingIntent.getService(
                this, 0,
                Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_TOGGLE_SHUFFLE" },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val repeatAction = NotificationCompat.Action(
            if (_isRepeatMode.value == 1) {
                R.drawable.ic_repeat_on
            } else if (_isRepeatMode.value == 2) {
                R.drawable.ic_repeat_one_on
            } else {
                R.drawable.ic_repeat
            },
            "Repeat",
            PendingIntent.getService(
                this, 1,
                Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_TOGGLE_REPEAT" },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val notification = NotificationCompat.Builder(this@MusicPlayerService, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_noti)
            .setContentTitle(track.name)
            .setContentText(track.artistName)
            .setContentIntent(contentIntent)
            // 버튼 추가 순서 = 화면 표시 순서
            .addAction(shuffleAction)   // 0
            .addAction(repeatAction)    // 1
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                     .setShowActionsInCompactView(0, 1)
            )
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startUpdatingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                _currentPosition.value = exoPlayer.currentPosition
                _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L

                updatePlaybackState(
                    if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED
                )
                showForegroundNotification()

                delay(200)
            }
        }
    }

    private fun updateMediaMetadata(track: Track) {
        serviceScope.launch {
            val albumArtBitmap = track.artworkUrl?.let { loadAlbumArtBitmap(it) }

            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artistName)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap)
                .build()
            mediaSession.setMetadata(metadata)
        }
    }

    fun play(track: Track, tracks: List<Track>) {
        trackList = tracks
        originalTrackList = tracks
        currentIndex = tracks.indexOf(track).takeIf { it >= 0 } ?: 0

        if (_isShuffleMode.value) {
            val current = trackList.firstOrNull { it.id == track.id } ?: return
            val shuffled = trackList.filterNot { it.id == track.id }.shuffled()
            trackList = buildList {
                add(current) // 현재곡을 0번째 위치에 둠
                addAll(shuffled)
            }
            currentIndex = 0
        }
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

        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        showForegroundNotification()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        } else {
            exoPlayer.play()
            _isPlaying.value = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        showForegroundNotification()
    }

    fun skipNext() {
        if (trackList.isEmpty()) return
        currentIndex = (currentIndex + 1) % trackList.size
        playCurrent()
    }

    fun skipPrevious() {
        if (trackList.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) trackList.size - 1 else currentIndex - 1
        playCurrent()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        updatePlaybackState(
            if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        )
    }

    fun toggleShuffle() {
        if (trackList.isEmpty()) return

        val currentTrack = _currentTrack.value ?: return

        if (_isShuffleMode.value) {
            // 셔플 해제: 원래 순서로 복원
            val currentId = currentTrack.id
            trackList = originalTrackList
            currentIndex = trackList.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            _isShuffleMode.value = false
            serviceScope.launch {
                playerPrefs.setShuffleMode(false)
            }
        } else {
            // 셔플 켜기: 현재 트랙 유지하고 나머지 무작위로 섞기
            originalTrackList = trackList // 원본 저장
            val currentId = currentTrack.id
            val current = trackList.firstOrNull { it.id == currentId } ?: return

            val shuffled = trackList.filterNot { it.id == currentId }.shuffled()
            trackList = buildList {
                add(current) // 현재곡을 0번째 위치에 둠
                addAll(shuffled)
            }
            currentIndex = 0
            _isShuffleMode.value = true
            serviceScope.launch {
                playerPrefs.setShuffleMode(true)
            }
        }
    }

    suspend fun loadAlbumArtBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader.Builder(applicationContext)
                .build()
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        positionUpdateJob?.cancel()

        mediaSession.run {
            setCallback(null)  // 콜백 제거 → MediaController가 더 이상 접근하지 않음
            try {
                release()      // 세션 해제
            } catch (e: DeadObjectException) {
                // 세션이 죽어도 안전하게 무시
            }
        }

        exoPlayer.release()
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}