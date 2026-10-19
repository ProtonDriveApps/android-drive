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

package me.proton.android.drive.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.document.scanner.presentation.component.ScanDocumentName
import me.proton.android.drive.document.scanner.presentation.viewevent.ScanDocumentNameViewEvent
import me.proton.android.drive.document.scanner.presentation.viewstate.ScanDocumentNameViewState
import me.proton.android.drive.ui.viewmodel.ScanDocumentNameViewModel
import me.proton.core.compose.activity.KeepScreenOn
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.compose.theme.interactionNorm
import me.proton.core.drive.base.presentation.component.ProtonTextButton
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.protonTextButtonColors
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ScanDocumentNameScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<ScanDocumentNameViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateBack = navigateBack,
        )
    }
    ScanDocumentNameScreen(
        viewState = viewState,
        viewEvent = viewEvent,
    )
}

@Composable
fun ScanDocumentNameScreen(
    viewState: ScanDocumentNameViewState,
    viewEvent: ScanDocumentNameViewEvent,
    modifier: Modifier = Modifier,
) {
    if (viewState.isScheduleUploadInProgress) {
        KeepScreenOn()
    }
    BackHandler { viewEvent.onBackPressed() }
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            viewState = viewState,
            viewEvent = viewEvent,
        )
        ScanDocumentName(
            viewState = viewState,
            viewEvent = viewEvent,
        )
    }
}

@Composable
fun TopAppBar(
    viewState: ScanDocumentNameViewState,
    viewEvent: ScanDocumentNameViewEvent,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    TopAppBar(
        navigationIcon = painterResource(id = CorePresentation.drawable.ic_proton_cross),
        navigationContentDescription = stringResource(I18N.string.common_close_action),
        onNavigationIcon = viewEvent.onBackPressed,
        title = stringResource(id = viewState.titleResId),
        modifier = modifier.statusBarsPadding(),
        actions = {
            ProtonTextButton(
                onClick = {
                    focusManager.clearFocus()
                    viewEvent.onDone()
                },
                enabled = viewState.isDoneEnabled,
                loading = viewState.isScheduleUploadInProgress,
                colors = ButtonDefaults.protonTextButtonColors(
                    backgroundColor = Color.Transparent
                )
            ) {
                Text(
                    text = stringResource(id = viewState.doneButtonLabelResId),
                    style = ProtonTheme.typography.headlineSmallNorm,
                    color = ProtonTheme.colors.interactionNorm(viewState.isDoneEnabled),
                )
            }
        }
    )
}
