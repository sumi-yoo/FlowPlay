package com.sumi.jamplay.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumi.jamplay.R
import com.sumi.jamplay.data.model.Track
import com.sumi.jamplay.ui.player.PlayerViewModel
import com.sumi.jamplay.ui.player.PlayingWave

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
    onTrackClick: (Track, List<Track>) -> Unit,
    onBack: () -> Unit
) {
    val playlist by playlistViewModel.selectedPlaylist.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val tracks by playlistViewModel.getTracksOfPlaylist(playlistId).collectAsState(initial = emptyList())
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    if (playlist == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = playlist!!.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) },
                navigationIcon = {
                    IconButton(onClick = {
                        playlistViewModel.updateDeleteTrackMode(false)
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (playlistViewModel.deleteTrackMode) {
                        TextButton(onClick = {
                            // 체크된 트랙 삭제
                            val toDelete = playlistViewModel.deletedTracks.filter { it.value }.keys
                            toDelete.forEach { trackId ->
                                tracks.find { it.id == trackId }?.let { playlistViewModel.deleteTrackFromPlaylist(playlistId, it) }
                            }
                            playlistViewModel.clearSelectionTracks()
                            playlistViewModel.updateDeleteTrackMode(false)
                        }) {
                            Text(stringResource(R.string.complete))
                        }
                    } else {
                        TextButton(onClick = { playlistViewModel.updateDeleteTrackMode(true) }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_tracks_in_playlist),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(tracks) { track ->
                    val isCurrentTrack = currentTrack?.id == track.id
                    val isSelected = playlistViewModel.deletedTracks[track.id] ?: false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                if (playlistViewModel.deleteTrackMode) {
                                    playlistViewModel.deletedTracks[track.id] = !(playlistViewModel.deletedTracks[track.id] ?: false)
                                } else {
                                    onTrackClick(track, tracks)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(64.dp)) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                                )

                                if (isCurrentTrack) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    )
                                    // 재생중 표시
                                    PlayingWave(
                                        isPlaying = isPlaying,
                                        barCount = 5,
                                        barWidth = 3.dp,
                                        barMaxHeight = 24.dp,
                                        barMinHeight = 6.dp,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (playlistViewModel.deleteTrackMode) {
                                Checkbox(
                                    modifier = Modifier.padding(5.dp).size(20.dp),
                                    checked = isSelected,
                                    onCheckedChange = { playlistViewModel.deletedTracks[track.id] = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}