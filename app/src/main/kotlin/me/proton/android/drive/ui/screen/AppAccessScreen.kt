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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewevent.AppAccessViewEvent
import me.proton.android.drive.ui.viewmodel.AppAccessViewModel
import me.proton.android.drive.ui.viewstate.AccessOption
import me.proton.android.drive.ui.viewstate.AppAccessViewState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.ProtonListItem
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
fun AppAccessScreen(
    modifier: Modifier = Modifier,
    navigateToSystemAccess: () -> Unit,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<AppAccessViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    AppAccess(
        viewState = viewState,
        viewEvent = viewModel.viewEvent(navigateToSystemAccess, navigateBack),
        modifier = modifier.fillMaxSize(),
        navigateBack = navigateBack,
    )
}

@Composable
fun AppAccess(
    viewState: AppAccessViewState,
    viewEvent: AppAccessViewEvent,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
            title = viewState.title,
            modifier = Modifier.statusBarsPadding()
        )
        AppAccessOptions(viewState.enabledOption, viewEvent)
    }
}

@Composable
fun AppAccessOptions(
    enabledOption: AccessOption,
    viewEvent: AppAccessViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        AppAccessOption(
            iconResId = BasePresentation.drawable.ic_proton_lock_open,
            titleResId = BasePresentation.string.app_lock_option_none,
            isSelected = enabledOption == AccessOption.NONE,
        ) {
            viewEvent.onDisable()
        }
        AppAccessOption(
            iconResId = CorePresentation.drawable.ic_proton_fingerprint,
            titleResId = BasePresentation.string.app_lock_option_system,
            isSelected = enabledOption == AccessOption.SYSTEM,
        ) {
            viewEvent.onSystem()
        }
    }
}

@Composable
fun AppAccessOption(
    iconResId: Int,
    titleResId: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProtonListItem(
            icon = painterResource(id = iconResId),
            title = stringResource(id = titleResId),
            modifier = modifier
                .weight(1f)
                .padding(start = DefaultSpacing),
        )
        RadioButton(
            selected = isSelected,
            onClick = { onClick() },
        )
    }
}

@Preview
@Composable
private fun AppAccessPreview() {
    ProtonTheme {
        AppAccess(
            viewState = AppAccessViewState(
                title = "Title",
                enabledOption = AccessOption.NONE,
            ),
            viewEvent = object : AppAccessViewEvent {
                override val onDisable = {}
                override val onSystem = {}
            },
            navigateBack = {}
        )
    }
}

