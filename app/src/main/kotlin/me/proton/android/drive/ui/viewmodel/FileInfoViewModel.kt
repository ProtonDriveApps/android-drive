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

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewstate.FileInfoViewState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.crypto.domain.usecase.DecryptAncestorsName
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.UpdateDriveLinkDisplayName
import me.proton.core.drive.file.info.presentation.extension.toItems
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
@SuppressLint("StaticFieldLeak")
class FileInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getDriveLink: GetDecryptedDriveLink,
    getShare: GetShare,
    private val decryptAncestorsName: DecryptAncestorsName,
    private val updateDriveLinkDisplayName: UpdateDriveLinkDisplayName,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId: ShareId = ShareId(userId, savedStateHandle.require(Screen.Info.SHARE_ID))
    private val linkId: LinkId = FileId(shareId, savedStateHandle.require(Screen.Info.LINK_ID))

    private val driveLink = getDriveLink(linkId, failOnDecryptionError = false)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val share = getShare(shareId, flowOf(false))
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val viewState: Flow<FileInfoViewState?> = combine(
        share.filterNotNull(),
        driveLink.filterNotNull(),
    ) { share, driveLink ->
        FileInfoViewState(
            link = driveLink,
            items = driveLink.toItems(
                context = context,
                parentPath = driveLink.getParentPath(),
                capturedOn = (driveLink as? DriveLink.File)?.photoCaptureTime,
                shareType = share.type,
            ),
        )
    }

    private suspend fun DriveLink.getParentPath(): String =
        decryptAncestorsName(id).toResult().map { ancestors ->
            ancestors.dropLast(1).fold("") { path, parent ->
                "$path/${updateDriveLinkDisplayName(parent).name}"
            }
        }.getOrElse { "" }
}
