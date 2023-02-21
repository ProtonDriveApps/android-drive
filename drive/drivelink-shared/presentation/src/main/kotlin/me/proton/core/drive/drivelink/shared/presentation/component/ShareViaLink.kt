/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonErrorMessageWithAction
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.shared.presentation.viewevent.SharedDriveLinkViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.SharedDriveLinkViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LoadingViewState
import me.proton.core.drive.link.domain.entity.LinkId
import kotlin.time.Duration
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ShareViaLink(
    navigateToStopSharing: (LinkId) -> Unit,
    navigateToDiscardChanges: (LinkId) -> Unit,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<SharedDriveLinkViewModel>()
    val viewState by rememberFlowWithLifecycle(viewModel.sharedLoadingViewState)
        .collectAsState(initial = LoadingViewState.Initial)
    ShareViaLink(
        viewModel = viewModel,
        viewState = viewState,
        viewEvent = viewModel.viewEvent(navigateToStopSharing, navigateToDiscardChanges, navigateBack),
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun ShareViaLink(
    viewModel: SharedDriveLinkViewModel,
    viewState: LoadingViewState,
    viewEvent: SharedDriveLinkViewEvent,
    modifier: Modifier = Modifier,
) {
    val saveButtonViewState by rememberFlowWithLifecycle(viewModel.saveButtonViewState)
        .collectAsState(initial = viewModel.initialSaveButtonViewState)
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        val focusManager = LocalFocusManager.current
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = viewEvent.onBackPressed,
            title = stringResource(id = BasePresentation.string.title_share_via_link),
            modifier = Modifier.statusBarsPadding(),
            actions = {
                if (saveButtonViewState.isVisible) {
                    ProtonTextButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewEvent.onSave()
                        },
                        enabled = saveButtonViewState.isEnabled,
                        loading = saveButtonViewState.inProgress,
                        colors = ButtonDefaults.protonTextButtonColors(
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = stringResource(id = BasePresentation.string.common_save_action),
                            style = ProtonTheme.typography.headlineSmall,
                            color = ProtonTheme.colors.interactionNorm,
                        )
                    }
                }
            }
        )
        when (viewState) {
            LoadingViewState.Initial -> Unit
            is LoadingViewState.Loading -> ShareViaLinkLoading(
                message = viewState.message,
                modifier = Modifier.navigationBarsPadding(),
            )
            is LoadingViewState.Error.Retryable -> ShareViaLinkErrorWithRetry(
                message = viewState.message,
                modifier = Modifier.navigationBarsPadding(),
                retry = viewEvent.onRetry,
            )
            is LoadingViewState.Error.NonRetryable -> ShareViaLinkError(
                message = viewState.message,
                deferDuration = viewState.defer,
                modifier = Modifier.navigationBarsPadding(),
            )
            is LoadingViewState.Available -> ShareViaLink(
                viewModel = viewModel,
                viewEvent = viewEvent,
                driveLink = viewState.driveLink,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
fun ShareViaLink(
    viewModel: SharedDriveLinkViewModel,
    viewEvent: SharedDriveLinkViewEvent,
    driveLink: DriveLink,
    modifier: Modifier = Modifier,
) {
    val sharedDriveLinkViewState by rememberFlowWithLifecycle(viewModel.viewState)
        .collectAsState(initial = null)
    sharedDriveLinkViewState?.let { viewState ->
        SharedDriveLink(
            viewState = viewState,
            viewEvent = viewEvent,
            driveLinkId = driveLink.id,
            modifier = modifier,
        )
    }
}

@Composable
fun ShareViaLinkLoading(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = ProtonTheme.typography.default,
            modifier = Modifier.padding(top = MediumSpacing)
        )
    }
}

@Composable
fun ShareViaLinkErrorWithRetry(
    message: String,
    modifier: Modifier = Modifier,
    retry: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(all = DefaultSpacing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ProtonErrorMessageWithAction(
            errorMessage = message,
            action = stringResource(BasePresentation.string.common_retry_action),
            onAction = retry
        )
    }
}

@Composable
fun ShareViaLinkError(
    message: String,
    deferDuration: Duration,
    modifier: Modifier = Modifier,
) {
    Deferred(
        duration = deferDuration,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(all = DefaultSpacing),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ProtonErrorMessage(
                errorMessage = message,
            )
        }
    }
}
