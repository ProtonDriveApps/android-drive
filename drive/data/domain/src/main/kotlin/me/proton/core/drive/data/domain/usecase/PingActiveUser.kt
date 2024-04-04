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

package me.proton.core.drive.data.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.data.domain.repository.DataRepository
import javax.inject.Inject

class PingActiveUser @Inject constructor(
    private val dataRepository: DataRepository,
    private val baseRepository: BaseRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(userId: UserId) = coRunCatching {
        val pingActiveUserUrl = dataRepository.pingActiveUserUrl
        val lastFetch = baseRepository.getLastFetch(userId, pingActiveUserUrl)
        if (lastFetch.isOlderThen(configurationProvider.activeUserPingDuration)) {
            dataRepository.pingActiveUser(userId)
            baseRepository.setLastFetch(userId, pingActiveUserUrl, TimestampMs())
        }
    }
}
