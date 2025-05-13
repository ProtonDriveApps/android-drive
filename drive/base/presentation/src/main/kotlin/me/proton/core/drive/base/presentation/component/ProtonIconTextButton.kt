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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.component.protonElevation
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ProtonIconTextButton(
    iconPainter: Painter,
    title: String,
    modifier: Modifier = Modifier.heightIn(min = 44.dp),
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonButtonColors(
        backgroundColor = ProtonTheme.colors.interactionWeakNorm,
        contentColor = ProtonTheme.colors.textNorm,
        disabledBackgroundColor = ProtonTheme.colors.interactionWeakDisabled,
        disabledContentColor = ProtonTheme.colors.textDisabled,
        loading = loading,
    ),
    onClick: () -> Unit,
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = null,
        shape = RoundedCornerShape(ProtonDimens.DefaultSpacing),
        border = null,
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = {
            IconTextContent(
                iconPainter = iconPainter,
                text = title,
                textColor = colors.contentColor(enabled).value,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun IconTextContent(
    iconPainter: Painter,
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
        Text(
            text = text,
            color = textColor,
            style = ProtonTheme.typography.defaultStrongNorm(enabled),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
fun ProtonIconTextButtonLightPreview() {
    ProtonTheme(isDark = false) {
        ProtonIconTextButtonPreview()
    }
}

@Preview
@Composable
fun ProtonIconTextButtonDarkPreview() {
    ProtonTheme(isDark = true) {
        ProtonIconTextButtonPreview()
    }
}

@Composable
private fun ProtonIconTextButtonPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProtonTheme.colors.backgroundNorm,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PreviewHelper(enabled = true)
            PreviewHelper(enabled = false)
        }
    }
}

@Composable
private fun PreviewHelper(
    enabled: Boolean = true,
) {
    ProtonIconTextButton(
        iconPainter = painterResource(CorePresentation.drawable.ic_proton_plus_circle_filled),
        title = stringResource(I18N.string.common_add_action),
        enabled = enabled,
        onClick = {},
    )
}
