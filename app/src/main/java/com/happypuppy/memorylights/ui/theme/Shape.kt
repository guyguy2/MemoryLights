package com.happypuppy.memorylights.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Harmonious Material 3 shape scale for Memory Lights.
 *
 * Follows the nested corner radius rule (`outer = inner + padding`) across
 * game panels (`large` 16.dp outer / `medium` 12.dp inner face), cards
 * (`medium` 12.dp), badges/chips (`small` 8.dp), and primary CTAs
 * (`extraLarge` 28.dp).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
