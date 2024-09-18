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
import me.proton.core.drive.base.domain.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.usersettings.domain.entity.Organization
import me.proton.core.usersettings.domain.repository.OrganizationRepository
import javax.inject.Inject

class FetchOrganization @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val baseRepository: BaseRepository,
) {

    suspend operator fun invoke(userId: UserId): Result<Organization?> = coRunCatching {
        organizationRepository.getOrganization(userId, true).also {
            baseRepository.setLastFetch(userId, ORGANIZATION_URL, TimestampMs())
        }
    }.recoverCatching { error ->
        if (error.hasProtonErrorCode(NOT_EXISTS)) {
            null
        } else {
            throw error
        }
    }

    companion object {
        const val ORGANIZATION_URL = "/core/v4/organizations"
    }
}
