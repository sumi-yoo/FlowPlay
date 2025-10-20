package com.sumi.jamplay.data.model

data class Playlist(
    val id: Long,
    val name: String,
    val tracks: List<Track> = mutableListOf()
)