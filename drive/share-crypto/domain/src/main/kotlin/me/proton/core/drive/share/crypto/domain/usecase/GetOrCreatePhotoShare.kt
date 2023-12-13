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

package me.proton.core.drive.share.crypto.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.exception.ShareException
import javax.inject.Inject

class GetOrCreatePhotoShare @Inject constructor(
    private val getPhotoShare: GetPhotoShare,
    private val createPhotoShare: CreatePhotoShare,
) {
    operator fun invoke(userId: UserId): Flow<DataResult<Share>> =
        getPhotoShare(userId).transform { result ->
            when (result) {
                is DataResult.Error -> {
                    when (result.cause) {
                        is ShareException -> {
                            if (result.cause is ShareException.ShareLocked) {
                                emit(result)
                            }
                            emitAll(createPhotoShare(userId))
                        }

                        else -> emit(result)
                    }
                }

                else -> emit(result)
            }
        }
}
