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

package me.proton.android.drive.photos.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class IsPhotosEnabled @Inject constructor(
    private val getPhotoShare: GetPhotoShare,
    private val backupManager: BackupManager,
) {
    operator fun invoke(userId: UserId): Flow<Boolean> =
        getPhotoShare(userId)
            .filterSuccessOrError()
            .mapSuccessValueOrNull()
            .flatMapLatest { share ->
                if (share != null) {
                    backupManager.isEnabled(share.rootFolderId)
                } else {
                    flowOf(false)
                }
            }
}
