package com.sumi.flowplay.di

import com.sumi.flowplay.data.api.JamendoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")  // ✅ 마지막에 / 반드시
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideJamendoApi(retrofit: Retrofit): JamendoApi {
        return retrofit.create(JamendoApi::class.java)
    }

    @Provides
    @Named("JamendoClientId")
    fun provideClientId(): String = "3f95f339"
}