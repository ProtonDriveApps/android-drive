/*
 * Copyright (c) 2025 Proton AG.
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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.photo.domain.extension.isPending
import me.proton.core.drive.drivelink.photo.domain.manager.PhotoShareMigrationManager
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import javax.inject.Inject

class ShowImportantUpdates @Inject constructor(
    configurationProvider: ConfigurationProvider,
    private val photoShareMigrationManager: PhotoShareMigrationManager,
    private val repository: PhotoShareMigrationRepository,
) {
    private val remindInterval = configurationProvider.minimumPhotosImportantUpdatesInterval

    operator fun invoke(userId: UserId): Flow<Boolean> = combine(
        photoShareMigrationManager.status,
        repository.getPhotosImportantUpdatesLastShownFlow(userId),
    ) { migrationStatus, lastShown ->
        migrationStatus.isPending && lastShown.isOlderThen(remindInterval)
    }
}
