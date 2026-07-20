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

package me.proton.core.drive.drivelink.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.volume.domain.entity.VolumeId

interface DriveLinkRepository {

    fun getDriveLink(linkId: LinkId): Flow<DriveLink?>

    fun getDriveLink(userId: UserId, volumeId: VolumeId, linkId: String, excludedShareTypes: Set<Share.Type>): Flow<List<DriveLink>>

    fun getDriveLinks(parentId: ParentId, fromIndex: Int, count: Int): Flow<List<DriveLink>>

    fun getDriveLinksCount(parentId: ParentId): Flow<Int>

    fun getDriveLinks(linkIds: List<LinkId>): Flow<List<DriveLink>>
}
