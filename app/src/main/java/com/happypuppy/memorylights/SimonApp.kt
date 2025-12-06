package com.happypuppy.memorylights

import android.app.Application
import com.happypuppy.memorylights.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SimonApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            // Use Android logger with appropriate log level
            androidLogger(Level.ERROR)

            // Provide Android context
            androidContext(this@SimonApp)

            // Register all modules
            modules(appModule)
        }
    }
}