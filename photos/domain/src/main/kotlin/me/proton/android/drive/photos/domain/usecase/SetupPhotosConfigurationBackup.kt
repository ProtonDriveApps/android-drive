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

package me.proton.android.drive.photos.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import me.proton.android.drive.photos.domain.provider.PhotosDefaultConfigurationProvider
import me.proton.core.drive.backup.domain.usecase.GetConfiguration
import me.proton.core.drive.backup.domain.usecase.UpdateConfiguration
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class SetupPhotosConfigurationBackup @Inject constructor(
    private val getConfiguration: GetConfiguration,
    private val updateConfiguration: UpdateConfiguration,
    private val photosDefaultConfigurationProvider: PhotosDefaultConfigurationProvider,
) {

    suspend operator fun invoke(folderId: FolderId) = coRunCatching {
        val configuration = getConfiguration(folderId).firstOrNull()
        if (configuration == null) {
            updateConfiguration(photosDefaultConfigurationProvider.get(folderId)).getOrThrow()
        }
    }
}
