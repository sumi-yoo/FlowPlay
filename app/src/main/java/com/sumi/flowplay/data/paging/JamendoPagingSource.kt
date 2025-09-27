package com.sumi.flowplay.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sumi.flowplay.data.model.TrackDto
import com.sumi.flowplay.data.datasource.JamendoRemoteDataSource

class JamendoPagingSource(
    private val remoteDataSource: JamendoRemoteDataSource,
    private val query: String
) : PagingSource<Int, TrackDto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TrackDto> {
        val page = params.key ?: 1
        return try {
            val perPage = params.loadSize
            val response = remoteDataSource.searchTracks(query, page, perPage)
            val tracks = response.results.map {
                TrackDto(
                    id = it.id,
                    name = it.name,
                    artistName = it.artistName,
                    albumName = it.albumName,
                    artworkUrl = it.artworkUrl,
                    streamUrl = it.audioUrl
                )
            }

            LoadResult.Page(
                data = tracks,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (tracks.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TrackDto>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.nextKey?.minus(1)
                ?: state.closestPageToPosition(anchor)?.prevKey?.plus(1)
        }
    }
}