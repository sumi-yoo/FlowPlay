package com.sumi.flowplay.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackDto(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumName: String?,
    val artworkUrl: String?,
    val streamUrl: String
) : Parcelable