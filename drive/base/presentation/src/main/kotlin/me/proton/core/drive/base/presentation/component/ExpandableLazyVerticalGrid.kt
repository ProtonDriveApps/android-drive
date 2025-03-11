/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.base.presentation.component

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.sign

object ExpandableLazyVerticalGrid {
    enum class State {
        COLLAPSED,
        EXPANDED,
        COLLAPSING,
        EXPANDING,
    }

    enum class Direction {
        NONE,
        COLLAPSING,
        EXPANDING,
    }
}

@Suppress("FunctionName")
fun MutableExpandableLazyVerticalGridStateSaver(
): Saver<MutableState<ExpandableLazyVerticalGrid.State>, *> = Saver(
    save = { it.value.name },
    restore = { mutableStateOf(enumValueOf<ExpandableLazyVerticalGrid.State>(it)) }
)

@Composable
fun rememberExpandableLazyVerticalGridState(
    initialState: ExpandableLazyVerticalGrid.State = ExpandableLazyVerticalGrid.State.COLLAPSED,
): MutableState<ExpandableLazyVerticalGrid.State> =
    rememberSaveable(
        saver = MutableExpandableLazyVerticalGridStateSaver()
    ) {
        mutableStateOf(initialState)
    }

@Composable
fun ExpandableLazyVerticalGrid(
    columns: GridCells,
    minGridHeight: Dp,
    maxGridHeight: Dp,
    modifier: Modifier = Modifier,
    lazyVerticalGridModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) {
        Arrangement.Top
    } else {
        Arrangement.Bottom
    },
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    state: MutableState<ExpandableLazyVerticalGrid.State> = rememberExpandableLazyVerticalGridState(),
    gridState: LazyGridState = rememberLazyGridState(),
    topComposable: @Composable BoxScope.() -> Unit,
    content: LazyGridScope.() -> Unit,
) {
    var gridHeight by remember {
        mutableStateOf(
            if (state.value == ExpandableLazyVerticalGrid.State.COLLAPSED) {
                minGridHeight
            } else {
                maxGridHeight
            }
        )
    }
    val isFirstItemFullyVisible by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }
    }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val direction = available.toDirection()
                val distance = 10f
                val factor = direction.factor(1f, state.value)
                when {
                    isFirstItemFullyVisible &&
                    direction == ExpandableLazyVerticalGrid.Direction.EXPANDING &&
                    state.value != ExpandableLazyVerticalGrid.State.EXPANDED ||
                    isFirstItemFullyVisible &&
                    direction in listOf(
                        ExpandableLazyVerticalGrid.Direction.COLLAPSING,
                        ExpandableLazyVerticalGrid.Direction.NONE
                    ) &&
                    state.value != ExpandableLazyVerticalGrid.State.COLLAPSED ||
                    state.value == ExpandableLazyVerticalGrid.State.EXPANDING ||
                    state.value == ExpandableLazyVerticalGrid.State.COLLAPSING
                        -> gridHeight = (gridHeight - (distance * factor).dp)
                            .coerceIn(minGridHeight, maxGridHeight)
                }
                state.value = when {
                    gridHeight == minGridHeight -> ExpandableLazyVerticalGrid.State.COLLAPSED
                    gridHeight == maxGridHeight -> ExpandableLazyVerticalGrid.State.EXPANDED
                    direction == ExpandableLazyVerticalGrid.Direction.COLLAPSING -> ExpandableLazyVerticalGrid.State.COLLAPSING
                    direction == ExpandableLazyVerticalGrid.Direction.EXPANDING -> ExpandableLazyVerticalGrid.State.EXPANDING
                    else -> state.value
                }
                return Offset.Zero
            }
        }
    }
    Box(
        modifier = modifier
    ) {
        topComposable()
        LazyVerticalGrid(
            columns = columns,
            modifier = lazyVerticalGridModifier
                .height(gridHeight)
                .zIndex(1f)
                .align(Alignment.BottomCenter)
                .nestedScroll(nestedScrollConnection),
            state = gridState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
    }
}

internal fun Offset.toDirection(): ExpandableLazyVerticalGrid.Direction = when (y.sign) {
    -1f -> ExpandableLazyVerticalGrid.Direction.EXPANDING
    1f -> ExpandableLazyVerticalGrid.Direction.COLLAPSING
    else -> ExpandableLazyVerticalGrid.Direction.NONE
}

internal fun ExpandableLazyVerticalGrid.Direction.factor(
    factor: Float,
    state: ExpandableLazyVerticalGrid.State,
): Float = when (this) {
    ExpandableLazyVerticalGrid.Direction.EXPANDING -> -factor
    ExpandableLazyVerticalGrid.Direction.COLLAPSING -> factor
    ExpandableLazyVerticalGrid.Direction.NONE -> when (state) {
        ExpandableLazyVerticalGrid.State.EXPANDING -> -factor
        ExpandableLazyVerticalGrid.State.COLLAPSING -> factor
        else -> 0f
    }
}

@Composable
fun defaultMinMaxGridHeight(
    topComposableHeight: Dp,
    overlapThreshold: Dp = topComposableHeight * 0.5f,
): Pair<Dp, Dp> {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density)
    val statusBarHeightDp = with(density) { statusBarHeightPx.toDp() }
    val navigationBarHeightDp = with(density) { navigationBarHeightPx.toDp() }
    val screenHeight = screenHeightDp + statusBarHeightDp + navigationBarHeightDp
    return (screenHeight - topComposableHeight) to (screenHeight - (topComposableHeight - overlapThreshold))
}
