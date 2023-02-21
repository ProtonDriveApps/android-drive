/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.ui.navigation.internal

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState
import androidx.navigation.compose.LocalOwnersProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.compose.component.bottomsheet.ModalBottomSheet
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState
import me.proton.core.compose.component.bottomsheet.RunAction

@Navigator.Name(ModalBottomSheetNavigator.NAME)
class ModalBottomSheetNavigator : Navigator<ModalBottomSheetNavigator.Destination>() {

    internal var attached by mutableStateOf(false)

    /**
     * Get the back stack from the [state].
     */
    internal val backStack get() = if (attached) state.backStack else MutableStateFlow(emptyList())

    /**
     * Dismiss the dialog destination associated with the given [backStackEntry].
     */
    internal fun dismiss(backStackEntry: NavBackStackEntry) {
        popBackStack(backStackEntry, false)
    }

    override fun onAttach(state: NavigatorState) {
        super.onAttach(state)
        attached = true
    }

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.lastOrNull { entry -> entry.destination is Destination }?.let { toBeAdded ->
            if (backStack.value.none { entry -> entry.destination is Destination }) {
                state.push(toBeAdded)
            }
        }
    }

    override fun createDestination() =
        Destination()

    /**
     * NavDestination specific to [ModalBottomSheetNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    class Destination(
        internal val viewState: ModalBottomSheetViewState = ModalBottomSheetViewState(),
        internal val content: @Composable (NavBackStackEntry, runAction: RunAction) -> Unit = { _, _ -> },
    ) : NavDestination(NAME), FloatingWindow

    internal companion object {
        internal const val NAME = "modalBottomSheet"
    }
}

@Composable
internal fun ModalBottomSheetHost(
    navigator: ModalBottomSheetNavigator,
) {
    val modalBackStack by remember(navigator.attached) { navigator.backStack }.collectAsState()
    val visibleBackStack = rememberVisibleList(modalBackStack)
    visibleBackStack.PopulateVisibleList(modalBackStack)
    visibleBackStack.forEach { backStackEntry ->
        ModalBottomSheetContent(backStackEntry) { navigator.dismiss(backStackEntry) }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ModalBottomSheetContent(
    backStackEntry: NavBackStackEntry,
    saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder(),
    onDismiss: () -> Unit,
) {
    val destination = backStackEntry.destination as ModalBottomSheetNavigator.Destination
    var dismissTrigger by remember(destination) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden) { value ->
        if (value == ModalBottomSheetValue.Hidden) {
            dismissTrigger = true
        }
        true
    }
    ModalBottomSheet(
        viewState = destination.viewState,
        sheetContent = { runAction ->
            // I don't know why exactly we need this check here as well as above but I believe that if we show and
            // dismiss very quickly the bottom sheet, the currentState can pass in DESTROYED before this is called
            // and it crashes so better safe than sorry
            if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                    destination.content(backStackEntry, runAction)
                }
            }
        },
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        onDismiss = onDismiss,
        content = { }
    )
    LaunchedEffect(destination) { sheetState.show() }
    LaunchedEffect(destination, dismissTrigger) {
        if (dismissTrigger) {
            while (sheetState.isAnimationRunning) {
                delay(10)
            }
            onDismiss()
        }
    }
}

// From NavHost.kt those are internal functions of androidx.navigation.compose package
@Composable
internal fun MutableList<NavBackStackEntry>.PopulateVisibleList(
    transitionsInProgress: Collection<NavBackStackEntry>
) {
    transitionsInProgress.forEach { entry ->
        DisposableEffect(entry.lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                // ON_START -> add to visibleBackStack, ON_STOP -> remove from visibleBackStack
                if (event == Lifecycle.Event.ON_START) {
                    // We want to treat the visible lists as Sets but we want to keep
                    // the functionality of mutableStateListOf() so that we recompose in response
                    // to adds and removes.
                    if (!contains(entry)) {
                        add(entry)
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    remove(entry)
                }
            }
            entry.lifecycle.addObserver(observer)
            onDispose {
                entry.lifecycle.removeObserver(observer)
            }
        }
    }
}

@Composable
internal fun rememberVisibleList(transitionsInProgress: Collection<NavBackStackEntry>) =
    remember(transitionsInProgress) {
        mutableStateListOf<NavBackStackEntry>().also {
            it.addAll(
                transitionsInProgress.filter { entry ->
                    entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                }
            )
        }
    }
