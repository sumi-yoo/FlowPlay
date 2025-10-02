package com.sumi.flowplay.data.datasource

import com.sumi.flowplay.data.api.JamendoApi
import com.sumi.flowplay.data.api.JamendoTrackResponse
import javax.inject.Inject
import javax.inject.Named

class JamendoRemoteDataSource @Inject constructor(
    private val api: JamendoApi,
    @Named("JamendoClientId") private val clientId: String
) {

    suspend fun searchTracks(query: String, offset: Int, limit: Int): JamendoTrackResponse {
        return api.searchTracks(
            clientId = clientId,
            query = query,
            offset = offset,
            limit = limit,
            audioFormat = "mp31"
        )
    }
}