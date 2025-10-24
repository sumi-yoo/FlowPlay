package com.sumi.jamplay.ui.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumi.jamplay.data.model.Playlist
import com.sumi.jamplay.data.model.Track
import com.sumi.jamplay.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _favoritesId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlists: StateFlow<List<Playlist>> = _favoritesId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getAllPlaylists(id)
            } else {
                flowOf(emptyList()) // null이면 빈 리스트
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 선택된 플레이리스트 */
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist

    private val _deletePlayListMode = MutableStateFlow(false)
    val deletePlayListMode: StateFlow<Boolean> = _deletePlayListMode

    var showCreateDialog by mutableStateOf(false)
        private set   // 외부에서 직접 set 불가

    var newPlaylistName by mutableStateOf("")
        private set

    val selectedPlaylists = mutableStateMapOf<Long, Boolean>()

    val deletedPlaylists = mutableStateMapOf<Long, Boolean>()

    // 선택 모드 상태
    var deleteTrackMode by mutableStateOf(false)
        private set

    val deletedTracks = mutableStateMapOf<Long, Boolean>()

    var acceptsClicks by mutableStateOf(true)
        private set

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
        val trimmedName = name.trim()
        val playlistId = trimmedName.hashCode().toLong()
        viewModelScope.launch {
            repository.addPlaylist(Playlist(id = playlistId, name = trimmedName))
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

    fun toggleDeletePlayListMode() {
        _deletePlayListMode.value = !_deletePlayListMode.value
    }

    fun deletePlaylists(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deletePlaylist(id)
            }
        }
    }

    fun setFavoritesId(favoritesName: String) {
        _favoritesId.value = favoritesName.hashCode().toLong()
    }

    fun updateShowCreateDialog(show: Boolean) {
        showCreateDialog = show
    }

    fun updateNewPlaylistName(name: String) {
        newPlaylistName = name
    }

    fun clearSelectedPlaylists() {
        selectedPlaylists.clear()
    }

    fun clearSelectionPlaylists() {
        deletedPlaylists.clear()
    }

    fun updateDeleteTrackMode(enabled: Boolean) {
        deleteTrackMode = enabled
    }

    fun clearSelectionTracks() {
        deletedTracks.clear()
    }

    fun enableClicks() { acceptsClicks = true }

    fun disableClicks() { acceptsClicks = false }
}