package com.sumi.flowplay.ui.play

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import com.sumi.flowplay.data.model.TrackDto
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    track: TrackDto,
    trackList: List<TrackDto>,
) {
    val context = LocalContext.current

    // 현재 재생 중인 트랙 인덱스 상태
    var currentIndex by remember { mutableStateOf(trackList.indexOf(track)) }

    // currentIndex 변화에 따라 currentTrack 계산
    val currentTrack by remember(currentIndex) {
        derivedStateOf { trackList.getOrNull(currentIndex) ?: trackList.first() }
    }

    // ExoPlayer 세팅
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }

    val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L

    // currentTrack 변화 시 자동 재생
    LaunchedEffect(currentTrack) {
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(currentTrack.streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        isPlaying = true
    }

    // 화면 나가면 ExoPlayer 정리
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    // 재생 상태 업데이트
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            delay(200)
        }
    }

    // 트랙 종료 감지 → 다음곡 자동 재생
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    if (currentIndex < trackList.size - 1) {
                        currentIndex += 1
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        Image(
            painter = rememberAsyncImagePainter(currentTrack.artworkUrl),
            contentDescription = currentTrack.name,
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Text(currentTrack.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(currentTrack.artistName, fontSize = 18.sp, color = Color.Gray)

        CustomProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            onSeek = { pos -> exoPlayer.seekTo(pos) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition))
            Text(formatTime(exoPlayer.duration.takeIf { it > 0 } ?: 0L))
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    currentIndex = if (currentIndex - 1 < 0) trackList.size - 1 else currentIndex - 1
                }
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
            }

            IconButton(
                onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                }
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = {
                    currentIndex = (currentIndex + 1) % trackList.size
                }
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun CustomProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(6.dp)
            .background(Color.White, RectangleShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((newProgress * duration).toLong())
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .background(Color(0xFF1DB954), RectangleShape)
        )
    }
}