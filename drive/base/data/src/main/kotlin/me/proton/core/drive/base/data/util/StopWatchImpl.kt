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

package me.proton.core.drive.base.data.util

import android.os.SystemClock
import me.proton.core.drive.base.domain.util.StopWatch

class StopWatchImpl : StopWatch {
    private var isRunning = false
    private var startTime = 0L
    private var accumulatedTime = 0L

    override fun start() {
        if (!isRunning) {
            startTime = SystemClock.elapsedRealtime()
            isRunning = true
        }
    }

    override fun stop() {
        if (isRunning) {
            accumulatedTime += SystemClock.elapsedRealtime() - startTime
            isRunning = false
        }
    }

    override fun reset() {
        isRunning = false
        startTime = 0L
        accumulatedTime = 0L
    }

    override fun getElapsedTimeInMs(): Long = if (isRunning) {
        accumulatedTime + (SystemClock.elapsedRealtime() - startTime)
    } else {
        accumulatedTime
    }

    override fun isRunning(): Boolean = isRunning
}
