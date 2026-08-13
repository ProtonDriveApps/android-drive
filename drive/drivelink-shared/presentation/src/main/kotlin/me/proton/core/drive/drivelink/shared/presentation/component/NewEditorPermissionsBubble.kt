/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraLargeCornerRadius
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun NewEditorPermissionsBubble(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = EnterTransition.None,
        exit = shrinkVertically(shrinkTowards = Alignment.CenterVertically) + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DefaultSpacing, vertical = SmallSpacing)
                .border(
                    width = BorderWidth,
                    color = ProtonTheme.colors.separatorNorm,
                    shape = BubbleWithTailShape(
                        cornerRadius = ExtraLargeCornerRadius,
                        tailWidth = TailWidth,
                        tailHeight = TailHeight,
                        tailInsetFromEnd = TailInsetFromEnd,
                    ),
                )
                .padding(start = DefaultSpacing, bottom = TailHeight)
                .testTag(NewEditorPermissionsBubbleTestTag.bubble),
            horizontalArrangement = Arrangement.spacedBy(DefaultSpacing),
        ) {
            Icon(
                modifier = Modifier.padding(top = DefaultSpacing),
                painter = painterResource(id = CorePresentation.drawable.ic_proton_users),
                contentDescription = null,
                tint = ProtonTheme.colors.brandNorm,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = DefaultSpacing),
                verticalArrangement = Arrangement.spacedBy(ExtraSmallSpacing),
            ) {
                Text(
                    text = stringResource(id = I18N.string.manage_access_new_editor_permissions_title),
                    style = ProtonTheme.typography.defaultNorm,
                )
                Text(
                    text = stringResource(id = I18N.string.manage_access_new_editor_permissions_description),
                    style = ProtonTheme.typography.defaultSmallWeak,
                )
            }
            IconButton(
                modifier = Modifier
                    .size(DefaultButtonMinHeight)
                    .testTag(NewEditorPermissionsBubbleTestTag.dismiss),
                onClick = onDismiss,
            ) {
                Icon(
                    painter = painterResource(id = CorePresentation.drawable.ic_proton_cross),
                    contentDescription = stringResource(id = I18N.string.common_close_action),
                    tint = ProtonTheme.colors.iconNorm,
                )
            }
        }
    }
}

/**
 * Rounded rectangle with a tail at the bottom, pointing down at what the bubble talks about.
 * Drawn as a single outline so that a border follows the tail instead of cutting across its base.
 */
private data class BubbleWithTailShape(
    val cornerRadius: Dp,
    val tailWidth: Dp,
    val tailHeight: Dp,
    val tailInsetFromEnd: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val tailHalfWidth = with(density) { tailWidth.toPx() } / 2f
        val tail = with(density) { tailHeight.toPx() }
        val bottom = size.height - tail
        val radius = with(density) { cornerRadius.toPx() }
            .coerceAtMost(minOf(size.width, bottom) / 2f)
        val insetFromEnd = with(density) { tailInsetFromEnd.toPx() }
        val tailCenter = when (layoutDirection) {
            LayoutDirection.Ltr -> size.width - insetFromEnd
            LayoutDirection.Rtl -> insetFromEnd
        }.coerceIn(radius + tailHalfWidth, size.width - radius - tailHalfWidth)
        return Outline.Generic(
            Path().apply {
                moveTo(radius, 0f)
                lineTo(size.width - radius, 0f)
                arcTo(
                    Rect(size.width - 2 * radius, 0f, size.width, 2 * radius),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(size.width, bottom - radius)
                arcTo(
                    Rect(size.width - 2 * radius, bottom - 2 * radius, size.width, bottom),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(tailCenter + tailHalfWidth, bottom)
                lineTo(tailCenter, bottom + tail)
                lineTo(tailCenter - tailHalfWidth, bottom)
                lineTo(radius, bottom)
                arcTo(
                    Rect(0f, bottom - 2 * radius, 2 * radius, bottom),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(0f, radius)
                arcTo(
                    Rect(0f, 0f, 2 * radius, 2 * radius),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                close()
            }
        )
    }
}

private val BorderWidth = 1.dp
private val TailWidth = 16.dp
private val TailHeight = 8.dp

// Distance from the end edge of the bubble to the tip of the tail, so that it points at the
// switch of the toggle right below it.
private val TailInsetFromEnd = 32.dp

object NewEditorPermissionsBubbleTestTag {
    const val bubble = "new editor permissions bubble"
    const val dismiss = "new editor permissions bubble dismiss"
}

@Preview
@Composable
private fun NewEditorPermissionsBubblePreview() {
    ProtonTheme {
        NewEditorPermissionsBubble(
            visible = true,
            onDismiss = {},
        )
    }
}
