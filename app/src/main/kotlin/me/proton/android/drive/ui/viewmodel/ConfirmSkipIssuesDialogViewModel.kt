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
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.ConfirmSkipIssuesViewEvent
import me.proton.core.drive.backup.domain.usecase.DeleteAllFailedFiles
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

@HiltViewModel
@Suppress("StaticFieldLeak")
class ConfirmSkipIssuesDialogViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    private val deleteAllFailedFiles: DeleteAllFailedFiles,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val shareId = ShareId(userId, savedStateHandle.require(Screen.BackupIssues.SHARE_ID))
    val folderId: FolderId =
        FolderId(shareId, savedStateHandle.require(Screen.BackupIssues.FOLDER_ID))

    fun viewEvent(
        onConfirm: () -> Unit,
    ): ConfirmSkipIssuesViewEvent = object : ConfirmSkipIssuesViewEvent {
        override val onConfirm = { onSkipConfirmed(onSuccess = onConfirm) }
    }

    private fun onSkipConfirmed(onSuccess: () -> Unit) {
        viewModelScope.launch {
            deleteAllFailedFiles(folderId).onSuccess {
                onSuccess()
            }.onFailure { error ->
                error.log(BACKUP, "Cannot delete failed files")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
        }
    }

}
