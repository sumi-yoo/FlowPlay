package com.sumi.flowplay.data.model

data class TrackDto(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumName: String?,
    val artworkUrl: String?,
    val streamUrl: String
)