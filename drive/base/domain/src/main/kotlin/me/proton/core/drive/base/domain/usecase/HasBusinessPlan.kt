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

package me.proton.core.drive.base.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.hasBusinessPlan
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.usecase.FetchOrganization.Companion.ORGANIZATION_URL
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.user.domain.extension.hasSubscription
import me.proton.core.user.domain.repository.UserRepository
import me.proton.core.usersettings.domain.entity.Organization
import me.proton.core.usersettings.domain.repository.OrganizationRepository
import javax.inject.Inject

class HasBusinessPlan @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val baseRepository: BaseRepository,
    private val fetchOrganization: FetchOrganization,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(userId: UserId): Result<Boolean> = coRunCatching {
        if (userRepository.getUser(userId).hasSubscription().not()) {
            // Free user
            return@coRunCatching false
        }
        getOrganization(userId)?.hasBusinessPlan ?: false
    }

    private suspend fun getOrganization(userId: UserId): Organization? {
        val cachedOrganization = coRunCatching {
            organizationRepository.getOrganization(userId, false)
        }.getOrNull()
        return if (cachedOrganization != null) {
            if (shouldRefreshCache(userId)) {
                fetchOrganization(userId).getOrThrow()
            } else {
                cachedOrganization
            }
        } else {
            fetchOrganization(userId).getOrThrow()
        }
    }

    private suspend fun shouldRefreshCache(userId: UserId): Boolean =
        baseRepository
            .getLastFetch(userId, ORGANIZATION_URL)
            .isOlderThen(configurationProvider.minimumOrganizationFetchInterval)
}
