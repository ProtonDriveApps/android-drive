/*
 * Copyright (c) 2023 Proton AG.
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

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.android.drive.photos.presentation.R
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraLargeCornerRadius
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.LargeSpacing
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun PhotosEmptyWithBackupTurnedOff(
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
) {
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> PhotosEmptyWithBackupTurnedOffSingleColumn(modifier, onEnable)
        else -> PhotosEmptyWithBackupTurnedOffTwoColumns(modifier, onEnable)
    }
}

@Composable
private fun PhotosEmptyWithBackupTurnedOffSingleColumn(
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_photos_welcome),
                contentDescription = null,
                modifier = Modifier.padding(top = MediumSpacing)
            )
            Title()
            SingleColumnDetails()
        }
        TurnOnBackupButton(
            modifier = Modifier.padding(vertical = MediumSpacing),
            onClick = onEnable,
        )
    }
}

@Composable
private fun PhotosEmptyWithBackupTurnedOffTwoColumns(
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Title()
            TwoColumnsDetails(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            )
        }
        TurnOnBackupButton(
            modifier = Modifier.padding(vertical = SmallSpacing),
            onClick = onEnable,
        )
    }
}

@Composable
private fun SingleColumnDetails(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Section(
            modifier = Modifier.padding(
                vertical = SmallSpacing,
                horizontal = MediumSpacing,
            ),
            icon = CorePresentation.drawable.ic_proton_lock,
            title = I18N.string.photos_permissions_section_1_title,
            description = I18N.string.photos_permissions_section_1_description,
        )
        Section(
            modifier = Modifier.padding(
                vertical = SmallSpacing,
                horizontal = MediumSpacing,
            ),
            icon = CorePresentation.drawable.ic_proton_arrows_rotate,
            title = I18N.string.photos_permissions_section_2_title,
            description = I18N.string.photos_permissions_section_2_description,
        )
    }
}

@Composable
private fun TwoColumnsDetails(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Section(
            modifier = Modifier
                .padding(
                    vertical = SmallSpacing,
                    horizontal = MediumSpacing,
                )
                .weight(1f),
            icon = CorePresentation.drawable.ic_proton_lock,
            title = I18N.string.photos_permissions_section_1_title,
            description = I18N.string.photos_permissions_section_1_description,
        )
        Section(
            modifier = Modifier
                .padding(
                    vertical = SmallSpacing,
                    horizontal = MediumSpacing,
                )
                .weight(1f),
            icon = CorePresentation.drawable.ic_proton_arrows_rotate,
            title = I18N.string.photos_permissions_section_2_title,
            description = I18N.string.photos_permissions_section_2_description,
        )
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = I18N.string.photos_permissions_title),
        style = ProtonTheme.typography.headlineNorm.copy(textAlign = TextAlign.Center),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = DefaultSpacing,
                bottom = MediumSpacing,
                start = LargeSpacing,
                end = LargeSpacing,
            ),
    )
}

@Composable
private fun TurnOnBackupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MediumSpacing),
        contentAlignment = Alignment.Center,
    ) {
        ProtonSolidButton(
            onClick = onClick,
            modifier = Modifier
                .conditional(isPortrait) {
                    fillMaxWidth()
                }
                .conditional(isLandscape) {
                    widthIn(min = ButtonMinWidth)
                }
                .heightIn(min = ListItemHeight),
        ) {
            Text(
                text = stringResource(id = I18N.string.photos_permissions_action),
                modifier = Modifier.padding(horizontal = DefaultSpacing),
            )
        }
    }
}

private val ButtonMinWidth = 300.dp

@Composable
private fun Section(
    modifier: Modifier = Modifier,
    icon: Int,
    title: Int,
    description: Int,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DefaultSpacing),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    color = ProtonTheme.colors.brandNorm.copy(alpha = 0.2F),
                    shape = RoundedCornerShape(ExtraLargeCornerRadius),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = ProtonTheme.colors.iconAccent
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ExtraSmallSpacing)) {
            Text(
                text = stringResource(id = title),
                style = ProtonTheme.typography.headlineNorm,
            )
            Text(
                text = stringResource(id = description),
                style = ProtonTheme.typography.defaultWeak,
            )
        }
    }
}

@Preview
@Composable
fun PhotosPermissionsPreview() {
    ProtonTheme {
        Surface {
            PhotosEmptyWithBackupTurnedOff(onEnable = {})
        }
    }
}

@Preview(widthDp = 600, heightDp = 360)
@Composable
fun PhotosPermissionsPreviewLandscape() {
    ProtonTheme {
        Surface {
            PhotosEmptyWithBackupTurnedOff(onEnable = {})
        }
    }
}
