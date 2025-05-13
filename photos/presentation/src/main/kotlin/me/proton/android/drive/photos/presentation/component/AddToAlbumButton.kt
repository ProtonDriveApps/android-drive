/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.extension.protonNotificationSuccessButtonColors
import me.proton.core.drive.i18n.R as I18N

@Composable
fun AddToAlbumButton(
    addToAlbumTitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonNotificationSuccessButtonColors(loading),
    onClick: () -> Unit,
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.elevation(0.dp),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, ProtonTheme.colors.backgroundNorm),
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = {
            Text(text = addToAlbumTitle)
        },
    )
}

@Preview
@Composable
fun AddToAlbumButtonLightPreview() {
    ProtonTheme(isDark = false) {
        AddToAlbumButtonPreview()
    }
}

@Preview
@Composable
fun AddToAlbumButtonDarkPreview() {
    ProtonTheme(isDark = true) {
        AddToAlbumButtonPreview()
    }
}

@Composable
private fun AddToAlbumButtonPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProtonTheme.colors.backgroundNorm,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PreviewHelper(count = 0, enabled = true)
                PreviewHelper(count = 13, enabled = true)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PreviewHelper(count = 0, enabled = false)
                PreviewHelper(count = 13, enabled = false)
            }
        }
    }
}

@Composable
private fun PreviewHelper(
    count: Int = 0,
    enabled: Boolean = true,
) {
    AddToAlbumButton(
        addToAlbumTitle = if (count == 0) {
            stringResource(I18N.string.albums_add_zero_to_album_button)
        } else {
            pluralStringResource(I18N.plurals.albums_add_non_zero_to_album_button, count, count)
        },
        enabled = enabled,
        onClick = {},
    )
}
