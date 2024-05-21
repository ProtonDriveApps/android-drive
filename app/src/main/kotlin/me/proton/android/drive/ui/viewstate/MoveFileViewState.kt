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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.files.presentation.state.FilesViewState

data class MoveFileViewState(
    val filesViewState: FilesViewState,
    val isMoveButtonEnabled: Boolean,
    val title: String,
    val isTitleEncrypted: Boolean = false,
    val navigationIconResId: Int = 0,
    val driveLinks: List<String> = emptyList(),
    val topBarActions: Flow<Set<Action>> = emptyFlow(),
)
