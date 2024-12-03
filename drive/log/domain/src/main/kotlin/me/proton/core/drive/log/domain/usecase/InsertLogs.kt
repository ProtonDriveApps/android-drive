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
 
package me.proton.core.drive.log.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidUserLogDisabled
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.repository.LogRepository
import javax.inject.Inject

class InsertLogs @Inject constructor(
    private val repository: LogRepository,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(logs: List<Log>) = coRunCatching {
        logs.firstOrNull()?.let { log ->
            onUserLogKillSwitchOff(log.userId) {
                repository.insert(logs)
            }
        }
    }

    private suspend fun onUserLogKillSwitchOff(userId: UserId, block: suspend () -> Unit) =
        takeIf { getFeatureFlag(driveAndroidUserLogDisabled(userId), { false }).off }
            ?.let { block() }
}

