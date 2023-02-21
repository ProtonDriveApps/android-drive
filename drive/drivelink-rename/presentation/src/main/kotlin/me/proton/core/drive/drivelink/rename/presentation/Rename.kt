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
package me.proton.core.drive.drivelink.rename.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import me.proton.core.drive.drivelink.rename.presentation.selection.Selection

@Composable
fun Rename(
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<RenameViewModel>()
    val renameViewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = null)
    val viewState = renameViewState
    val renameFilenameWithSelection by rememberFlowWithLifecycle(flow = viewModel.nameWithSelection)
        .collectAsState(initial = null)
    val filenameWithSelection = renameFilenameWithSelection
    if (viewState != null && filenameWithSelection != null) {
        var inputError by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(viewModel, LocalContext.current) {
            viewModel.renameEffect
                .onEach { renameEffect ->
                    when (renameEffect) {
                        RenameEffect.Dismiss -> onDismiss()
                        is RenameEffect.ShowInputError -> inputError = renameEffect.message
                        RenameEffect.ClearInputError -> inputError = null
                    }
                }
                .launchIn(this)
        }
        Rename(
            titleResId = viewState.titleResId,
            filename = filenameWithSelection.name,
            selection = filenameWithSelection.selection,
            showProgress = viewState.isRenaming,
            onRename = viewModel.viewEvent.onRename,
            onDismiss = onDismiss,
            onValueChanged = viewModel.viewEvent.onNameChanged,
            inputError = inputError,
        )
    }
}

@Composable
fun Rename(
    @StringRes titleResId: Int,
    filename: String,
    selection: Selection,
    showProgress: Boolean,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    inputError: String? = null,
) {
    val state = remember(filename, selection) {
        mutableStateOf(
            TextFieldValue(
                text = filename,
                selection = TextRange(selection.start, selection.end)
            )
        )
    }
    ProtonAlertDialog(
        modifier = modifier
            .testTag(RenameScreenTestTag.screen),
        onDismissRequest = onDismiss,
        titleResId = titleResId,
        text = {
            RenameContent(
                modifier = modifier
                    .testTag(RenameScreenTestTag.textField),
                textFieldValue = state,
                onValueChanged = onValueChanged,
                inputError = inputError,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.link_rename_button,
                onClick = { onRename(state.value.text) },
                loading = showProgress,
            )
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.link_rename_dismiss_button,
                onClick = onDismiss,
            )
        }
    )
}

@Composable
fun RenameContent(
    textFieldValue: MutableState<TextFieldValue>,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    inputError: String? = null,
    onValueChanged: (String) -> Unit,
) {
    Column(
        modifier = modifier.height(intrinsicSize = IntrinsicSize.Max)
    ) {
        Spacer(Modifier.size(DefaultSpacing))
        OutlinedTextFieldWithError(
            textFieldValue = textFieldValue.value,
            errorText = inputError,
            maxLines = MaxLines,
            focusRequester = focusRequester,
        ) { textField ->
            textFieldValue.value = textField
            onValueChanged(textField.text)
        }
    }

    LaunchedEffect(focusRequester) {
        awaitFrame()
        // TODO: revise after https://issuetracker.google.com/issues/204502668 is fixed
        delay(100)
        focusRequester.requestFocus()
    }
}

private const val MaxLines = 2

object RenameScreenTestTag {
    const val screen = "rename screen"
    const val textField = "rename text field"
}