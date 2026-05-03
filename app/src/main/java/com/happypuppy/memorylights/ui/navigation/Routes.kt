package com.happypuppy.memorylights.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route objects for Compose Navigation.
 *
 * Each destination is an `@Serializable` object so navigation calls and
 * `composable<>` builders can be generic over the route type — no string
 * paths, no NavArguments. See:
 * https://developer.android.com/guide/navigation/design/type-safety
 */
@Serializable
object GameRoute

@Serializable
object SettingsRoute

@Serializable
object StatisticsRoute

@Serializable
object GameModesRoute

@Serializable
object GameplayRoute

@Serializable
object SoundAndHapticsRoute
