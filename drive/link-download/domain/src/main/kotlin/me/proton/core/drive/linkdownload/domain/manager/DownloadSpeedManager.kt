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

package me.proton.core.drive.linkdownload.domain.manager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.restart
import me.proton.core.drive.base.domain.log.LogTag.TRACKING
import me.proton.core.drive.base.domain.util.StopWatch
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.minutes

class DownloadSpeedManager @Inject constructor(
    private val stopWatch: StopWatch,
    private val minuteWatch: StopWatch,
    private val asyncAnnounceEventProvider: Provider<AsyncAnnounceEvent>, // avoid dependency cycle
) {

    private val mutex = Mutex()
    private var bytesPerMinutes: Long = 0L

    init {
        minuteWatch.start()
    }

    fun resume() {
        stopWatch.start()
    }

    fun pause(userId: UserId) {
        if (stopWatch.isRunning()) {
            announceSpeed(userId, stopWatch.getElapsedTimeInMs())
        }
        stopWatch.stop()
    }

    fun isRunning() = stopWatch.isRunning()

    suspend fun add(userId: UserId, value: Long) {
        mutex.withLock {
            bytesPerMinutes += value
            stopWatch.start()
            checkTime(userId)
        }
    }

    private fun checkTime(userId: UserId) {
        val clockElapsedTime = minuteWatch.getElapsedTimeInMs()
        if (clockElapsedTime >= THRESHOLD) {
            CoreLogger.d(
                TRACKING,
                "checkTime(download) elapsed(sec) ${clockElapsedTime.toSeconds()}"
            )
            if (stopWatch.isRunning()) {
                val stopElapsedTime = stopWatch.getElapsedTimeInMs()
                if (stopElapsedTime < MAX_LIMIT) {
                    announceSpeed(userId, stopElapsedTime)
                } else {
                    CoreLogger.d(
                        TRACKING,
                        "checkTime(download) ignoring elapsed(sec): ${stopElapsedTime.toSeconds()} " +
                                "over ${MAX_LIMIT.toSeconds()}"
                    )
                }
                stopWatch.restart()
            }
            minuteWatch.restart()
            bytesPerMinutes = 0
        }
    }

    private fun Long.toSeconds() = this.toFloat() / 1000

    private fun announceSpeed(userId: UserId, elapsedTimeInMs: Long) {
        if (elapsedTimeInMs != 0L) {
            asyncAnnounceEventProvider.get().invoke(
                userId, Event.DownloadSpeed(
                    bytes = bytesPerMinutes.bytes,
                    elapsedTime = TimestampMs(elapsedTimeInMs)
                )
            )
        }
    }

    private companion object {
        val THRESHOLD = 1.minutes.inWholeMilliseconds
        val MAX_LIMIT = 3.minutes.inWholeMilliseconds
    }
}
