/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.base.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState

typealias RunAction = (action: suspend () -> Unit) -> Unit

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ModalBottomSheet(
    sheetState: ModalBottomSheetState,
    sheetContent: @Composable ColumnScope.(runAction: RunAction) -> Unit,
    viewState: ModalBottomSheetViewState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    BackHandler(viewState.closeOnBack && sheetState.isVisible) {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ProtonModalBottomSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        sheetContent = {
            Spacer(modifier = Modifier.height(RoundedCornerHeight))
            if (sheetState.isVisible) {
                sheetContent { action: suspend () -> Unit ->
                    runAction(scope, sheetState, viewState, onDismiss, action)
                }
            }
        },
        content = content
    )
}

@OptIn(ExperimentalMaterialApi::class)
private fun runAction(
    scope: CoroutineScope,
    modalBottomSheetState: ModalBottomSheetState,
    viewState: ModalBottomSheetViewState,
    onDismiss: () -> Unit,
    action: suspend () -> Unit,
) {
    // Force action to run on the main dispatcher (a confined dispatcher).
    // Avoid Room to change thread a never switch back to main in ui tests.
    // https://issuetracker.google.com/issues/254115946
    scope.launch(Dispatchers.Main) {
        if (viewState.closeOnAction) {
            modalBottomSheetState.hide()
        }
        action()
        if (viewState.dismissOnAction) {
            onDismiss()
        }
    }
}

private val RoundedCornerHeight = 8.dp
