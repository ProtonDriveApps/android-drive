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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.ConfirmStopSharingViewEvent
import me.proton.android.drive.ui.viewstate.ConfirmStopSharingViewState
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.shared.domain.usecase.DeleteShareUrl
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@ExperimentalCoroutinesApi
@Suppress("StaticFieldLeak")
class ConfirmStopSharingDialogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deleteShareUrl: DeleteShareUrl,
    private val broadcastMessages: BroadcastMessages,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val shareId = ShareId(userId, savedStateHandle.require(Screen.Files.Dialogs.ConfirmStopSharing.SHARE_ID))
    val linkId: LinkId = FileId(shareId, savedStateHandle.require(Screen.Files.Dialogs.ConfirmStopSharing.LINK_ID))

    val initialViewState = ConfirmStopSharingViewState()
    private val isLoading = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(initialViewState.isLoading) }
    private val errorMessage = MutableSharedFlow<String?>().apply { tryEmit(initialViewState.errorMessage) }
    val viewState = combine(isLoading, errorMessage) { isLoading, errorMessage ->
        ConfirmStopSharingViewState(isLoading, errorMessage)
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    fun viewEvent(confirm: () -> Unit) = object : ConfirmStopSharingViewEvent {
        override val onConfirm = {
            viewModelScope.launch {
                isLoading.emit(true)
                errorMessage.emit(null)
                val result = deleteShareUrl(linkId)
                isLoading.emit(false)
                result
                    .onSuccess {
                        broadcastMessages(
                            userId = linkId.userId,
                            message = context.getString(I18N.string.description_files_stop_sharing_action_success),
                            type = BroadcastMessage.Type.SUCCESS,
                        )
                        confirm()
                    }.onFailure { error ->
                        errorMessage.emit(context.getString(I18N.string.description_files_stop_sharing_action_error))
                        error.log(VIEW_MODEL)
                    }
            }
            Unit
        }
    }
}
