/*
 * Copyright (c) 2021-2023 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.EventManagerProvider
import javax.inject.Inject
import kotlin.time.Duration

class UpdateEventActionImpl @Inject constructor(
    private val eventManagerProvider: EventManagerProvider,
    private val getShare: GetShare,
    private val getMinimumFetchInterval: GetMinimumFetchInterval,
) : UpdateEventAction {

    override suspend fun <T> invoke(shareId: ShareId, block: suspend () -> T): T =
        getShare(shareId).toResult().getOrThrow().volumeId.let { volumeId ->
            eventManagerProvider.get(
                EventManagerConfig.Drive.Volume(
                    userId = shareId.userId,
                    volumeId = volumeId.id,
                    minimumFetchInterval = getMinimumFetchInterval(
                        userId = shareId.userId,
                        volumeId = volumeId,
                    ).getOrNull() ?: Duration.ZERO,
                )
            ).suspend(block)
        }

    override suspend fun <T> invoke(userId: UserId, volumeId: VolumeId, block: suspend () -> T): T =
        eventManagerProvider.get(
            EventManagerConfig.Drive.Volume(
                userId = userId,
                volumeId = volumeId.id,
                minimumFetchInterval = getMinimumFetchInterval(
                    userId = userId,
                    volumeId = volumeId,
                ).getOrNull() ?: Duration.ZERO,
            )
        ).suspend(block)
}
