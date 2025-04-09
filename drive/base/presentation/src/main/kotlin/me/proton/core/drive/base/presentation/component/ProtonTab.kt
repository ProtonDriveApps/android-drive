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

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.i18n.R
import kotlin.math.roundToInt

@Composable
fun ProtonTab(
    @StringRes titleResId: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onTab: () -> Unit,
    minWith: Dp = 120.dp,
) {
    val brandColor = ProtonTheme.colors.brandNorm
    val dividerColor by animateColorAsState(
        targetValue = if (isSelected) brandColor else Color.Transparent,
        label = "dividerColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) brandColor else ProtonTheme.colors.textWeak,
        label = "textColor"
    )
    val thickness = 4.dp
    val offset by animateIntOffsetAsState(
        targetValue = if (isSelected) {
            IntOffset.Zero
        } else {
            with(LocalDensity.current) {
                IntOffset(0, thickness.toPx().roundToInt())
            }
        },
        label = "offset"
    )
    ProtonButton(
        modifier = modifier,
        onClick = { onTab() },
        contentPadding = PaddingValues(horizontal = SmallSpacing),
        colors = ButtonDefaults.protonTextButtonColors(false),
        shape = RoundedCornerShape(0.dp),
        border = null,
        elevation = null,
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minHeight = DefaultButtonMinHeight, minWidth = minWith),
        ) {
            Text(
                text = stringResource(id = titleResId),
                style = ProtonTheme.typography.body2Regular.copy(
                    color = textColor
                ),
                modifier = Modifier
                    .align(Alignment.Center),
            )
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Divider(
                    color = dividerColor,
                    thickness = thickness,
                    modifier = Modifier
                        .offset { offset }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100.dp, 100.dp, 0.dp, 0.dp)),
                )
            }
        }
    }
}

@Preview
@Composable
fun ProtonTabPreview() {
    ProtonTheme {
        var isSelected by remember { mutableIntStateOf(0) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProtonTab(
                titleResId = R.string.shared_with_me_title,
                isSelected = isSelected == 0,
                onTab = { isSelected = 0 }
            )
            ProtonTab(
                titleResId = R.string.shared_by_me_title,
                isSelected = isSelected == 1,
                onTab = { isSelected = 1 }
            )
        }
    }
}
