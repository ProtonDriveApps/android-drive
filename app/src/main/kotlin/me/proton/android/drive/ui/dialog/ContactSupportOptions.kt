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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.proton.android.drive.ui.viewevent.ContactSupportOptionsViewEvent
import me.proton.android.drive.ui.viewmodel.ContactSupportOptionsViewModel
import me.proton.android.drive.ui.viewstate.ContactSupportOptionsViewState
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ContactSupportOptions(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ContactSupportOptionsViewModel>()
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
        )
    }
    ContactSupportOptions(
        viewState = viewModel.initialViewState,
        viewEvent = viewEvent,
        modifier = modifier
            .systemBarsPadding(),
    )
}

@Composable
fun ContactSupportOptions(
    viewState: ContactSupportOptionsViewState,
    viewEvent: ContactSupportOptionsViewEvent,
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
                onClick = viewEvent.onContactUs,
                modifier = buttonModifier
                    .padding(top = ProtonDimens.MediumSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = viewState.contactUsButtonTitle,
                        modifier = Modifier.padding(ProtonDimens.SmallSpacing)
                    )
                }
            }
            ProtonTextButton(
                onClick = viewEvent.onNotNow,
                modifier = buttonModifier
                    .padding(top = ProtonDimens.DefaultSpacing)
            ) {
                Text(
                    text = viewState.notNowButtonTitle,
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
fun ContactSupportOptionsPreview() {
    ProtonTheme {
        ContactSupportOptions(
            viewState = ContactSupportOptionsViewState(
                title = stringResource(I18N.string.support_contact_options_title),
                description = stringResource(I18N.string.support_contact_options_description),
                contactUsButtonTitle = stringResource(I18N.string.support_contact_options_action_button),
                notNowButtonTitle = stringResource(I18N.string.support_contact_options_cancel_button),
            ),
            viewEvent = object : ContactSupportOptionsViewEvent {}
        )
    }
}
