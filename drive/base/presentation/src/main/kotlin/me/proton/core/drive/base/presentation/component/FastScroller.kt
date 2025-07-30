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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.entity.FastScrollAnchor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun FastScroller(
    state: LazyGridState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    isThumbVisible: MutableState<Boolean> = remember { mutableStateOf(false) },
    isFastScrollEnabled: Boolean = false,
    stepSize: Dp = 4.dp,
    thumbHeight: Dp = 48.dp,
    thumbContent: @Composable () -> Unit,
    thumbHideDelay: Duration = 3.seconds,
    getFastScrollAnchors: suspend (Int, Int) -> List<FastScrollAnchor>,
    onDraggedToPosition: suspend (Int) -> Unit,
) {
    with(LocalDensity.current) {
        FastScroller(
            state = state,
            itemCount = itemCount,
            stepSize = stepSize,
            stepPx = stepSize.toPx(),
            thumbHeightPx = thumbHeight.toPx(),
            isThumbVisible = isThumbVisible,
            isFastScrollEnabled = isFastScrollEnabled,
            modifier = modifier,
            thumbContent = thumbContent,
            thumbHideDelay = thumbHideDelay,
            getFastScrollAnchors = getFastScrollAnchors,
            onDraggedToPosition = onDraggedToPosition,
        )
    }
}

@Composable
fun FastScroller(
    state: LazyGridState,
    itemCount: Int,
    stepSize: Dp,
    stepPx: Float,
    thumbHeightPx: Float,
    isThumbVisible: MutableState<Boolean>,
    isFastScrollEnabled: Boolean,
    modifier: Modifier = Modifier,
    thumbContent: @Composable () -> Unit,
    thumbHideDelay: Duration = 3.seconds,
    getFastScrollAnchors: suspend (Int, Int) -> List<FastScrollAnchor>,
    onDraggedToPosition: suspend (Int) -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.isScrollInProgress, isDragging) {
        if (state.isScrollInProgress || isDragging) {
            isThumbVisible.value = isFastScrollEnabled
        } else {
            delay(thumbHideDelay)
            isThumbVisible.value = false
        }
    }
    var areLabelsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging) {
        if (isDragging) {
            areLabelsVisible = isFastScrollEnabled
        } else {
            delay(thumbHideDelay)
            areLabelsVisible = false
        }
    }

    var boxHeightPx by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    var lastStepIndex by remember { mutableIntStateOf(-1) }
    val maxSteps by remember(boxHeightPx, thumbHeightPx) {
        derivedStateOf {
            ((boxHeightPx - thumbHeightPx) / stepPx).toInt().coerceAtLeast(0)
        }
    }
    var fastScrollAnchors: List<FastScrollAnchor> by remember {
        mutableStateOf(emptyList())
    }
    var ranges: List<Pair<IntRange, Int>> by remember {
        mutableStateOf(emptyList())
    }
    LaunchedEffect(itemCount, maxSteps) {
        withContext(Dispatchers.Default) {
            fastScrollAnchors = getFastScrollAnchors(maxSteps + 1, (labelSize / stepSize.value).toInt())
            ranges = fastScrollAnchors
                .withIndex()
                .filter { (_, fastScrollAnchor) -> fastScrollAnchor.scrollToPosition != null }
                .zipWithNext { (indexStart, start), (_, end) ->
                    IntRange(requireNotNull(start.scrollToPosition), requireNotNull(end.scrollToPosition) - 1) to indexStart
                }
        }
    }
    val validStepIndexes by remember(fastScrollAnchors) {
        derivedStateOf {
            fastScrollAnchors.mapIndexedNotNull { index, anchor ->
                if (anchor.scrollToPosition != null) index else null
            }
        }
    }
    val snappedOffsetY by remember(rawOffsetY, stepPx, validStepIndexes) {
        derivedStateOf {
            if (validStepIndexes.isEmpty() || boxHeightPx < thumbHeightPx) return@derivedStateOf 0f
            val approximateIndex = (rawOffsetY / stepPx).roundToInt()
            val closestValidIndex = validStepIndexes.minByOrNull { abs(it - approximateIndex) } ?: 0
            (closestValidIndex * stepPx).coerceIn(0f, boxHeightPx - thumbHeightPx)
        }
    }
    val currentStepIndex by remember(snappedOffsetY, stepPx) {
        derivedStateOf {
            (snappedOffsetY / stepPx).roundToInt()
        }
    }
    LaunchedEffect(Unit) {
        combine(
            snapshotFlow { snappedOffsetY }.map { (it / stepPx).roundToInt() },
            snapshotFlow { state.firstVisibleItemIndex }
        ) { currentStepIndex, currentVisibleItemIndex ->
            if (boxHeightPx > 0) {
                if (isDragging) {
                    if (lastStepIndex != -1 && currentStepIndex != lastStepIndex) {
                        fastScrollAnchors
                            .getOrNull(currentStepIndex)
                            ?.takeIf { fastScrollAnchor -> fastScrollAnchor.scrollToPosition != null }
                            ?.let { fastScrollAnchor ->
                                onDraggedToPosition(requireNotNull(fastScrollAnchor.scrollToPosition))
                                state.scrollToItem(requireNotNull(fastScrollAnchor.scrollToPosition))
                            }
                    }
                } else {
                    val lastVisibleItemIndex = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    if (lastVisibleItemIndex != null && lastVisibleItemIndex == state.layoutInfo.totalItemsCount - 1) {
                        rawOffsetY = maxSteps * stepPx
                    } else {
                        val stepIndex = ranges
                            .find { (range, _) -> currentVisibleItemIndex in range }
                            ?.let { (_, index) -> index }
                            ?: currentStepIndex
                        rawOffsetY = stepIndex * stepPx
                    }
                }
                lastStepIndex = currentStepIndex
            }
        }.launchIn(this)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                boxHeightPx = coordinates.size.height.toFloat()
            }
    ) {
        AnimatedVisibility(
            visible = areLabelsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            AnchoredLabels(
                labels = fastScrollAnchors.map { fastScrollAnchor ->
                    Label.standardLabel(fastScrollAnchor.label)
                },
                stepSize = stepSize,
            )
        }
        AnimatedVisibility(
            visible = isThumbVisible.value,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, snappedOffsetY.roundToInt()) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (boxHeightPx >= thumbHeightPx) {
                                rawOffsetY = (rawOffsetY + delta)
                                    .coerceIn(0f, boxHeightPx - thumbHeightPx)
                            }
                        },
                        onDragStarted = { isDragging = true },
                        onDragStopped = { isDragging = false },
                    )
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    fastScrollAnchors.getOrNull(currentStepIndex)?.let { fastScrollAnchor ->
                        AnimatedVisibility(
                            visible = areLabelsVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            HighLightedLabel(
                                label = Label.highlightedLabel(fastScrollAnchor.dragLabel),
                                modifier = Modifier.padding(end = 24.dp)
                            )
                        }
                    }
                    thumbContent()
                }
            }
        }
    }
}

