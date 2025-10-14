package com.sumi.flowplay.domain.repository

import com.sumi.flowplay.data.model.Playlist
import com.sumi.flowplay.data.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(playlistId: Long): Flow<Playlist>
    suspend fun addPlaylist(playlist: Playlist)
    suspend fun addTrackToPlaylist(playlistId: Long, track: Track)
    suspend fun deleteTrackFromPlaylist(playlistId: Long, track: Track)
    suspend fun deletePlaylist(playlistId: Long)
    fun getTracksOfPlaylist(playlistId: Long): Flow<List<Track>>
}