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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.base.domain.usecase.SignOut
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.data.DriveDocumentsProvider
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class SigningOutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signOut: SignOut,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    suspend fun signOut() {
        signOut(userId)
        DriveDocumentsProvider.notifyRootsHaveChanged(context)
    }
}
