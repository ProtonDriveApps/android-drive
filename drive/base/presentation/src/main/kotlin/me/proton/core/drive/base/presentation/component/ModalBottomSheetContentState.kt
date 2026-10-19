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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
@OptIn(ExperimentalMaterial3Api::class)
class ProtonModalBottomSheetState internal constructor(
    internal val sheetState: SheetState,
) {
    var isVisible by mutableStateOf(false)
        private set

    fun show() {
        isVisible = true
    }

    suspend fun hide() {
        try {
            if (isVisible) {
                sheetState.hide()
            }
        } finally {
            isVisible = false
        }
    }

    internal fun onDismissed() {
        isVisible = false
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun rememberProtonModalBottomSheetState(): ProtonModalBottomSheetState {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    return remember(sheetState) { ProtonModalBottomSheetState(sheetState) }
}

@Stable
data class ModalBottomSheetContentState(
    val sheetState: ProtonModalBottomSheetState,
    val sheetContent: MutableState<@Composable ColumnScope.(runAction: RunAction) -> Unit>,
)

@Composable
fun rememberModalBottomSheetContentState(
    modalBottomSheetState: ProtonModalBottomSheetState = rememberProtonModalBottomSheetState(),
    modalBottomSheetContent: MutableState<@Composable ColumnScope.(runAction: RunAction) -> Unit> = mutableStateOf({}),
): ModalBottomSheetContentState = remember {
    ModalBottomSheetContentState(
        modalBottomSheetState,
        modalBottomSheetContent
    )
}
