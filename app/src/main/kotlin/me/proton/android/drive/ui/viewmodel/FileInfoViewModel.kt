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
import kotlinx.coroutines.flow.shareIn
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewstate.FileInfoViewState
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.crypto.domain.usecase.DecryptAncestorsName
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.presentation.extension.getName
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
@SuppressLint("StaticFieldLeak")
class FileInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getDriveLink: GetDecryptedDriveLink,
    private val decryptAncestorsName: DecryptAncestorsName,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId: ShareId = ShareId(userId, savedStateHandle.require(Screen.Info.SHARE_ID))
    private val linkId: LinkId = FileId(shareId, savedStateHandle.require(Screen.Info.LINK_ID))

    val viewState: Flow<FileInfoViewState?> =
        getDriveLink(linkId, failOnDecryptionError = false)
            .mapSuccess { (_, driveLink) ->
                FileInfoViewState(driveLink, driveLink.getParentPath()).asSuccess
            }.mapSuccessValueOrNull()
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private suspend fun DriveLink.getParentPath(): String =
        decryptAncestorsName(id).toResult().map { ancestors ->
            ancestors.dropLast(1).fold("") { path, parent ->
                "$path/${parent.getName(context)}"
            }
        }.getOrElse { "" }
}
