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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.drivelink.shared.presentation.viewevent.LinkSettingsViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.LinkSettingsViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LinkSettingsViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PrivacySettingsViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SaveButtonViewState
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun LinkSettings(
    navigateToDiscardChanges: (LinkId) -> Unit,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<LinkSettingsViewModel>()
    val viewState by rememberFlowWithLifecycle(viewModel.viewState)
        .collectAsState(initial = null)
    val viewEvent = remember(viewModel) {
        viewModel.viewEvent(
            navigateToDiscardChanges,
            navigateBack
        )
    }
    val saveButtonViewState by rememberFlowWithLifecycle(viewModel.saveButtonViewState)
        .collectAsState(initial = viewModel.initialSaveButtonViewState)
    LinkSettings(viewState, saveButtonViewState, viewEvent, modifier)
}

@Composable
fun LinkSettings(
    viewState: LinkSettingsViewState?,
    saveButtonViewState: SaveButtonViewState,
    viewEvent: LinkSettingsViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        val focusManager = LocalFocusManager.current
        BackHandler { viewEvent.onBackPressed() }
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = viewEvent.onBackPressed,
            title = stringResource(id = I18N.string.shared_link_settings),
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
                            text = stringResource(id = I18N.string.common_save_action),
                            style = ProtonTheme.typography.headlineSmall,
                            color = ProtonTheme.colors.interactionNorm,
                        )
                    }
                }
            }
        )
        if (viewState == null) {
            LinkSettingsLoading(
                modifier = Modifier.navigationBarsPadding(),
            )
        } else
            LinkSettingsContent(
                viewState = viewState,
                viewEvent = viewEvent,
                modifier = modifier.fillMaxSize(),
            )
    }
}

@Composable
fun LinkSettingsContent(
    viewState: LinkSettingsViewState,
    viewEvent: LinkSettingsViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = LinkSpacing)
            .verticalScroll(rememberScrollState())
            .testTag(SharedDriveLinkTestTag.content)
    ) {
        PrivacySettings(
            viewState = viewState.privacySettingsViewState,
            viewEvent = viewEvent,
            modifier = Modifier.padding(bottom = MediumSpacing),
            title = stringResource(id = I18N.string.shared_link_options)
        )
    }
}

private val LinkSpacing = 20.dp

@Composable
fun LinkSettingsLoading(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Preview
@Composable
private fun LinkSettingsPreview() {
    ProtonTheme {
        LinkSettings(
            viewState = LinkSettingsViewState(
                hasUnsavedChanges = false,
                privacySettingsViewState = PrivacySettingsViewState(
                    enabled = false,
                    password = null,
                    passwordChecked = false,
                    expirationDate = null,
                    expirationDateChecked = false,
                    minDatePickerDate = 0,
                    maxDatePickerDate = 0
                )
            ),
            saveButtonViewState = SaveButtonViewState(
                label = "",
                isVisible = true,
                isEnabled = false,
                inProgress = false
            ),
            viewEvent = object : LinkSettingsViewEvent {
                override val onBackPressed: () -> Unit = {}
                override val onRetry: () -> Unit = {}
                override val onSave: () -> Unit = {}
                override val onPasswordChanged: (String) -> Unit = {}
                override val onPasswordEnabledChanged: (Boolean) -> Unit = {}
                override val onExpirationDateChanged: (Int, Int, Int) -> Unit = { _, _, _ -> }
                override val onExpirationDateEnabledChanged: (Boolean) -> Unit = {}
            }
        )
    }
}
