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

package me.proton.core.drive.drivelink.photo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.photo.domain.usecase.GetPhotoCount
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

class GetPhotoCount @Inject constructor(
    private val getPhotoCount: GetPhotoCount,
    private val getPhotoShare: GetPhotoShare,
) {

    operator fun invoke(
        userId: UserId,
    ): Flow<Int> = getPhotoShare(userId)
        .mapSuccessValueOrNull()
        .distinctUntilChanged()
        .transform { photoShare ->
            if (photoShare == null) {
                emit(0)
            } else {
                emitAll(getPhotoCount(userId, photoShare.volumeId))
            }
        }
}
