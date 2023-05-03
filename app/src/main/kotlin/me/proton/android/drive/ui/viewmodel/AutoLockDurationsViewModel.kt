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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.lock.domain.usecase.GetAutoLockDuration
import me.proton.android.drive.lock.domain.usecase.UpdateAutoLockDuration
import me.proton.android.drive.ui.viewevent.AutoLockDurationsViewEvent
import me.proton.android.drive.ui.viewstate.AutoLockDurationsViewState
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject
import kotlin.time.Duration
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class AutoLockDurationsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    configurationProvider: ConfigurationProvider,
    getAutoLockDuration: GetAutoLockDuration,
    private val updateAutoLockDuration: UpdateAutoLockDuration,
) : ViewModel() {
    val initialViewState: AutoLockDurationsViewState = AutoLockDurationsViewState(
        title = appContext.getString(I18N.string.settings_auto_lock),
        durations = configurationProvider.autoLockDurations,
        selected = configurationProvider.autoLockDurations.first(),
    )
    val viewState: Flow<AutoLockDurationsViewState> = getAutoLockDuration().map { duration ->
        initialViewState.copy(
            selected = duration
        )
    }

    fun viewEvent(
        runAction: RunAction,
        dismiss: () -> Unit,
    ): AutoLockDurationsViewEvent = object : AutoLockDurationsViewEvent {
        override val onDuration: (Duration) -> Unit = { duration ->
            runAction {
                viewModelScope.launch {
                    updateAutoLockDuration(duration)
                    dismiss()
                }
            }
        }
    }
}
