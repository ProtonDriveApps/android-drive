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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.UpdateSorting
import javax.inject.Inject

@HiltViewModel
class SortingDialogViewModel @Inject constructor(
    private val updateSorting: UpdateSorting,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val sorting = Sorting(
        savedStateHandle.require(Screen.Sorting.BY),
        savedStateHandle.require(Screen.Sorting.DIRECTION)
    )

    suspend fun setSorting(sorting: Sorting) {
        viewModelScope.launch {
            updateSorting(userId, sorting)
        }.join()
    }
}
