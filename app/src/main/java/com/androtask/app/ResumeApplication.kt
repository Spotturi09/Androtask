package com.androtask.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResumeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.ADS_ENABLED) {
            // Initialise off the main thread to keep app start fast.
            CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(this@ResumeApplication) }
        }
    }
}
