/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.usecase.GetDeselectedLogOrigins
import me.proton.core.drive.log.domain.usecase.GetDeselectedLogLevels
import me.proton.core.drive.log.domain.usecase.ToggleLogOrigin
import me.proton.core.drive.log.domain.usecase.ToggleLogLevel
import me.proton.core.drive.log.presentation.entity.LogOriginItem
import me.proton.core.drive.log.presentation.entity.LogLevelItem
import me.proton.core.drive.log.presentation.viewevent.LogOptionsViewEvent
import me.proton.core.drive.log.presentation.viewstate.LogOptionsViewState
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class LogOptionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDeselectedLogLevels: GetDeselectedLogLevels,
    getDeselectedLogOrigins: GetDeselectedLogOrigins,
    private val toggleLogLevel: ToggleLogLevel,
    private val toggleLogOrigin: ToggleLogOrigin,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val logLevelItems: Flow<Set<LogLevelItem>> = getDeselectedLogLevels(userId)
        .transform { deselectedLogLevels ->
            emit(
                Log.Level.entries.map { logLevel ->
                    LogLevelItem(
                        title = logLevel.title,
                        isChecked = !deselectedLogLevels.contains(logLevel),
                        level = logLevel,
                    )
                }
                    .sortedBy { logLevelItem -> logLevelItem.title }
                    .toSet()
            )
        }

    val logOriginItems: Flow<Set<LogOriginItem>> = getDeselectedLogOrigins(userId)
        .transform { deselectedLogOrigins ->
            emit(
                Log.Origin.entries.map { logOrigin ->
                    LogOriginItem(
                        title = logOrigin.title,
                        isChecked = !deselectedLogOrigins.contains(logOrigin),
                        origin = logOrigin,
                    )
                }
                    .sortedBy { logOriginItem -> logOriginItem.title }
                    .toSet()
            )
        }

    val initialViewState = LogOptionsViewState(
        logLevelItemsLabel = I18N.string.log_level,
        logOriginItemsLabel = I18N.string.log_category,
    )

    fun viewEvent() = object : LogOptionsViewEvent {
        override val onLogLevel: (Log.Level) -> Unit = { logLevel -> this@LogOptionsViewModel.onLogLevel(logLevel) }
        override val onLogOrigin: (Log.Origin) -> Unit = { logOrigin -> this@LogOptionsViewModel.onLogOrigin(logOrigin)}
    }

    private fun onLogLevel(logLevel: Log.Level) = viewModelScope.launch {
        toggleLogLevel(userId, logLevel)
            .onFailure { error ->
                error.log(LogTag.LOG)
            }
    }

    private fun onLogOrigin(logOrigin: Log.Origin) = viewModelScope.launch {
        toggleLogOrigin(userId, logOrigin)
            .onFailure { error ->
                error.log(LogTag.LOG)
            }
    }

    private val Log.Level.title: String get() = appContext.getString(
        when (this) {
            Log.Level.NORMAL -> I18N.string.log_level_normal
            Log.Level.WARNING -> I18N.string.log_level_warning
            Log.Level.ERROR -> I18N.string.log_level_error
        }
    )

    private val Log.Origin.title: String get() = appContext.getString(
        when (this) {
            Log.Origin.EVENT_DOWNLOAD -> I18N.string.log_origin_download
            Log.Origin.EVENT_NETWORK -> I18N.string.log_origin_network
            Log.Origin.EVENT_THROWABLE -> I18N.string.log_origin_throwable
            Log.Origin.EVENT_UPLOAD -> I18N.string.log_origin_upload
            Log.Origin.EVENT_LOGGER -> I18N.string.log_origin_logger
        }
    )
}
