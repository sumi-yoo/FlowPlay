package com.sumi.flowplay.data.db

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long
)