package com.sumi.flowplay.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sumi.flowplay.data.model.TrackDto
import com.sumi.flowplay.data.datasource.JamendoRemoteDataSource
import com.sumi.flowplay.data.paging.JamendoPagingSource
import com.sumi.flowplay.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrackRepositoryImpl @Inject constructor(
    private val remoteDataSource: JamendoRemoteDataSource
) : TrackRepository {

    override fun searchTracks(query: String): Flow<PagingData<TrackDto>> {
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 5),
            pagingSourceFactory = { JamendoPagingSource(remoteDataSource, query) }
        ).flow
    }
}