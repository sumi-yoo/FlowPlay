package com.sumi.flowplay.ui.playlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumi.flowplay.R
import com.sumi.flowplay.data.model.Playlist
import com.sumi.flowplay.data.model.Track
import com.sumi.flowplay.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val favoritesId = context.getString(R.string.favorites_playlist_name).hashCode().toLong()

    val playlists: StateFlow<List<Playlist>> =
        repository.getAllPlaylists(favoritesId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 선택된 플레이리스트 */
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist

    private val _isDeleteMode = MutableStateFlow(false)
    val isDeleteMode: StateFlow<Boolean> = _isDeleteMode

    fun selectPlaylistById(playlistId: Long) {
        viewModelScope.launch {
            repository.getPlaylistById(playlistId)
                .collect { playlist ->
                    _selectedPlaylist.value = playlist
                }
        }
    }

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

    fun getTracksOfPlaylist(playlistId: Long): Flow<List<Track>> {
        return repository.getTracksOfPlaylist(playlistId)
    }

    fun toggleDeleteMode() {
        _isDeleteMode.value = !_isDeleteMode.value
    }

    fun deletePlaylists(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deletePlaylist(id)
            }
        }
    }
}