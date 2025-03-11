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

package me.proton.core.drive.base.data.usecase

import android.graphics.Bitmap
import android.os.Build
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import javax.inject.Inject
import me.proton.core.drive.base.data.extension.compress

class CompressBitmap @Inject constructor(
    private val getPrimaryUser: GetPrimaryUser,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        maxSize: Bytes,
    ): Result<ByteArray?> = coRunCatching {
        val userId = requireNotNull(getPrimaryUser()).userId
        val format = if (getFeatureFlag(FeatureFlagId.driveThumbnailWebP(userId)).on) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
        } else {
            Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(maxSize, format)
    }
}
