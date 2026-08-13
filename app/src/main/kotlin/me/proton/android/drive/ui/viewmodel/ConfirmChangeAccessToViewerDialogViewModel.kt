/*
 * Copyright (c) 2026 Proton AG.
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.viewevent.ConfirmChangeAccessToViewerViewEvent
import me.proton.android.drive.ui.viewstate.ConfirmChangeAccessToViewerViewState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.user.domain.usecase.UpdateMemberPermissions
import javax.inject.Inject
import me.proton.core.drive.base.data.extension.log as logResult
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("StaticFieldLeak")
class ConfirmChangeAccessToViewerDialogViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDecryptedDriveLink: GetDecryptedDriveLink,
    private val updateMemberPermissions: UpdateMemberPermissions,
    private val configurationProvider: ConfigurationProvider,
    private val getShare: GetShare,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val memberId: String = savedStateHandle.require(KEY_MEMBER_ID)
    private val linkId = FileId(
        ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        savedStateHandle.require(KEY_LINK_ID)
    )
    private val driveLink: StateFlow<DriveLink?> = getDecryptedDriveLink(linkId = linkId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val initialViewState = ConfirmChangeAccessToViewerViewState()

    val viewState: Flow<ConfirmChangeAccessToViewerViewState> = combine(
        driveLink.filterNotNull(),
        isLoading,
        errorMessage,
    ) { driveLink, isLoading, errorMessage ->
        initialViewState.copy(
            description = appContext.getString(
                I18N.string.manage_access_change_access_to_viewer_description,
                driveLink.name,
            ),
            isLoading = isLoading,
            errorMessage = errorMessage,
        )
    }

    fun viewEvent(
        onDismiss: () -> Unit,
    ): ConfirmChangeAccessToViewerViewEvent = object : ConfirmChangeAccessToViewerViewEvent {
        override val onConfirm = {
            viewModelScope.launch {
                changeAccessToViewer(onDismiss)
            }
            Unit
        }
    }

    private suspend fun changeAccessToViewer(dismiss: () -> Unit) {
        val driveLink = driveLink.value ?: return
        val shareId = driveLink.sharingDetails?.shareId ?: return
        isLoading.value = true
        errorMessage.value = null
        updateMemberPermissions(
            shareId = shareId,
            memberId = memberId,
            permissions = Permissions.viewer,
        ).filterSuccessOrError()
            .last()
            .onSuccess {
                getShare(
                    shareId = linkId.shareId,
                    refresh = flowOf(true),
                ).toResult().getOrNull(
                    tag = VIEW_MODEL,
                    message = "Failed to get share for ${linkId.shareId.id.logId()}",
                )
                dismiss()
            }
            .onFailure { error ->
                error.logResult(VIEW_MODEL, "Failed to update member permissions for $memberId")
                errorMessage.value = error.getDefaultMessage(
                    context = appContext,
                    useExceptionMessage = configurationProvider.useExceptionMessage,
                )
            }
        isLoading.value = false
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"
        const val KEY_MEMBER_ID = "memberId"
    }
}
