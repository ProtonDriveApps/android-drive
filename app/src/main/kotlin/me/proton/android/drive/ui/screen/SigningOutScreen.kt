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

package me.proton.android.drive.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.SigningOutViewModel
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.i18n.R as I18N

@ExperimentalCoroutinesApi
@Composable
fun SigningOutScreen(
    modifier: Modifier = Modifier,
) {
    val signingOutViewModel = hiltViewModel<SigningOutViewModel>()
    LaunchedEffect(Unit) {
        signingOutViewModel.signOut()
    }
    SigningOut(modifier)
}

@Composable
private fun SigningOut(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(
                modifier = Modifier.padding(top = DefaultSpacing),
                text = stringResource(id = I18N.string.description_signing_out),
                color = ProtonTheme.colors.textNorm,
            )
        }
    }
}

@Preview(
    name = "Signing out in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Signing out in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
@Suppress("unused")
private fun PreviewSigningOut() {
    ProtonTheme {
        SigningOut(Modifier.background(ProtonTheme.colors.backgroundSecondary))
    }
}
