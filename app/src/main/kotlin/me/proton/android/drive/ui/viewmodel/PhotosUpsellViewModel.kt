/*
 * Copyright (c) 2023-2024 Proton AG.
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
import me.proton.android.drive.photos.presentation.viewevent.PhotosUpsellViewEvent
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.telemetry.domain.event.PhotosEvent.UpsellPhotosAccepted
import me.proton.core.drive.telemetry.domain.event.PhotosEvent.UpsellPhotosDeclined
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryManager
import me.proton.core.drive.user.domain.entity.UserMessage
import me.proton.core.drive.user.domain.usecase.CancelUserMessage
import javax.inject.Inject

@HiltViewModel
class PhotosUpsellViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cancelUserMessage: CancelUserMessage,
    private val driveTelemetryManager: DriveTelemetryManager,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    fun viewEvent(
        runAction: RunAction,
        navigateToSubscription: () -> Unit,
    ): PhotosUpsellViewEvent = object : PhotosUpsellViewEvent {
        override val onMoreStorage = {
            runAction {
                navigateToSubscription()
                driveTelemetryManager.enqueue(userId, UpsellPhotosAccepted())
            }
        }
        override val onCancel = {
            runAction {
                driveTelemetryManager.enqueue(userId, UpsellPhotosDeclined())
            }
        }
        override val onDismiss = {
            viewModelScope.launch {
                cancelUserMessage(userId, UserMessage.UPSELL_PHOTOS).onFailure { error ->
                    error.log(PHOTO, "Cannot cancel upsell photos message")
                }
            }
            Unit
        }
    }
}
