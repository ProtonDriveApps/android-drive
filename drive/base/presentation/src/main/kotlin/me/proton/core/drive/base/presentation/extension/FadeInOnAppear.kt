/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.presentation.extension

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

val LocalContentFadeIn = staticCompositionLocalOf { false }

fun Modifier.fadeInOnAppear(
    durationMillis: Int = DEFAULT_FADE_IN_DURATION_MS,
): Modifier = this then FadeInOnAppearElement(durationMillis)

@Composable
fun LazyItemScope.fadeInOnInsert(
    durationMillis: Int = DEFAULT_FADE_IN_DURATION_MS,
): Modifier = if (LocalContentFadeIn.current) {
    Modifier.animateItem(
        fadeInSpec = tween(durationMillis),
        placementSpec = null,
        fadeOutSpec = null,
    )
} else {
    Modifier
}

@Composable
fun SuppressContentFadeIn(
    isSuppressed: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentFadeIn provides (LocalContentFadeIn.current && !isSuppressed),
        content = content,
    )
}

suspend fun <T> withContentFadeInSuppressed(
    setSuppressed: (Boolean) -> Unit,
    block: suspend () -> T,
): T {
    setSuppressed(true)
    try {
        return block()
    } finally {
        setSuppressed(false)
    }
}

private data class FadeInOnAppearElement(
    val durationMillis: Int,
) : ModifierNodeElement<FadeInOnAppearNode>() {

    override fun create() = FadeInOnAppearNode(durationMillis)

    override fun update(node: FadeInOnAppearNode) {
        node.durationMillis = durationMillis
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "fadeInOnAppear"
        properties["durationMillis"] = durationMillis
    }
}

private class FadeInOnAppearNode(
    var durationMillis: Int,
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val alpha = Animatable(0f)
    private var isEnabled = false

    override fun onAttach() {
        isEnabled = currentValueOf(LocalContentFadeIn)
        if (isEnabled) {
            coroutineScope.launch { alpha.animateTo(1f, tween(durationMillis)) }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (isEnabled) {
                placeable.placeWithLayer(0, 0) { alpha = this@FadeInOnAppearNode.alpha.value }
            } else {
                placeable.place(0, 0)
            }
        }
    }
}

private const val DEFAULT_FADE_IN_DURATION_MS = 220
