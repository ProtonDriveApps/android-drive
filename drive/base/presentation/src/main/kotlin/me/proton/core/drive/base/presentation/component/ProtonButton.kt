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

package me.proton.core.drive.base.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun ProtonSolidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonButtonColors(loading),
    content: @Composable () -> Unit,
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.protonElevation(),
        shape = ProtonTheme.shapes.medium,
        border = null,
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = content,
    )
}

@Composable
fun ButtonDefaults.protonElevation() = buttonElevation()

@Composable
fun ButtonDefaults.protonButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.interactionNorm,
    contentColor: Color = Color.White,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.interactionPressed
    } else {
        ProtonTheme.colors.brandLighten40
    },
    disabledContentColor: Color = if (loading) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.5f)
    },
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor,
)

@Composable
fun ProtonOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonOutlinedButtonColors(loading),
    content: @Composable () -> Unit,
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = null,
        shape = ProtonTheme.shapes.medium,
        border = ButtonDefaults.protonOutlinedBorder(enabled, loading),
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = content,
    )
}

@Composable
fun ButtonDefaults.protonOutlinedBorder(
    enabled: Boolean = true,
    loading: Boolean = false,
) = BorderStroke(
    OutlinedBorderSize,
    when {
        loading -> ProtonTheme.colors.interactionPressed
        !enabled -> ProtonTheme.colors.interactionDisabled
        else -> ProtonTheme.colors.brandNorm
    },
)

@Composable
fun ButtonDefaults.protonOutlinedButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    contentColor: Color = ProtonTheme.colors.textAccent,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.backgroundSecondary
    } else {
        ProtonTheme.colors.backgroundNorm
    },
    disabledContentColor: Color = if (loading) {
        ProtonTheme.colors.interactionPressed
    } else {
        ProtonTheme.colors.interactionDisabled
    },
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor,
)

@Composable
fun ProtonTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonTextButtonColors(loading),
    content: @Composable () -> Unit,
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = null,
        shape = ProtonTheme.shapes.small,
        border = null,
        colors = colors,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        content = content,
    )
}

@Composable
fun ButtonDefaults.protonTextButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = if (loading) {
        ProtonTheme.colors.backgroundSecondary
    } else {
        Color.Transparent
    },
    contentColor: Color = ProtonTheme.colors.textAccent,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.backgroundSecondary
    } else {
        Color.Transparent
    },
    disabledContentColor: Color = if (loading) {
        ProtonTheme.colors.interactionPressed
    } else {
        ProtonTheme.colors.textDisabled
    },
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor,
)

@Composable
fun ProtonSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonSecondaryButtonColors(loading),
    content: @Composable () -> Unit,
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        interactionSource = interactionSource,
        elevation = null,
        shape = ProtonTheme.shapes.medium,
        border = null,
        colors = colors,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        content = content,
    )
}

@Composable
fun ButtonDefaults.protonSecondaryButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.interactionWeakNorm,
    contentColor: Color = ProtonTheme.colors.textNorm,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.interactionWeakPressed
    } else {
        ProtonTheme.colors.interactionWeakDisabled
    },
    disabledContentColor: Color = if (loading) {
        ProtonTheme.colors.textNorm
    } else {
        ProtonTheme.colors.textDisabled
    },
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor,
)

@Suppress("LongParameterList")
@Composable
fun ProtonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation?,
    shape: Shape,
    border: BorderStroke?,
    colors: ButtonColors,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
        enabled = !loading && enabled,
        interactionSource = interactionSource,
        elevation = elevation,
        shape = shape,
        border = border,
        colors = colors,
        contentPadding = contentPadding,
    ) {
        ProtonButtonContent(
            loading = loading,
            contained = contained,
            content = content,
            progressColor = colors.disabledContentColor,
        )
    }
}

@Composable
private fun ProtonButtonContent(
    loading: Boolean = false,
    contained: Boolean = true,
    progressColor: Color,
    content: @Composable () -> Unit,
) {
    if (!contained) {
        Box(Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                content()
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(LoadingIndicatorSize)
                        .align(Alignment.CenterEnd),
                    color = progressColor,
                    strokeWidth = LoadingIndicatorStroke,
                )
            }
        }
    } else {
        content()
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
                    .size(LoadingIndicatorSize),
                color = progressColor,
                strokeWidth = LoadingIndicatorStroke,
            )
        }
    }
}

private val OutlinedBorderSize = 1.dp
private val LoadingIndicatorSize = 14.dp
private val LoadingIndicatorStroke = 1.dp