@Composable
private fun AnchoredLabels(
    labels: List<Label.Standard>,
    stepSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 72.dp, top = 13.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        labels.forEachIndexed { index, label ->
            StandardLabel(
                label = label,
                modifier = modifier.offset(y = (index * stepSize.value).dp),
            )
        }
    }
}

@Composable
fun HighLightedLabel(
    label: Label.Highlighted,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(labelSize.dp)
            .zIndex(1f),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            label = label,
            height = 26.dp,
        )
    }
}

@Composable
fun StandardLabel(
    label: Label.Standard,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(labelSize.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            label = label,
            height = (labelSize - 2).dp,
        )
    }
}

@Composable
fun Label(
    label: Label,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    label.value?.let { labelValue ->
        Card(
            shape = RoundedCornerShape(100.dp),
            elevation = ProtonDimens.ExtraSmallSpacing,
            backgroundColor = ProtonTheme.colors.backgroundSecondary,
            contentColor = ProtonTheme.colors.textNorm,
            modifier = modifier
                .height(height)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelValue,
                    style = label.textStyle,
                    modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                )
            }
        }
    }
}

sealed interface Label {
    val value: String?
    val textStyle: TextStyle

    data class Standard(
        override val value: String?,
        override val textStyle: TextStyle,
    ) : Label

    data class Highlighted(
        override val value: String?,
        override val textStyle: TextStyle,
    ) : Label

    companion object {
        @Composable
        fun standardLabel(value: String?) = Standard(value, ProtonTheme.typography.captionMedium)

        @Composable
        fun highlightedLabel(value: String?) = Highlighted(value, ProtonTheme.typography.body1Medium)
    }
}

private const val labelSize = 24
