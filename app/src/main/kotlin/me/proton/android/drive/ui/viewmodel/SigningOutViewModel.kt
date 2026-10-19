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
import me.proton.android.drive.auth.SignOutAuthorization
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.SignOut
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.data.DriveDocumentsProvider
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class SigningOutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signOut: SignOut,
    signOutAuthorization: SignOutAuthorization,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val isAuthorized = signOutAuthorization.consume(userId)

    suspend fun signOut(onUnauthorized: () -> Unit) {
        if (!isAuthorized) {
            CoreLogger.w(VIEW_MODEL, "Sign out was not confirmed by the user, ignoring")
            onUnauthorized()
            return
        }
        signOut(userId)
        DriveDocumentsProvider.notifyRootsHaveChanged(context)
    }
}
