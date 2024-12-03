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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.ShareLinkPermissionsOption
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.shared.domain.entity.SharedDriveLink
import me.proton.core.drive.drivelink.shared.domain.usecase.GetSharedDriveLink
import me.proton.core.drive.drivelink.shared.presentation.entry.ShareUserOptionEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.crypto.domain.usecase.UpdateShareUrlPermissions
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareLinkPermissionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDriveLink,
    getSharedDriveLink: GetSharedDriveLink,
    private val updateShareUrlPermissions: UpdateShareUrlPermissions,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val linkId = FileId(
        ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        savedStateHandle.require(KEY_LINK_ID)
    )

    private val driveLink: Flow<DriveLink?> = getDriveLink(linkId = linkId).mapSuccessValueOrNull()

    private val sharedDriveLink = driveLink.filterNotNull().transformLatest { driveLink ->
        emitAll(getSharedDriveLink(driveLink))
    }.mapSuccessValueOrNull()

    fun entries(
        runAction: RunAction,
    ): Flow<List<ShareUserOptionEntry>> = sharedDriveLink.filterNotNull().map { sharedDriveLink ->
        options.map { option ->
            when (option) {
                is ShareLinkPermissionsOption.PermissionsViewer -> option.build(
                    isSelected = sharedDriveLink.permissions == Permissions.viewer,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(sharedDriveLink, Permissions.viewer)
                    }
                }

                is ShareLinkPermissionsOption.PermissionsEditor -> option.build(
                    isSelected = sharedDriveLink.permissions == Permissions.editor,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(sharedDriveLink, Permissions.editor)
                    }
                }
            }
        }
    }

    private suspend fun updatePermissions(
        sharedDriveLink: SharedDriveLink,
        permissions: Permissions,
    ) {
        updateShareUrlPermissions(
            sharedDriveLink.volumeId,
            sharedDriveLink.shareUrlId,
            permissions = permissions,
        ).onFailure { error ->
            error.log(SHARING)
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    context = appContext,
                    useExceptionMessage = configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.ERROR,
            )
        }
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"

        private val options = setOf(
            ShareLinkPermissionsOption.PermissionsViewer,
            ShareLinkPermissionsOption.PermissionsEditor,
        )
    }
}
