/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.share.user.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.repository.SharedRepository
import javax.inject.Inject

class GetAllSharedByMeIds @Inject constructor(
    private val repository: SharedRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(userId: UserId): Result<List<SharedLinkId>> = coRunCatching {
        pagedList(configurationProvider.dbPageSize) { index, count ->
            repository.getSharedByMeListing(
                userId = userId,
                index = index,
                count = count,
            )
        }
    }
}
