package com.sumi.flowplay.data.datasource

import com.sumi.flowplay.data.api.JamendoApi
import com.sumi.flowplay.data.api.JamendoTrackResponse
import javax.inject.Inject
import javax.inject.Named

class JamendoRemoteDataSource @Inject constructor(
    private val api: JamendoApi,
    @Named("JamendoClientId") private val clientId: String
) {

    suspend fun searchTracks(query: String, page: Int, perPage: Int): JamendoTrackResponse {
        val offset = (page - 1) * perPage

        return api.searchTracks(
            clientId = clientId,
            query = query,
            offset = offset,
            limit = perPage,
            audioFormat = "mp31"
        )
    }
}