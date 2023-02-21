/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.base.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetCacheTempFolder @Inject constructor(
    private val storageLocationProvider: StorageLocationProvider,
) {
    suspend operator fun invoke(
        userId: UserId,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO
    ) =
        withContext(coroutineContext) {
            storageLocationProvider.getCacheTempFolder(userId)
        }
}
