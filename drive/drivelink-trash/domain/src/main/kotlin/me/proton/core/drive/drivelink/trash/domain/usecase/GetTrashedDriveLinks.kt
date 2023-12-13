/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.drivelink.trash.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.UpdateIsAnyAncestorMarkedAsOffline
import me.proton.core.drive.drivelink.trash.domain.repository.DriveLinkTrashRepository
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetTrashedDriveLinks @Inject constructor(
    private val driveLinkTrashRepository: DriveLinkTrashRepository,
    private val updateIsAnyAncestorMarkedAsOffline: UpdateIsAnyAncestorMarkedAsOffline,
    private val driveTrashRepository: DriveTrashRepository,
    private val linkTrashRepository: LinkTrashRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        refresh: Flow<Boolean> = flowOf { linkTrashRepository.shouldInitiallyFetchTrashContent(userId, volumeId) }
    ): Flow<DataResult<List<DriveLink>>> =
        refresh.transform { shouldRefresh ->
            if (shouldRefresh) {
                fetcher<List<DriveLink>> {
                    val (_, saveAction) = driveTrashRepository.fetchTrashContent(
                        userId = userId,
                        volumeId = volumeId,
                        pageIndex = 0,
                        pageSize = configurationProvider.uiPageSize,
                    ).getOrThrow()
                    saveAction()
                }
            }
            emitAll(
                driveLinkTrashRepository
                    .getTrashDriveLinks(userId, volumeId, fromIndex, count)
                    .map { driveLinks -> updateIsAnyAncestorMarkedAsOffline(driveLinks).asSuccess }
            )
        }
}
