/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.feature.flag.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidDownloadManager
import me.proton.core.drive.feature.flag.domain.extension.on
import javax.inject.Inject

class IsDownloadManagerEnabled @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val getFeatureFlag: GetFeatureFlag,
) {

    suspend operator fun invoke(userId: UserId) =
        configurationProvider.preferPipelineBasedDownloadManager && isDownloadManagerEnabled(userId)

    private suspend fun isDownloadManagerEnabled(userId: UserId) =
        getFeatureFlag(driveAndroidDownloadManager(userId)).on
}
