package com.sumi.flowplay.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import com.sumi.flowplay.MainActivity
import com.sumi.flowplay.R
import com.sumi.flowplay.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class MusicPlayerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_playback_channel"

        var instance: MusicPlayerService? = null
            private set
    }

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
    private var currentIndex: Int = 0

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var positionUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
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
            })
        }
    }

    private fun updatePlaybackState(state: Int) {
        val track = trackList.getOrNull(currentIndex) ?: return
        updateMediaMetadata(track)
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, exoPlayer.currentPosition, 1.0f)
            .setBufferedPosition(exoPlayer.bufferedPosition)
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

        val notification = NotificationCompat.Builder(this@MusicPlayerService, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_noti)
            .setContentTitle(track.name)
            .setContentText(track.artistName)
            .setContentIntent(contentIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
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
        currentIndex = tracks.indexOf(track).takeIf { it >= 0 } ?: 0
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
        playCurrent()
        currentIndex = (currentIndex + 1) % trackList.size
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
        mediaSession.release()
        exoPlayer.release()
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}