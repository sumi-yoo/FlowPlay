package com.sumi.flowplay.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApi {
    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,          // 페이지 계산용 offset
        @Query("search") query: String? = null,    // 검색어
        @Query("audioformat") audioFormat: String = "mp31"
    ): JamendoTrackResponse
}

data class JamendoTrackResponse(
    val headers: Map<String, Any>,
    @SerializedName("results") val results: List<JamendoTrack>
)

data class JamendoTrack(
    val id: Long,
    val name: String,
    @SerializedName("artist_name") val artistName: String,
    @SerializedName("album_name") val albumName: String?,
    @SerializedName("audio") val audioUrl: String,
    @SerializedName("album_image") val artworkUrl: String?,
    @SerializedName("duration") val duration: Int?,          // 트랙 길이 (초)
    @SerializedName("license_ccurl") val licenseUrl: String?, // 라이선스 URL
    @SerializedName("releasedate") val releaseDate: String?  // 발매일
)