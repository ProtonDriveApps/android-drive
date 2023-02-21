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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.UpdateIsAnyAncestorMarkedAsOffline
import me.proton.core.drive.drivelink.shared.domain.repository.DriveLinkSharedRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.domain.usecase.GetShareUrls
import me.proton.core.drive.shareurl.base.domain.usecase.HasShareUrls
import javax.inject.Inject

class GetSharedDriveLinks @Inject constructor(
    private val getShareUrls: GetShareUrls,
    private val hasShareUrls: HasShareUrls,
    private val repository: DriveLinkSharedRepository,
    private val updateIsAnyAncestorMarkedAsOffline: UpdateIsAnyAncestorMarkedAsOffline,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        shareId: ShareId,
        refresh: Flow<Boolean> = flowOf { !hasShareUrls(shareId) },
    ): Flow<DataResult<List<DriveLink>>> =
        getShareUrls(
            shareId = shareId,
            refresh = refresh,
        ).transformSuccess {
            emitAll(
                repository.getSharedDriveLinks(shareId)
                    .map { driveLinks ->
                        updateIsAnyAncestorMarkedAsOffline(driveLinks).asSuccess
                    }
            )
        }
}
