package com.sumi.jamplay.data.datasource

import com.sumi.jamplay.data.api.JamendoApi
import com.sumi.jamplay.data.model.JamendoTrackResponse
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
            audioFormat = "mp31",
            license = "by,by-sa"
        )
    }
}