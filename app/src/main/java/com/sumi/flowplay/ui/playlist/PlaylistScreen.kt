package com.sumi.flowplay.ui.playlist

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumi.flowplay.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    onPlaylistClick: (Long) -> Unit
) {
    val playlists by playlistViewModel.playlists.collectAsState()
    val isDeleteMode by playlistViewModel.deletePlayListMode.collectAsState()
    val favoritesName = stringResource(R.string.favorites_playlist_name)

    LaunchedEffect(Unit) {
        playlistViewModel.setFavoritesId(favoritesName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlist_title)) },
                actions = {
                    if (isDeleteMode) {
                        TextButton(onClick = {
                            val idsToDelete = playlistViewModel.deletedPlaylists.filterValues { it }.keys
                            playlistViewModel.deletePlaylists(idsToDelete.toList())
                            playlistViewModel.clearSelectionPlaylists()
                            playlistViewModel.toggleDeletePlayListMode()
                        }) {
                            Text(stringResource(R.string.complete))
                        }
                    } else {
                        var expanded by rememberSaveable { mutableStateOf(false) }

                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기"
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                offset = DpOffset(x = (-5).dp, y = 0.dp) // 왼쪽으로 5dp 이동
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.add)) },
                                    onClick = {
                                        expanded = false
                                        playlistViewModel.updateShowCreateDialog(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    onClick = {
                                        expanded = false
                                        playlistViewModel.clearSelectionPlaylists()
                                        playlistViewModel.toggleDeletePlayListMode()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_playlists),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn {
                    items(playlists) { playlist ->
                        val checked = playlistViewModel.deletedPlaylists[playlist.id] ?: false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable(enabled = !isDeleteMode) {
                                    if (!isDeleteMode) onPlaylistClick(playlist.id)
                                },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (playlist.tracks.isEmpty()) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    AsyncImage(
                                        model = playlist.tracks.first().artworkUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.playlist_track_count,
                                            playlist.tracks.size
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isDeleteMode && playlist.id != favoritesName.hashCode().toLong()) {
                                    Checkbox(
                                        modifier = Modifier.padding(5.dp).size(20.dp),
                                        checked = checked,
                                        onCheckedChange = {
                                            playlistViewModel.deletedPlaylists[playlist.id] = it
                                        }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 새 플레이리스트 생성 다이얼로그
    if (playlistViewModel.showCreateDialog) {
        var showError by rememberSaveable { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { playlistViewModel.updateShowCreateDialog(false) },
            title = { Text(stringResource(R.string.new_playlist_dialog_title)) },
            text = {
                Column {
                    TextField(
                        value = playlistViewModel.newPlaylistName,
                        onValueChange = {
                            playlistViewModel.updateNewPlaylistName(it)
                            showError = false
                        },
                        label = { Text(stringResource(R.string.playlist_name_label)) },
                        singleLine = true
                    )
                    if (showError) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.playlist_name_exists), // "이미 존재하는 이름입니다" 같은 문자열
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                val existing = playlists.any { it.name == playlistViewModel.newPlaylistName.trim() }
                TextButton(onClick = {
                    if (playlistViewModel.newPlaylistName.isNotBlank() && !existing) {
                        playlistViewModel.addPlaylist(playlistViewModel.newPlaylistName)
                        playlistViewModel.updateNewPlaylistName("")
                        playlistViewModel.updateShowCreateDialog(false)
                    } else {
                        showError = true
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    playlistViewModel.updateNewPlaylistName("")
                    playlistViewModel.updateShowCreateDialog(false)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}