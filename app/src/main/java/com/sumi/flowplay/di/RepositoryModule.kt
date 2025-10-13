package com.sumi.flowplay.di

import com.sumi.flowplay.data.repository.PlaylistRepositoryImpl
import com.sumi.flowplay.data.repository.TrackRepositoryImpl
import com.sumi.flowplay.domain.repository.PlaylistRepository
import com.sumi.flowplay.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTrackRepository(
        impl: TrackRepositoryImpl
    ): TrackRepository

    @Binds
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository
}