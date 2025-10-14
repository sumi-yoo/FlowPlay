package com.sumi.flowplay.data.model

data class Playlist(
    val id: Long,
    val name: String,
    val tracks: List<Track> = mutableListOf()
)