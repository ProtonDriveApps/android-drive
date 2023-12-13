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

package me.proton.android.drive.photos.domain.handler

import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.usecase.StopBackup
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_PHOTOS_UPLOAD_DISABLED
import me.proton.core.drive.feature.flag.domain.extension.onEnabled
import me.proton.core.drive.feature.flag.domain.handler.FeatureFlagHandler
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class DrivePhotosUploadDisabledFeatureFlagHandler @Inject constructor(
    private val stopBackup: StopBackup,
) : FeatureFlagHandler {

    override suspend fun onFeatureFlag(featureFlag: FeatureFlag) {
        require(featureFlag.id.id == DRIVE_PHOTOS_UPLOAD_DISABLED) { "Wrong feature flag id ${featureFlag.id.id}" }
        featureFlag
            .onEnabled {
                stopBackup(featureFlag.id.userId, BackupError.PhotosUploadNotAllowed())
                    .onFailure { error ->
                        CoreLogger.d(
                            tag = LogTag.BACKUP,
                            e = error,
                            message = "Stop backup due to drive photos upload disabled feature flag failed"
                        )
                    }
            }
    }
}
