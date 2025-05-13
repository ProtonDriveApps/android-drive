/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait

@Composable
fun PromoContainer(
    titleResId: Int,
    descriptionResId: Int,
    imageResId: Int,
    actionText: String,
    dismissActionText: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
    onCancel: () -> Unit,
) {
    PromoContainer(
        modifier = modifier,
        title = stringResource(id = titleResId),
        description = stringResource(id = descriptionResId),
        image = {
            Image(painter = painterResource(id = imageResId), contentDescription = null)
        },
        actionText = actionText,
        dismissActionText = dismissActionText,
        onAction = onAction,
        onCancel = onCancel
    )
}

@Composable
fun PromoContainer(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    image: @Composable () -> Unit,
    actionText: String,
    dismissActionText: String,
    onAction: () -> Unit,
    onCancel: () -> Unit,
) {
    PromoContainer(
        actionText = actionText,
        dismissActionText = dismissActionText,
        bodyContent = {
            IllustratedMessage(
                imageContent = image,
                title = title,
                description = description,
            )
        },
        onAction = onAction,
        onCancel = onCancel,
        modifier = modifier,
    )
}

@Composable
fun PromoContainer(
    actionText: String,
    dismissActionText: String,
    modifier: Modifier = Modifier,
    bodyContent: @Composable () -> Unit,
    onAction: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        bodyContent()
        val buttonModifier = Modifier
            .conditional(isPortrait) {
                fillMaxWidth()
            }
            .conditional(isLandscape) {
                widthIn(min = ButtonMinWidth)
            }
            .heightIn(min = ProtonDimens.ListItemHeight)
        ProtonSolidButton(
            onClick = onAction,
            modifier = buttonModifier,
        ) {
            Text(
                text = actionText,
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }
        ProtonTextButton(
            onClick = onCancel,
            modifier = buttonModifier
        ) {
            Text(
                text = dismissActionText,
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }
    }
}

private val ButtonMinWidth = 300.dp
