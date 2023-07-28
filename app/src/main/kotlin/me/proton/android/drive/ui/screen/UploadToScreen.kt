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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.ui.effect.SnackbarEffect
import me.proton.android.drive.ui.viewevent.UploadToViewEvent
import me.proton.android.drive.ui.viewmodel.UploadToViewModel
import me.proton.android.drive.ui.viewstate.UploadToViewState
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.default
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@ExperimentalCoroutinesApi
@Composable
fun UploadToScreen(
    navigateToStorageFull: () -> Unit,
    navigateToCreateFolder: (parentId: FolderId) -> Unit,
    exitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<UploadToViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember(viewModel) {
        viewModel.viewEvent(navigateToStorageFull, navigateToCreateFolder, exitApp)
    }
    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel, LocalContext.current) {
        viewModel.snackbarEffect
            .onEach { effect ->
                when (effect) {
                    is SnackbarEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                        type = effect.type,
                        message = when (effect) {
                            is SnackbarEffect.ShowSnackbar.Message -> effect.message
                            is SnackbarEffect.ShowSnackbar.Resource -> context.getString(effect.resource)
                        }
                    )
                }
            }
            .launchIn(this)
    }
    UploadTo(
        viewState = viewState,
        viewEvent = viewEvent,
        driveLinks = DriveLinksFlow.PagingList(
            value = viewModel.driveLinks,
            effect = viewModel.listEffect
        ),
        snackbarHostState = snackbarHostState,
        exitApp = exitApp,
        modifier = modifier,
    )
}

@Composable
fun UploadTo(
    viewState: UploadToViewState,
    viewEvent: UploadToViewEvent,
    driveLinks: DriveLinksFlow.PagingList,
    snackbarHostState: ProtonSnackbarHostState,
    exitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = viewState.isBackHandlerEnabled) { viewEvent.onTopAppBarNavigation() }

    Box(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .testTag(UploadToScreenTestTag.screen)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                navigationIcon = painterResource(id = viewState.navigationIconResId),
                onNavigationIcon = viewEvent.onTopAppBarNavigation,
                title = viewState.title,
                isTitleEncrypted = viewState.isTitleEncrypted,
            ) {
                ActionButton(
                    modifier = Modifier.testTag(UploadToScreenTestTag.plusFolderButton),
                    icon = CorePresentation.drawable.ic_proton_folder_plus,
                    contentDescription = I18N.string.folder_option_create_folder,
                    onClick = viewEvent.onCreateFolder,
                )
            }
            SingleRowLinkNames(
                linkNames = viewState.fileNames,
                modifier = Modifier
                    .padding(horizontal = DefaultSpacing, vertical = SmallSpacing),
            )
            Files(
                modifier = Modifier.weight(1f),
                driveLinks = driveLinks,
                viewState = viewState.filesViewState,
                viewEvent = viewEvent,
                showTopAppBar = false,
            ) {}
            Row(
                Modifier
                    .align(Alignment.End)
                    .padding(SmallSpacing)
            ) {
                OutlinedButton(
                    onClick = {
                        exitApp()
                    }
                ) {
                    Text(text = stringResource(id = I18N.string.upload_to_dismiss_action))
                }

                Button(
                    modifier = Modifier.padding(start = SmallSpacing),
                    onClick = { viewEvent.upload() }
                ) {
                    Text(text = stringResource(id = I18N.string.upload_title))
                }
            }

        }
        ProtonSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SingleRowLinkNames(
    linkNames: List<String>,
    modifier: Modifier = Modifier,
) {
    val numberOfLinks = linkNames.size
    if (numberOfLinks > 1) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = linkNames.filter { name -> name.isNotBlank() }.joinToString(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = ProtonTheme.typography.default,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = pluralStringResource(
                    id = I18N.plurals.number_of_items,
                    count = numberOfLinks,
                    numberOfLinks,
                ),
                maxLines = 1,
                style = ProtonTheme.typography.captionWeak,
                modifier = Modifier.padding(start = SmallSpacing)
            )
        }
    } else {
        Text(
            text = linkNames.joinToString(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = ProtonTheme.typography.default,
            modifier = modifier.fillMaxWidth()
        )
    }
}

object UploadToScreenTestTag {
    const val screen = "upload to screen"
    const val plusFolderButton = "plus folder button"
}

@Preview
@Composable
private fun PreviewSingleRowFileNames() {
    SingleRowLinkNames(
        linkNames = listOf("first.jpg", "second.pdf", "third.mp4", "fourth.doc", "fifth.txt"),
    )
}
