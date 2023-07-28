/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.folder.create.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.drive.base.presentation.component.OutlinedTextFieldWithError
import me.proton.core.drive.i18n.R as I18N

@Composable
fun CreateFolder(
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<CreateFolderViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    LaunchedEffect(viewModel, LocalContext.current) {
        viewModel.createFolderEffect
            .onEach { effect ->
                when (effect) {
                    CreateFolderEffect.Dismiss -> onDismiss()
                }
            }
            .launchIn(this)
    }
    CreateFolder(
        titleResId = viewState.titleResId,
        folderName = viewState.name,
        selection = viewState.selection,
        showProgress = viewState.inProgress,
        inputError = viewState.error,
        onCreateFolder = viewModel.viewEvent.onCreateFolder,
        onDismiss = onDismiss,
        onValueChanged = viewModel.viewEvent.onNameChanged,
        modifier = Modifier
            .testTag(CreateFolderComponentTestTag.screen),
    )
}

@Composable
fun CreateFolder(
    @StringRes titleResId: Int,
    folderName: String,
    selection: IntRange?,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
    inputError: String? = null,
    onCreateFolder: () -> Unit,
    onDismiss: () -> Unit,
    onValueChanged: (String) -> Unit,
) {
    ProtonAlertDialog(
        titleResId = titleResId,
        modifier = modifier,
        onDismissRequest = onDismiss,
        text = {
            CreateFolderContent(
                folderName = folderName,
                selection = selection,
                onValueChanged = onValueChanged,
                inputError = inputError,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.folder_create_button,
                onClick = { onCreateFolder() },
                loading = showProgress,
            )
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.folder_create_dismiss_button,
                onClick = onDismiss,
            )
        }
    )
}

@Composable
fun CreateFolderContent(
    folderName: String,
    selection: IntRange?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    inputError: String? = null,
    onValueChanged: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
    ) {
        Spacer(modifier = Modifier.size(DefaultSpacing))
        var state by remember {
            mutableStateOf(
                TextFieldValue(
                    text = folderName,
                    selection = if (selection == null) {
                        TextRange.Zero
                    } else {
                        TextRange(selection.first, selection.last)
                    }
                )
            )
        }
        LaunchedEffect(folderName, selection) {
            state = state.copy(
                text = folderName,
                selection = if (selection != null) {
                    TextRange(selection.first, selection.last)
                } else {
                    state.selection
                }
            )
        }
        OutlinedTextFieldWithError(
            textFieldValue = state,
            errorText = inputError,
            focusRequester = focusRequester,
            onValueChanged = { textFieldValue ->
                if (state != textFieldValue) {
                    if (state.text != textFieldValue.text) {
                        onValueChanged(textFieldValue.text)
                    }
                    state = textFieldValue
                }
            },
            modifier = Modifier
                .testTag(CreateFolderComponentTestTag.folderNameTextField),
        )
    }

    LaunchedEffect(focusRequester) {
        awaitFrame()
        // TODO: revise after https://issuetracker.google.com/issues/204502668 is fixed
        delay(100)
        focusRequester.requestFocus()
    }
}

object CreateFolderComponentTestTag {
    const val screen = "create folder screen"
    const val folderNameTextField = "folder name text field"
}
