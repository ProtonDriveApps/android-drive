/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.utils.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object NetworkInterceptor {
    fun setResponseDelay(delay: Duration) = apply {
        NetworkSimulator.responseDelay = delay
    }

    fun simulateUnknownHost() = apply {
        NetworkSimulator.isNetworkDisabled = true
    }

    fun simulateNetworkTimeout() = apply {
        NetworkSimulator.isNetworkTimeout = true
    }

    fun reset() = apply {
        NetworkSimulator.isNetworkTimeout = false
        NetworkSimulator.isNetworkDisabled = false
        NetworkSimulator.responseDelay = 0.milliseconds
    }
}