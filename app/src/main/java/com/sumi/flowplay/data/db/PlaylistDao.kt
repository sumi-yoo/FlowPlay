package com.sumi.flowplay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Transaction
    @Query("""
        SELECT * FROM playlists
        ORDER BY 
            CASE WHEN id = :favoritesId THEN 0 ELSE 1 END, 
            name ASC
       """)
    fun getAllPlaylists(favoritesId: Long): Flow<List<PlaylistWithTracks>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistById(playlistId: Long): Flow<PlaylistWithTracks>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrackCrossRef(ref: PlaylistTrackCrossRef)

    @Transaction
    suspend fun insertTrackToPlaylist(track: TrackEntity, playlistId: Long) {
        // 1. 트랙 먼저 insert
        insertTrack(track)

        // 2. 그 다음 CrossRef insert
        insertPlaylistTrackCrossRef(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = track.id
            )
        )
    }

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deleteTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Transaction
    suspend fun deletePlaylistSafe(playlistId: Long) {
        // CrossRef는 CASCADE로 자동 삭제됨
        deletePlaylist(playlistId)
    }

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Transaction
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_cross_ref r ON t.id = r.trackId
        WHERE r.playlistId = :playlistId
        ORDER BY t.name ASC
    """)
    fun getTracksOfPlaylist(playlistId: Long): Flow<List<TrackEntity>>
}