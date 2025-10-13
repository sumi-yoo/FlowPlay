package com.sumi.flowplay.domain.repository

import androidx.paging.PagingData
import com.sumi.flowplay.data.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {

    fun searchTracks(query: String): Flow<PagingData<Track>>
}