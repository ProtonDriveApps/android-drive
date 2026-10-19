/*
 * Copyright (c) 2026 Proton AG.
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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonTheme
import androidx.compose.material3.ModalBottomSheet as Material3ModalBottomSheet

typealias RunAction = (action: suspend () -> Unit) -> Unit

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ModalBottomSheet(
    sheetState: ProtonModalBottomSheetState,
    sheetContent: @Composable ColumnScope.(runAction: RunAction) -> Unit,
    viewState: ModalBottomSheetViewState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    content()

    if (sheetState.isVisible) {
        Material3ModalBottomSheet(
            onDismissRequest = {
                sheetState.onDismissed()
                onDismiss()
            },
            modifier = modifier.statusBarsPadding(),
            sheetState = sheetState.sheetState,
            shape = ProtonTheme.shapes.bottomSheet,
            containerColor = ProtonTheme.colors.backgroundNorm,
            contentColor = ProtonTheme.colors.textNorm,
            scrimColor = ProtonTheme.colors.blenderNorm,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = viewState.closeOnBack,
                shouldDismissOnClickOutside = true,
            ),
        ) {
            Spacer(modifier = Modifier.height(RoundedCornerHeight))
            sheetContent { action: suspend () -> Unit ->
                runAction(scope, sheetState, viewState, onDismiss, action)
            }
        }
    }
}

private fun runAction(
    scope: CoroutineScope,
    modalBottomSheetState: ProtonModalBottomSheetState,
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
        onDismiss()
    }
}

private val RoundedCornerHeight = 8.dp
