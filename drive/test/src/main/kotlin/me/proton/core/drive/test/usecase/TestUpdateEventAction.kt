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

package me.proton.core.drive.test.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestUpdateEventAction @Inject constructor() : UpdateEventAction {

    private val lock = Mutex()
    override suspend fun <T> invoke(
        shareId: ShareId,
        overrideMinimumFetchInterval: Boolean,
        block: suspend () -> T,
    ): T = lock.withLock { block() }

    override suspend fun <T> invoke(
        userId: UserId,
        volumeId: VolumeId,
        overrideMinimumFetchInterval: Boolean,
        block: suspend () -> T,
    ): T = lock.withLock { block() }
}
