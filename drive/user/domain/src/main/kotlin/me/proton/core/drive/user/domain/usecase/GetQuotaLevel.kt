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

package me.proton.core.drive.user.domain.usecase

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.availableSpace
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.user.domain.entity.QuotaLevel
import me.proton.core.user.domain.UserManager
import javax.inject.Inject

class GetQuotaLevel @Inject constructor(
    private val userManager: UserManager,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(userId: UserId) =
        userManager.observeUser(userId).filterNotNull().map { user ->
            val available = user.availableSpace
            val percentage =
                Percentage(user.usedSpace.toFloat() / user.maxSpace)
            when {
                available < configurationProvider.backupLeftSpace -> QuotaLevel.ERROR
                percentage >= QuotaLevel.WARNING.percentage -> QuotaLevel.WARNING
                percentage >= QuotaLevel.INFO.percentage -> QuotaLevel.INFO
                else -> QuotaLevel.NULL
            }
        }
}
