/*
 * Copyright (c) 2024 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.usecase.ToggleNetworkConfiguration
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class TogglePhotosNetworkConfiguration @Inject constructor(
    private val toggleNetworkConfiguration: ToggleNetworkConfiguration,
    private val getPhotosDriveLink: GetPhotosDriveLink,
) {

    suspend operator fun invoke(userId: UserId) = coRunCatching {
        val photoRootId = getPhotosDriveLink(userId)
            .firstSuccessOrError().toResult().getOrThrow().id
        toggleNetworkConfiguration(photoRootId).getOrThrow()
    }
}
