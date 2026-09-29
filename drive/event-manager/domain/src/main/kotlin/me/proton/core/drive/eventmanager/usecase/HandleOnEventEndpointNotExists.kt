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

package me.proton.core.drive.eventmanager.usecase

import me.proton.android.drive.photos.domain.usecase.DisablePhotosBackup
import me.proton.core.domain.entity.UserId
import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.entity.isActive
import me.proton.core.drive.volume.domain.usecase.GetVolume
import me.proton.core.drive.volume.domain.usecase.RefreshVolume
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class HandleOnEventEndpointNotExists @Inject constructor(
    private val getVolume: GetVolume,
    private val refreshVolume: RefreshVolume,
    private val disablePhotosBackup: DisablePhotosBackup,
) {

    suspend operator fun invoke(userId: UserId, volumeId: VolumeId) = coRunCatching {
        CoreLogger.i(LogTag.EVENTS, "onEventEndpointNotExists: volume ${volumeId.id.logId()}")
        // A deleted volume is gone from the local database once refreshed.
        val staleVolume = getVolume(userId, volumeId, refresh = flowOf(false)).toResult().getOrNull()
        val volume = refreshVolume(userId, volumeId).getOrThrow()
        CoreLogger.i(
            LogTag.EVENTS,
            "onEventEndpointNotExists: volume type ${staleVolume?.type}" +
                " state ${staleVolume?.state} -> ${volume?.state}",
        )
        if (staleVolume?.type == Volume.Type.PHOTO && volume?.isActive != true) {
            disablePhotosBackup(
                folderId = FolderId(ShareId(userId, staleVolume.shareId), staleVolume.linkId),
                state = Event.Backup.BackupState.FAILED_FOLDER_NOT_FOUND,
            ).getOrThrow()
        }
    }
}
