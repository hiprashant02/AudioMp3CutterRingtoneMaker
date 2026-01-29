package com.audio.mp3cutter.ringtone.maker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AudioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.google.android.gms.ads.MobileAds.initialize(this) {}
    }
}
