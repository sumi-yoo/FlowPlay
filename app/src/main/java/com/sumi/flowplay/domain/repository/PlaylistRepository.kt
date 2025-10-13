package com.sumi.flowplay.domain.repository

import com.sumi.flowplay.data.model.Playlist
import com.sumi.flowplay.data.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun addPlaylist(playlist: Playlist)
    suspend fun addTrackToPlaylist(playlistId: Long, track: Track)
}