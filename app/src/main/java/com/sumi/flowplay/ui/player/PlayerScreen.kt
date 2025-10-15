package com.sumi.flowplay.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.sumi.flowplay.R
import com.sumi.flowplay.ui.playlist.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onAddToPlaylist: () -> Unit,
    onBack: () -> Unit
) {
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    if (currentTrack == null) return

    val favoritesPlaylistId = stringResource(R.string.favorites_playlist_name).hashCode().toLong() // 좋아요 플레이리스트 Id
    val isFavorite = playlistViewModel.isTrackInPlaylist(favoritesPlaylistId, currentTrack!!.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { "" },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = rememberAsyncImagePainter(currentTrack!!.artworkUrl),
                contentDescription = currentTrack!!.name,
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(currentTrack!!.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(currentTrack!!.artistName, fontSize = 18.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))

            CustomProgressBar(
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { pos -> playerViewModel.seekTo(pos) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPosition))
                Text(formatTime(duration))
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { playerViewModel.skipPrevious() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.padding(0.dp).size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.padding(0.dp).size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { playerViewModel.skipNext() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.padding(0.dp).size(48.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 왼쪽: 좋아요 버튼
                IconButton(
                    onClick = {
                        if (isFavorite) {
                            playlistViewModel.deleteTrackFromPlaylist(
                                favoritesPlaylistId,
                                currentTrack!!
                            )
                        } else {
                            playlistViewModel.addTrackToPlaylist(
                                favoritesPlaylistId,
                                currentTrack!!
                            )
                        }
                    }
                ) {
                    Icon(
                        modifier = Modifier.padding(0.dp),
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "좋아요 해제" else "좋아요",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 오른쪽: 플레이리스트 추가 버튼
                IconButton(
                    onClick = { onAddToPlaylist() }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Add to Playlist",
                        modifier = Modifier.padding(0.dp).size(28.dp)
                    )
                }
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

@Composable
fun PlayingWave(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    barCount: Int = 5,
    barWidth: Dp = 4.dp,
    barMaxHeight: Dp = 24.dp,
    barMinHeight: Dp = 6.dp,
    barSpacing: Dp = 2.dp,
    color: Color = Color.Green
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(barMaxHeight)
    ) {
        repeat(barCount) { index ->
            var lastHeight by remember { mutableStateOf(barMinHeight.value) }

            val infiniteTransition = rememberInfiniteTransition()
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = barMinHeight.value,
                targetValue = barMaxHeight.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + index * 100,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val height = if (isPlaying) animatedHeight else lastHeight
            lastHeight = height

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height.dp)
                    .background(color, shape = RoundedCornerShape(50))
            )
        }
    }
}