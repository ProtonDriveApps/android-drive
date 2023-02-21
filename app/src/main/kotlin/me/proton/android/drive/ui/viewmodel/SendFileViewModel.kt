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
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import me.proton.core.drive.base.presentation.R
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.isConnectedToNetwork
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileUri
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.base.presentation.R as BasePresentation
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class SendFileViewModel @Inject constructor(
    @ApplicationContext context: Context,
    getDriveLink: GetDecryptedDriveLink,
    getFile: GetFile,
    getFileUri: GetFileUri,
    broadcastMessage: BroadcastMessages,
    savedStateHandle: SavedStateHandle,
    configurationProvider: ConfigurationProvider,
    isConnectedToNetwork: isConnectedToNetwork,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val shareId = ShareId(userId, savedStateHandle.require(Screen.SendFile.SHARE_ID))
    private val fileId = FileId(shareId, savedStateHandle.require(Screen.SendFile.FILE_ID))

    val driveLink = getDriveLink(fileId, failOnDecryptionError = false)
        .onEach { result ->
            if (result is DataResult.Error) {
                broadcastMessage(
                    userId = userId,
                    message = context.getString(R.string.error_could_not_send_file),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
        }
        .mapSuccessValueOrNull()
        .filterNotNull()
        .take(1)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val downloadState = driveLink.filterNotNull().flatMapLatest { driveLink ->
        if (driveLink.size <= configurationProvider.maxFileSizeToSendWithoutDownload && isConnectedToNetwork()) {
            flowOf(ShareState.Ready(getFileUri(userId, fileId), driveLink.mimeType))
        } else {
            flow<ShareState> {
                emit(ShareState.Downloading(emptyFlow()))
                emitAll(
                    getFile(driveLink, checkSignature = false).map { state ->
                        when (state) {
                            GetFile.State.Decrypting -> ShareState.Decrypting
                            is GetFile.State.Downloading -> ShareState.Downloading(progress = state.progress)
                            is GetFile.State.Error.Decrypting,
                            is GetFile.State.Error.Downloading,
                            GetFile.State.Error.NotFound -> {
                                broadcastMessage(
                                    userId = userId,
                                    message = context.getString(R.string.error_could_not_send_file),
                                    type = BroadcastMessage.Type.ERROR,
                                )
                                ShareState.Error
                            }
                            GetFile.State.Error.NoConnection -> {
                                broadcastMessage(
                                    userId = userId,
                                    message = context.getString(BasePresentation.string.description_file_download_failed),
                                    type = BroadcastMessage.Type.ERROR,
                                )
                                ShareState.Error
                            }
                            is GetFile.State.Error.VerifyingSignature -> ShareState.Error
                            is GetFile.State.Ready -> ShareState.Ready(getFileUri(userId, fileId), driveLink.mimeType)
                        }
                    }
                )
            }
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
}

sealed class ShareState {
    object Decrypting : ShareState()
    data class Downloading(val progress: Flow<Percentage>) : ShareState()
    object Error : ShareState()
    data class Ready(val uri: Uri, val mimeType: String) : ShareState()
}
