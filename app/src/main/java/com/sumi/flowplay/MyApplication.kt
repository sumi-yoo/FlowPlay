package com.sumi.flowplay

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.sumi.flowplay.service.MusicPlayerService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 음악 재생용 Notification 채널 생성
        val channel = NotificationChannel(
            MusicPlayerService.CHANNEL_ID,
            getString(R.string.channel_name_music_playback),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_desc_music_playback)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}