/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.drivelink.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetDecryptedSharedDriveLinks @Inject constructor(
    private val getSharedDriveLinks: GetSharedDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
    private val getSorting: GetSorting,
    private val sortDriveLinks: SortDriveLinks,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userId: UserId, volumeId: VolumeId, refresh: Boolean): Flow<DataResult<List<DriveLink>>> {
        var shouldRefresh = refresh
        return getSorting(userId).flatMapLatest { sorting ->
            val willRefresh = shouldRefresh
            shouldRefresh = false
            getSharedDriveLinks(userId, volumeId, flowOf(willRefresh)).mapSuccess { (source, driveLinks) ->
                DataResult.Success(source, sortDriveLinks(sorting, decryptDriveLinks(driveLinks)))
            }
        }
    }
}
