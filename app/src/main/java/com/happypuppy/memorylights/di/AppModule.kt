package com.happypuppy.memorylights.di

import com.happypuppy.memorylights.data.manager.SimonSoundManager
import com.happypuppy.memorylights.data.manager.StatisticsManager
import com.happypuppy.memorylights.data.repository.DataStoreSettingsRepository
import com.happypuppy.memorylights.data.repository.SettingsRepository
import com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Main application module for Dependency Injection
 */
val appModule = module {
    // Single instance of SimonSoundManager using constructor reference
    single { SimonSoundManager(androidContext()) }

    // Single instance of StatisticsManager using constructor reference
    single { StatisticsManager.fromContext(androidContext()) }

    // Settings repository for persisting game settings (uses DataStore with SharedPreferences migration)
    single<SettingsRepository> { DataStoreSettingsRepository.fromContext(androidContext()) }

    // ViewModel using the core module dsl syntax for Koin 4.x
    viewModelOf(::SimonGameViewModel)
}