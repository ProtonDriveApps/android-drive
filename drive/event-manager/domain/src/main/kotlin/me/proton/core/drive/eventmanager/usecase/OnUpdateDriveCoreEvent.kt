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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.eventmanager.entity.DriveCoreEvent
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.eventmanager.domain.entity.Action
import javax.inject.Inject

class OnUpdateDriveCoreEvent @Inject constructor(
    private val getShares: GetShares,
) {
    suspend operator fun invoke(userId: UserId, entries: List<DriveCoreEvent>) {
        if (entries.any { it.driveShareRefreshAction == Action.Update }) {
            getShares(userId, Share.Type.STANDARD, flowOf(true))
                .toResult()
                .getOrNull(
                    tag = LogTag.EVENTS,
                    message = "OnUpdateDriveCoreEvent failed to get standard shares",
                )
        }
    }
}
