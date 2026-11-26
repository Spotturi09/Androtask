package com.androtask.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ResumeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.ADS_ENABLED) AdsInit.start(this)
    }
}

/**
 * The Ads SDK must finish initialising before any AdView is created or loaded.
 * Creating a banner while MobileAds.initialize() is still running on a background thread
 * deadlocks: the SDK holds an internal lock while waiting for WebView setup on the main
 * thread, and the main thread blocks on that lock inside AdView.loadAd(). The app then
 * never draws its first frame (stuck on the splash screen).
 */
object AdsInit {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun start(app: Application) {
        Thread({
            MobileAds.initialize(app) { _ready.value = true }
        }, "ads-init").start()
    }
}
