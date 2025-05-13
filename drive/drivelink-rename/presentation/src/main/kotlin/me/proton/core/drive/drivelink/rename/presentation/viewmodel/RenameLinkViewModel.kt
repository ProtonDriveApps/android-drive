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

package me.proton.core.drive.drivelink.rename.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.ellipsizeMiddle
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.rename.domain.usecase.RenameAlbum
import me.proton.core.drive.drivelink.rename.domain.usecase.RenameLink
import me.proton.core.drive.drivelink.rename.presentation.RenameEffect
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class RenameLinkViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getDriveLink: GetDecryptedDriveLink,
    private val renameLink: RenameLink,
    private val renameAlbum: RenameAlbum,
    private val broadcastMessages: BroadcastMessages,
) : RenameViewModel(appContext, savedStateHandle) {
    private val unused = getDriveLink(linkId)
        .filterSuccessOrError()
        .map { driveLinkResult ->
            name.emit(
                savedStateHandle.get<String>(KEY_FILENAME)
                    ?: driveLinkResult.name
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val titleResId: Int get() = when (linkId) {
        is FileId -> I18N.string.link_rename_title_file
        is FolderId -> I18N.string.link_rename_title_folder
        is AlbumId -> I18N.string.link_rename_title_album
    }

    private val DataResult<DriveLink>.name get() =
        toResult()
            .getOrNull()
            ?.takeUnless { link -> link.isNameEncrypted }
            ?.name
            ?: ""

    override suspend fun doRenameFile(name: String) {
        val renameBlock: suspend () -> Result<Unit> = {
            if (linkId is AlbumId) {
                val driveLink = getDriveLink(linkId).toResult().getOrThrow()
                renameAlbum(
                    volumeId = driveLink.volumeId,
                    albumId = linkId,
                    newName = name,
                )
            } else {
                renameLink(
                    linkId = linkId,
                    linkName = name,
                )
            }
        }
        renameBlock()
            .onFailure { error ->
                error.log(LogTag.RENAME, "Cannot rename link: ${linkId.id.logId()}")
                error.handle()
            }
            .onSuccess {
                _renameEffect.emit(RenameEffect.Dismiss)
                broadcastMessages(
                    userId = userId,
                    message = appContext.getString(
                        I18N.string.link_rename_successful,
                        name.ellipsizeMiddle(MAX_DISPLAY_FILENAME_LENGTH)
                    )
                )
            }
    }
}
