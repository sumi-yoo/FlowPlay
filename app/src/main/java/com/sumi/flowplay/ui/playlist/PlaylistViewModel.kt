package com.sumi.flowplay.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumi.flowplay.data.model.Playlist
import com.sumi.flowplay.data.model.Track
import com.sumi.flowplay.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> =
        repository.getAllPlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean {
        return playlists.value.firstOrNull { it.id == playlistId }?.tracks?.any { it.id == trackId } == true
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun addPlaylist(name: String) {
        val playlistId = name.hashCode().toLong()
        viewModelScope.launch {
            repository.addPlaylist(Playlist(id = playlistId, name = name))
        }
    }

    fun deleteTrackFromPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.deleteTrackFromPlaylist(playlistId, track)
        }
    }
}