/*
 * Copyright (c) 2023-2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.observability

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Upload
import me.proton.core.drive.announce.event.domain.entity.Event.Upload.UploadState.UPLOADING
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.base.domain.log.LogTag.TRACKING
import me.proton.core.drive.linkupload.domain.manager.UploadSpeedManager
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class UploadEventHandler @Inject constructor(
    private val uploadSpeedManager: UploadSpeedManager,
    private val enqueueUploadSpeedHistogramEvent: EnqueueUploadSpeedHistogramEvent,
) : EventHandler {

    private val mutex = Mutex()
    override suspend fun onEvent(
        userId: UserId,
        event: Event,
    ) {
        mutex.withLock {
            when (event) {
                is Upload -> {
                    if (event.state == UPLOADING) {
                        if (!uploadSpeedManager.isRunning()) {
                            CoreLogger.v(TRACKING, "Resuming, uploading")
                            uploadSpeedManager.resume()
                        }
                    }
                    Unit
                }

                is Event.UploadSpeed -> enqueueUploadSpeedHistogramEvent(event)
                else -> Unit
            }
        }
    }
}
