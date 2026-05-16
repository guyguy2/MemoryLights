package com.happypuppy.memorylights.di

import com.happypuppy.memorylights.data.manager.SimonSoundManager
import com.happypuppy.memorylights.data.repository.DataStoreSettingsRepository
import com.happypuppy.memorylights.data.repository.SettingsRepository
import com.happypuppy.memorylights.data.repository.StatisticsRepository
import com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

/**
 * Main application module for Dependency Injection
 */
val appModule = module {
    // Single instance of SimonSoundManager using constructor reference
    single { SimonSoundManager(androidContext()) }

    // Single instance of StatisticsRepository using constructor reference.
    // `onClose` cancels the repo's owned IO scope when Koin releases the instance
    // (e.g. `stopKoin()` in tests). Process death on real devices already cleans
    // up unowned scopes; this hook covers the test + reset path.
    single { StatisticsRepository.fromContext(androidContext()) }.withOptions {
        onClose { it?.close() }
    }

    // Settings repository for persisting game settings (uses DataStore with SharedPreferences migration)
    single<SettingsRepository> {
        DataStoreSettingsRepository.fromContext(androidContext())
    }.withOptions {
        onClose { it?.close() }
    }

    // ViewModel using the core module dsl syntax for Koin 4.x
    viewModelOf(::SimonGameViewModel)
}