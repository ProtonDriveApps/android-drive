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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import me.proton.core.compose.component.bottomsheet.RunAction

@Stable
@OptIn(ExperimentalMaterialApi::class)
data class ModalBottomSheetContentState(
    val sheetState: ModalBottomSheetState,
    val sheetContent: MutableState<@Composable ColumnScope.(runAction: RunAction) -> Unit>,
)

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun rememberModalBottomSheetContentState(
    modalBottomSheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    modalBottomSheetContent: MutableState<@Composable ColumnScope.(runAction: RunAction) -> Unit> = mutableStateOf({}),
): ModalBottomSheetContentState = remember {
    ModalBottomSheetContentState(
        modalBottomSheetState,
        modalBottomSheetContent
    )
}
