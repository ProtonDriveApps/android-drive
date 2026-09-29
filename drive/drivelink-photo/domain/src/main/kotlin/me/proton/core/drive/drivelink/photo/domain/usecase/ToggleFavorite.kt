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
package me.proton.core.drive.drivelink.photo.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKUpdatePhotos
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.drive.sdk.entity.NodeResultPair
import javax.inject.Inject

class ToggleFavorite @Inject constructor(
    private val toggleFavoriteLegacy: ToggleFavoriteLegacy,
    private val toggleFavoriteSdk: ToggleFavoriteSdk,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(
        driveLink: DriveLink.File,
    ) = coRunCatching {
        if (getFeatureFlag(driveAndroidSDKUpdatePhotos(driveLink.userId)).on) {
            val nodeUid = driveLink.id.nodeUid(driveLink.volumeId)
            val resultPair = toggleFavoriteSdk(driveLink)
                .getOrThrow().first { pair -> pair.nodeUid == nodeUid }
            if (resultPair is NodeResultPair.Failure) {
                throw resultPair.error
            }
        } else {
            toggleFavoriteLegacy(driveLink).getOrThrow()
        }
    }
}
