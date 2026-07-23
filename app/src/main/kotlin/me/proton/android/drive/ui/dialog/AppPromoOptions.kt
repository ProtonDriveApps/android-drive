/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.proton.android.drive.ui.viewevent.AppPromoOptionsViewEvent
import me.proton.android.drive.ui.viewmodel.AppPromoOptionsViewModel
import me.proton.android.drive.ui.viewstate.AppPromoOptionsViewState
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.presentation.R as CorePresentation

@Composable
fun AppPromoOptions(
    navigateToRatingBooster: () -> Unit,
    navigateToContactSupportOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AppPromoOptionsViewModel>()
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToRatingBooster = navigateToRatingBooster,
            navigateToContactSupportOptions = navigateToContactSupportOptions,
        )
    }
    LaunchedEffect(Unit) {
        viewEvent.onShown()
    }
    AppPromoOptions(
        viewState = viewModel.initialViewState,
        viewEvent = viewEvent,
        modifier = modifier
            .systemBarsPadding(),
    )
}

@Composable
fun AppPromoOptions(
    viewState: AppPromoOptionsViewState,
    viewEvent: AppPromoOptionsViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(all = ProtonDimens.DefaultSpacing),
    ) {
        Text(
            text = viewState.title,
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.headline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = viewState.description,
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.body1Regular,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ProtonDimens.SmallSpacing)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.DefaultSpacing)
        ) {
            val buttonModifier = Modifier
                .conditional(isPortrait) {
                    fillMaxWidth()
                }
                .conditional(isLandscape) {
                    widthIn(min = ButtonMinWidth)
                }
                .heightIn(min = ListItemHeight)
            ProtonSolidButton(
                onClick = viewEvent.onPositive,
                modifier = buttonModifier
                    .padding(top = ProtonDimens.MediumSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(CorePresentation.drawable.ic_proton_heart),
                        contentDescription = null,
                    )
                    Text(
                        text = viewState.positiveButtonTitle,
                        modifier = Modifier.padding(ProtonDimens.SmallSpacing)
                    )
                }
            }
            ProtonTextButton(
                onClick = viewEvent.onNegative,
                modifier = buttonModifier
                    .padding(top = ProtonDimens.DefaultSpacing)
            ) {
                Text(
                    text = viewState.negativeButtonTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val ButtonMinWidth = 300.dp

@Preview
@Composable
fun AppPromoOptionsPreview() {
    ProtonTheme {
        AppPromoOptions(
            viewState = AppPromoOptionsViewState(
                title = "Enjoying Proton Drive?",
                description = "If you're enjoying Proton Drive, we would greatly appreciate an app review!\nIf you are having an issue our Help Docs and support team are here to help!",
                positiveButtonTitle = "I like it",
                negativeButtonTitle = "Could be better",
            ),
            viewEvent = object : AppPromoOptionsViewEvent {}
        )
    }
}
