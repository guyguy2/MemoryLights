package com.happypuppy.memorylights.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.domain.enums.ColorBlindShape
import com.happypuppy.memorylights.domain.enums.SimonButton

/**
 * Colored-panel grid for the Simon game screen — extracted from
 * `SimonGameScreen` (#17) so the screen file stays focused on overlays,
 * the center disc, and game-state plumbing.
 *
 * Layout rules:
 *   - Memory Lights+ (6 buttons) + landscape -> 2 rows x 3 columns
 *   - Memory Lights+ (6 buttons) + portrait  -> 3 rows x 2 columns
 *   - Classic (4 buttons)                    -> 2 rows x 2 columns (orientation-agnostic)
 *
 * Orientation is read once via `LocalConfiguration.current.orientation`
 * and stabilized with `remember(orientation)` so unrelated Configuration
 * changes (font scale, locale, density) don't re-evaluate the boolean
 * (#20 partial — full read still scoped to this composable so the parent
 * screen no longer recomposes on orientation changes outside the grid).
 *
 * The grid is intentionally state-free: it receives `litButton`,
 * `allButtonsLit`, and `pressedButtons` as inputs and forwards press
 * events through `onButtonInteraction`. The caller (SimonGameScreen)
 * owns the `displayLit` derivation (Audio-Only mode swallow) and the
 * `localPressedButtons` map.
 *
 * @param memoryLightsPlusEnabled true to render 6 panels, false for the classic 4.
 * @param litButton button the game is currently lighting up, or null. Pass
 *   `null` while audio-only mode is in its ShowingSequence phase.
 * @param allButtonsLit when true, every panel lights up (game-over flash).
 * @param pressedButtons map of buttons that the user is physically holding down.
 * @param colorBlindModeEnabled when true, each panel draws its [ColorBlindShape] (#35).
 * @param onButtonInteraction (button, isPress) callback. Fires on both press and release.
 */
@Composable
fun SimonButtonGrid(
    memoryLightsPlusEnabled: Boolean,
    litButton: SimonButton?,
    allButtonsLit: Boolean,
    pressedButtons: Map<SimonButton, Boolean>,
    onButtonInteraction: (SimonButton, Boolean) -> Unit,
    colorBlindModeEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Wrapped in `remember(orientation)` so the boolean is stable across
    // unrelated Configuration changes (font scale, locale, density).
    val orientation = LocalConfiguration.current.orientation
    val isLandscape = remember(orientation) {
        orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    if (memoryLightsPlusEnabled) {
        if (isLandscape) {
            SixButtonGridLandscape(
                litButton = litButton,
                allButtonsLit = allButtonsLit,
                pressedButtons = pressedButtons,
                colorBlindModeEnabled = colorBlindModeEnabled,
                onButtonInteraction = onButtonInteraction,
                modifier = modifier
            )
        } else {
            SixButtonGridPortrait(
                litButton = litButton,
                allButtonsLit = allButtonsLit,
                pressedButtons = pressedButtons,
                colorBlindModeEnabled = colorBlindModeEnabled,
                onButtonInteraction = onButtonInteraction,
                modifier = modifier
            )
        }
    } else {
        FourButtonGrid(
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            modifier = modifier
        )
    }
}

// 4-button mode panel padding (slightly larger than 6-button so the
// quadrants read as four distinct chunks at the original game's classic
// proportions).
private val FourButtonPanelPadding = 4.dp
// 6-button mode panel padding (tighter so the six panels fit without
// shrinking each cell uncomfortably).
private val SixButtonPanelPadding = 2.dp

@Composable
private fun SixButtonGridLandscape(
    litButton: SimonButton?,
    allButtonsLit: Boolean,
    pressedButtons: Map<SimonButton, Boolean>,
    colorBlindModeEnabled: Boolean,
    onButtonInteraction: (SimonButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        GridRow(
            buttons = TopRowLandscape,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = SixButtonPanelPadding
        )
        GridRow(
            buttons = BottomRowLandscape,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = SixButtonPanelPadding
        )
    }
}

@Composable
private fun SixButtonGridPortrait(
    litButton: SimonButton?,
    allButtonsLit: Boolean,
    pressedButtons: Map<SimonButton, Boolean>,
    colorBlindModeEnabled: Boolean,
    onButtonInteraction: (SimonButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        GridRow(
            buttons = TopRowPortrait,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = SixButtonPanelPadding
        )
        GridRow(
            buttons = MiddleRowPortrait,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = SixButtonPanelPadding
        )
        GridRow(
            buttons = BottomRowPortrait,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = SixButtonPanelPadding
        )
    }
}

@Composable
private fun FourButtonGrid(
    litButton: SimonButton?,
    allButtonsLit: Boolean,
    pressedButtons: Map<SimonButton, Boolean>,
    colorBlindModeEnabled: Boolean,
    onButtonInteraction: (SimonButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        GridRow(
            buttons = TopRowClassic,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = FourButtonPanelPadding
        )
        GridRow(
            buttons = BottomRowClassic,
            litButton = litButton,
            allButtonsLit = allButtonsLit,
            pressedButtons = pressedButtons,
            colorBlindModeEnabled = colorBlindModeEnabled,
            onButtonInteraction = onButtonInteraction,
            panelPadding = FourButtonPanelPadding
        )
    }
}

/**
 * One horizontal row of panels with `weight = 1f` cells. Declared as a
 * `ColumnScope` extension so the row itself claims `weight = 1f` of the
 * surrounding column's vertical space — that's how rows stay equal-height
 * regardless of how many rows the grid has (2 or 3).
 */
@Composable
private fun ColumnScope.GridRow(
    buttons: List<SimonButton>,
    litButton: SimonButton?,
    allButtonsLit: Boolean,
    pressedButtons: Map<SimonButton, Boolean>,
    colorBlindModeEnabled: Boolean,
    onButtonInteraction: (SimonButton, Boolean) -> Unit,
    panelPadding: Dp
) {
    Row(modifier = Modifier.weight(1f)) {
        buttons.forEach { button ->
            SimonPanel(
                color = button.color,
                colorName = stringResource(button.displayNameRes),
                isLit = litButton == button || allButtonsLit,
                userPressed = pressedButtons[button] == true,
                colorBlindShape = if (colorBlindModeEnabled) {
                    ColorBlindShape.forButton(button)
                } else {
                    null
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(panelPadding),
                onTouchStateChanged = { isPressed ->
                    onButtonInteraction(button, isPressed)
                }
            )
        }
    }
}

// Row orderings are declared as top-level constants so the grid composables
// stay declarative and so a future layout swap only edits one place.
private val TopRowLandscape = listOf(SimonButton.GREEN, SimonButton.RED, SimonButton.YELLOW)
private val BottomRowLandscape = listOf(SimonButton.BLUE, SimonButton.PURPLE, SimonButton.ORANGE)
private val TopRowPortrait = listOf(SimonButton.GREEN, SimonButton.RED)
private val MiddleRowPortrait = listOf(SimonButton.YELLOW, SimonButton.BLUE)
private val BottomRowPortrait = listOf(SimonButton.PURPLE, SimonButton.ORANGE)
private val TopRowClassic = listOf(SimonButton.GREEN, SimonButton.RED)
private val BottomRowClassic = listOf(SimonButton.YELLOW, SimonButton.BLUE)
