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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.android.drive.photos.presentation.R
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineHint
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.extension.launchApplicationDetailsSettings
import me.proton.core.drive.i18n.R as I18N

@Composable
fun PhotosPermissionRationale(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val onSettings = {
        context.launchApplicationDetailsSettings()
        onBack()
    }
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> PhotosPermissionRationaleWithIllustration(modifier, onSettings, onBack)
        else -> PhotosPermissionRationaleWithoutIllustration(modifier, onSettings, onBack)
    }
}

@Composable
fun PhotosPermissionRationaleWithIllustration(
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Illustration()
            TitleAndDescription()
        }
        val context = LocalContext.current
        Buttons(
            onSettings = onSettings,
            onNotNow = onNotNow,
        )
    }
}

@Composable
fun PhotosPermissionRationaleWithoutIllustration(
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            TitleAndDescription()
        }
        val context = LocalContext.current
        Buttons(
            onSettings = onSettings,
            onNotNow = onNotNow,
        )
    }
}

@Composable
private fun Illustration(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = MediumSpacing),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_photos_allow_access),
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .width(275.dp)
                .padding(top = 52.dp, start = 106.dp),
            text = stringResource(id = I18N.string.photos_permission_rational_img_text),
            style = ProtonTheme.typography.headlineHint.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TitleAndDescription(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                id = I18N.string.photos_permission_rational_title,
                stringResource(id = I18N.string.app_name),
            ),
            style = ProtonTheme.typography.headlineNorm.copy(textAlign = TextAlign.Center),
            modifier = modifier
                .padding(
                    top = MediumSpacing,
                    start = MediumSpacing,
                    end = MediumSpacing,
                )
        )
        Text(
            text = stringResource(
                id = I18N.string.photos_permission_rational_description,
                stringResource(id = I18N.string.app_name),
            ),
            style = ProtonTheme.typography.defaultWeak.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .padding(
                    top = DefaultSpacing,
                    start = MediumSpacing,
                    end = MediumSpacing,
                )
        )
    }
}

@Composable
private fun Buttons(
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(vertical = SmallSpacing)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SmallSpacing)
    ) {
        val buttonModifier = Modifier
            .conditional(isPortrait) {
                fillMaxWidth()
            }
            .conditional(isLandscape) {
                widthIn(min = ButtonMinWidth)
            }
            .padding(horizontal = MediumSpacing)
            .heightIn(min = ListItemHeight)
        ProtonSolidButton(
            onClick = onSettings,
            modifier = buttonModifier,
        ) {
            Text(
                text = stringResource(id = I18N.string.photos_permission_rational_confirm_action),
                modifier = Modifier.padding(horizontal = DefaultSpacing)
            )
        }
        ProtonTextButton(
            onClick = onNotNow,
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(id = I18N.string.photos_permission_rational_dismiss_action),
                modifier = Modifier.padding(horizontal = DefaultSpacing)
            )
        }
    }
}

private val ButtonMinWidth = 300.dp

@Preview
@Composable
fun PhotosPermissionRationalePreview() {
    ProtonTheme {
        Surface {
            PhotosPermissionRationale(onBack = {})
        }
    }
}
@Preview(widthDp = 600, heightDp = 360)
@Composable
private fun PhotosPermissionRationalePreviewLandscape() {
    ProtonTheme {
        Surface {
            PhotosPermissionRationale(onBack = {})
        }
    }
}
