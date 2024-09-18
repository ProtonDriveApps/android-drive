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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.effectiveMaxDriveSpace
import me.proton.core.drive.user.domain.entity.QuotaLevel
import me.proton.core.drive.user.domain.repository.QuotaRepository
import me.proton.core.user.domain.UserManager
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class HasCanceledQuotaMessages @Inject constructor(
    private val userManager: UserManager,
    private val repository: QuotaRepository,
) {
    operator fun invoke(userId: UserId, level: QuotaLevel) =
        userManager.observeUser(userId).filterNotNull().flatMapLatest { user ->
            repository.exists(userId, level, user.effectiveMaxDriveSpace)
        }
}
