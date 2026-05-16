package com.happypuppy.memorylights

import android.app.Application
import com.happypuppy.memorylights.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
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

    /**
     * `onTerminate` is invoked only on the emulator (never on real devices), but
     * wiring it lets `getKoin().close()` release the IO scopes held by the
     * settings/statistics repositories during instrumented tests and avoids the
     * scope-leak audit finding (improvements #24). Process death handles the
     * real-device path; this is the deterministic cleanup hook.
     */
    override fun onTerminate() {
        GlobalContext.getOrNull()?.close()
        super.onTerminate()
    }
}