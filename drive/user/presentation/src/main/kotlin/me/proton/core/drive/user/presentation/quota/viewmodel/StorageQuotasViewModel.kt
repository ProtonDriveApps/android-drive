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

package me.proton.core.drive.user.presentation.quota.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.user.domain.entity.QuotaLevel
import me.proton.core.drive.user.domain.usecase.CancelQuotaMessage
import me.proton.core.drive.user.domain.usecase.GetQuotaLevel
import me.proton.core.drive.user.domain.usecase.HasCanceledQuotaMessages
import me.proton.core.drive.user.presentation.quota.extension.toState
import me.proton.core.drive.user.presentation.quota.viewevent.StorageQuotasViewEvent
import me.proton.core.drive.user.presentation.quota.viewstate.QuotaViewState
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class StorageQuotasViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    getQuotaLevel: GetQuotaLevel,
    hasCanceledQuotaMessages: HasCanceledQuotaMessages,
    savedStateHandle: SavedStateHandle,
    private val cancelQuotaMessage: CancelQuotaMessage,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val viewState: Flow<QuotaViewState?> = getQuotaLevel(userId).flatMapLatest { level ->
        hasCanceledQuotaMessages(userId, level)
            .map { cancelled ->
                if (cancelled) {
                    null
                } else {
                    level.toState(appContext)
                }
            }
    }

    fun viewEvent(
        getStorage: () -> Unit,
    ): StorageQuotasViewEvent = object : StorageQuotasViewEvent {
        override val onCancel: (QuotaViewState.Level) -> Unit = ::onCancel
        override val onGetStorage: () -> Unit = getStorage
    }

    private fun onCancel(level: QuotaViewState.Level) {
        viewModelScope.launch {
            val quotaLevel = when (level) {
                QuotaViewState.Level.INFO -> QuotaLevel.INFO
                QuotaViewState.Level.WARNING -> QuotaLevel.WARNING
                QuotaViewState.Level.ERROR -> QuotaLevel.ERROR
            }
            cancelQuotaMessage(userId, quotaLevel).onFailure { error ->
                CoreLogger.e(VIEW_MODEL, error, "Cannot cancel storage banner: $level")
            }
        }
    }

}
