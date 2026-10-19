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

package me.proton.android.drive.ui.viewstate

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import me.proton.core.drive.base.presentation.component.ModalBottomSheetContentState
import me.proton.core.drive.base.presentation.component.rememberModalBottomSheetContentState

@Stable
data class HomeScaffoldState(
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState,
    val drawerGesturesEnabled: MutableState<Boolean>,
    val modalBottomSheetContentState: ModalBottomSheetContentState,
    val topAppBar: MutableState<@Composable () -> Unit>,
    val bottomNavigationEnabled: MutableState<Boolean>,
)

@Composable
fun rememberHomeScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    drawerGesturesEnabled: MutableState<Boolean> = mutableStateOf(true),
    modalBottomSheetContentState: ModalBottomSheetContentState = rememberModalBottomSheetContentState(),
    topAppBar: MutableState<@Composable () -> Unit> = mutableStateOf({}),
    bottomNavigationEnabled: MutableState<Boolean> = mutableStateOf(true),
): HomeScaffoldState = remember {
    HomeScaffoldState(
        drawerState,
        snackbarHostState,
        drawerGesturesEnabled,
        modalBottomSheetContentState,
        topAppBar,
        bottomNavigationEnabled,
    )
}
