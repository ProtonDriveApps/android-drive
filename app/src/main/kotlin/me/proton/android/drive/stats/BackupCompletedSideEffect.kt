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

package me.proton.android.drive.stats

import me.proton.android.drive.usecase.GetShareAsPhotoShare
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.stats.domain.usecase.DeleteUploadStats
import me.proton.core.drive.stats.domain.usecase.SetOrIgnoreInitialBackup
import javax.inject.Inject

class BackupCompletedSideEffect @Inject constructor(
    private val getShareAsPhotoShare: GetShareAsPhotoShare,
    private val deleteUploadStats: DeleteUploadStats,
    private val setOrIgnoreInitialBackup: SetOrIgnoreInitialBackup,
) {

    suspend operator fun invoke(event: Event.BackupCompleted) {
        getShareAsPhotoShare(event.folderId.shareId)?.let {
            setOrIgnoreInitialBackup(event.folderId).getOrThrow()
            deleteUploadStats(event.folderId).getOrThrow()
        }
    }
}
