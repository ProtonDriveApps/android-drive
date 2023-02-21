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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import me.proton.android.drive.ui.viewstate.LauncherViewState
import me.proton.android.drive.ui.viewstate.PrimaryAccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.drive.documentsprovider.data.DriveDocumentsProvider
import me.proton.drive.android.settings.domain.usecase.HasShownWelcome
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
@Suppress("StaticFieldLeak")
class LauncherViewModel @Inject constructor(
    accountManager: AccountManager,
    @ApplicationContext private val context: Context,
    private val hasShownWelcome: HasShownWelcome,
) : ViewModel() {
    private var currentViewState = LauncherViewState.initialValue

    val viewState: Flow<LauncherViewState> =
        accountManager.getPrimaryAccount()
            .transform { account ->
                currentViewState = account?.userId?.let { userId ->
                    DriveDocumentsProvider.notifyRootsHaveChanged(context)
                    LauncherViewState(PrimaryAccountState.SignedIn(userId))
                } ?: LauncherViewState(PrimaryAccountState.SigningIn)
                emit(currentViewState)
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    suspend fun hasShownWelcomeFlow() = hasShownWelcome()
}
