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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.android.drive.R
import me.proton.android.drive.ui.effect.SnackbarEffect
import me.proton.android.drive.ui.viewmodel.MoveToFolderViewModel
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.presentation.R as CorePresentation

@ExperimentalCoroutinesApi
@Composable
fun MoveToFolder(
    modifier: Modifier = Modifier,
    navigateToCreateFolder: (parentId: FolderId) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val viewModel = hiltViewModel<MoveToFolderViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember(viewModel, onDismissRequest) { viewModel.viewEvent(onDismissRequest) }
    val snackbarHostState = remember { ProtonSnackbarHostState() }

    val context = LocalContext.current
    LaunchedEffect(viewModel, LocalContext.current) {
        viewModel.snackbarEffect
            .onEach { effect ->
                when (effect) {
                    is SnackbarEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                        type = ProtonSnackbarType.ERROR,
                        message = when (effect) {
                            is SnackbarEffect.ShowSnackbar.Message -> effect.message
                            is SnackbarEffect.ShowSnackbar.Resource -> context.getString(effect.resource)
                        }
                    )
                }
            }
            .launchIn(this)
    }

    BackHandler { viewEvent.onTopAppBarNavigation() }

    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier
            .systemBarsPadding()
            .padding(vertical = DefaultSpacing)
            .testTag(MoveToFolderScreenTestTag.screen)
    ) {
        Column {
            Title(
                title = viewState.title,
                isTitleEncrypted = viewState.isTitleEncrypted,
                modifier = Modifier.padding(horizontal = DefaultSpacing),
            )
            Files(
                modifier = Modifier.weight(1f),
                driveLinks = DriveLinksFlow.PagingList(
                    value = viewModel.driveLinks,
                    effect = viewModel.listEffect
                ),
                viewState = viewState.filesViewState,
                viewEvent = viewEvent,
            ) {
                ActionButton(
                    modifier = Modifier.testTag(MoveToFolderScreenTestTag.plusFolderButton),
                    icon = CorePresentation.drawable.ic_proton_folder_plus,
                    contentDescription = R.string.folder_option_create_folder,
                    onClick = { viewModel.onCreateFolder(navigateToCreateFolder) }
                )
            }

            Row(Modifier
                .align(Alignment.End)
                .padding(SmallSpacing)
            ) {
                OutlinedButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = R.string.move_file_dismiss_action))
                }

                Button(
                    modifier = Modifier.padding(start = SmallSpacing),
                    enabled = viewState.isMoveButtonEnabled,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.confirmMove()
                            onDismissRequest()
                        }
                    }) {
                    Text(stringResource(id = R.string.move_file_confirm_action))
                }
            }

        }
        ProtonSnackbarHost(
            modifier = Modifier.align(Alignment.BottomCenter),
            hostState = snackbarHostState,
        )
    }
}

@Composable
fun Title(
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier
) {
    if (isTitleEncrypted) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            TitleText(
                title = stringResource(id = R.string.move_encrypted_file_title_prefix),
                modifier = Modifier.padding(end = SmallSpacing),
            )
            EncryptedItem(
                modifier = Modifier.weight(1f)
            )
            TitleText(
                title = stringResource(id = R.string.move_encrypted_file_title_suffix),
                modifier = Modifier.padding(start = SmallSpacing),
            )
        }
    } else {
        TitleText(
            title = title,
            modifier = modifier,
        )
    }
}

@Composable
fun TitleText(
    title: String,
    modifier: Modifier = Modifier,
) {
    TextWithMiddleEllipsis(
        text = title,
        style = MaterialTheme.typography.h6,
        color = MaterialTheme.colors.onBackground,
        maxLines = 1,
        modifier = modifier,
    )
}

object MoveToFolderScreenTestTag {
    const val screen = "move to folder screen"
    const val plusFolderButton = "move to folder plus folder button"
}
