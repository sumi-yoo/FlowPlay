package com.sumi.flowplay.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.res.stringResource
import com.sumi.flowplay.R
import com.sumi.flowplay.ui.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectScreen(
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val playlists by playlistViewModel.playlists.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // 선택 상태: playlistId -> Boolean
    val selectedPlaylists = remember { mutableStateMapOf<Long, Boolean>() }

    // 체크 상태 초기화
    LaunchedEffect(playlists, currentTrack) {
        playlists.forEach { playlist ->
            val alreadyIn = currentTrack?.let { playlistViewModel.isTrackInPlaylist(playlist.id, it.id) } ?: false
            selectedPlaylists[playlist.id] = alreadyIn
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlist_add_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        currentTrack?.let { track ->
                            val (selected, unselected) = selectedPlaylists.entries.partition { it.value }

                            // 체크된 플레이리스트 → 추가
                            selected.forEach { (playlistId, _) ->
                                playlistViewModel.addTrackToPlaylist(playlistId, track)
                            }

                            // 체크 해제된 플레이리스트 → 삭제
                            unselected.forEach { (playlistId, _) ->
                                playlistViewModel.deleteTrackFromPlaylist(playlistId, track)
                            }
                        }
                        onBack()
                    }) {
                        Text(stringResource(R.string.complete))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_playlists),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.select_playlist),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }

                items(playlists) { playlist ->
                    val checked = selectedPlaylists[playlist.id] ?: false
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPlaylists[playlist.id] = !(selectedPlaylists[playlist.id] ?: false)
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.playlist_track_count, playlist.tracks.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(
                                modifier = Modifier.padding(5.dp).size(20.dp),
                                checked = checked,
                                onCheckedChange = { selectedPlaylists[playlist.id] = it }
                            )
                        }
                    }
                }
            }

            // 새 플레이리스트 버튼
            item {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.create_new_playlist))
                }
            }
        }
    }

    // 새 플레이리스트 생성 다이얼로그
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.new_playlist_dialog_title)) },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        playlistViewModel.addPlaylist(newPlaylistName)
                        selectedPlaylists[newPlaylistName.hashCode().toLong()] = true
                        newPlaylistName = ""
                        showCreateDialog = false
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}