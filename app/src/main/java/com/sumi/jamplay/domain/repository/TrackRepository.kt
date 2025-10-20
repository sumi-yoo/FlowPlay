package com.sumi.jamplay.domain.repository

import androidx.paging.PagingData
import com.sumi.jamplay.data.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {

    fun searchTracks(query: String): Flow<PagingData<Track>>
}