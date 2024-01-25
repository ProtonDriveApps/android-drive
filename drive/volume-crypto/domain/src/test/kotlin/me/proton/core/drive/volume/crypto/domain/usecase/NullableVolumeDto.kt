/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.volume.crypto.domain.usecase

import me.proton.core.drive.db.test.shareId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.volume.data.api.entity.VolumeDto
import me.proton.core.drive.volume.data.api.entity.VolumeShare

@Suppress("TestFunctionName")
internal fun NullableVolumeDto(
    id: String = volumeId,
    volumeShare: VolumeShare = VolumeShare(shareId, "root-id"),
    state: Long = 1,
) = VolumeDto(
    id = id,
    creationTime = 0,
    maxSpace = 0,
    usedSpace = 0,
    state = state,
    share = volumeShare
)
