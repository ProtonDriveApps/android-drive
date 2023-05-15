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
package me.proton.android.drive.lock.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.proton.android.drive.lock.domain.exception.LockException
import me.proton.android.drive.lock.domain.usecase.UnlockApp
import me.proton.android.drive.lock.presentation.viewevent.UnlockViewEvent
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.SignOut
import me.proton.android.drive.lock.presentation.extension.getDefaultMessage
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.getDefaultMessage
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject

@Suppress("StaticFieldLeak")
@HiltViewModel
class UnlockViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val signOut: SignOut,
    private val unlockApp: UnlockApp,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel() {

    val viewEvent = object : UnlockViewEvent {
        override val onShowBiometric: (userId: UserId) -> Unit = { userId -> showBiometrics(userId) }
        override val onSignOut: (userId: UserId) -> Unit = { userId -> doSignOut(userId) }
    }

    private fun showBiometrics(userId: UserId) = viewModelScope.launch {
        unlockApp()
            .onFailure { error ->
                broadcastMessages(
                    userId = userId,
                    message = when (error) {
                        is LockException -> error.getDefaultMessage(appContext)
                        else -> error.getDefaultMessage(appContext, configurationProvider.useExceptionMessage)
                    },
                    type = BroadcastMessage.Type.WARNING,
                )
            }
    }

    private fun doSignOut(userId: UserId) = viewModelScope.launch {
        signOut(userId)
    }
}
