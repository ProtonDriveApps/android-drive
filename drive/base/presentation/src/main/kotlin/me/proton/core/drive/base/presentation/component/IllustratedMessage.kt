/*
 * Copyright (c) 2024 Proton AG.
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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.i18n.R as I18N

@Composable
fun IllustratedMessage(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    @StringRes descriptionResId: Int? = null,
) {
    IllustratedMessage(
        imageContent = {
            Image(painter = painterResource(id = imageResId), contentDescription = null)
        },
        titleResId = titleResId,
        modifier = modifier,
        descriptionResId = descriptionResId,
    )
}

@Composable
fun IllustratedMessage(
    imageContent: @Composable () -> Unit,
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    @StringRes descriptionResId: Int? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            imageContent()
            Text(
                text = stringResource(id = titleResId),
                style = ProtonTheme.typography.headlineNorm.copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(
                    top = ProtonDimens.DefaultSpacing,
                    start = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.DefaultSpacing,
                )
            )
            descriptionResId?.let {
                Text(
                    text = stringResource(id = descriptionResId),
                    style = ProtonTheme.typography.defaultNorm.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.padding(
                        top = ProtonDimens.SmallSpacing,
                        start = ProtonDimens.DefaultSpacing,
                        end = ProtonDimens.DefaultSpacing,
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewListEmpty() {
    ProtonTheme {
        Surface {
            IllustratedMessage(
                imageResId = R.drawable.empty_folder_daynight,
                titleResId = I18N.string.title_empty_folder,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
fun PreviewListEmptyWithDescription() {
    ProtonTheme {
        Surface {
            IllustratedMessage(
                imageResId = R.drawable.empty_folder_daynight,
                titleResId = I18N.string.title_empty_folder,
                descriptionResId = I18N.string.description_empty_folder,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
