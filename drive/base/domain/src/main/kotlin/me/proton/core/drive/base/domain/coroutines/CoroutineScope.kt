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

package me.proton.core.drive.base.domain.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.util.kotlin.CoreLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun timeLimitedScope(
    tag: String,
    timeout: Duration = 5.minutes,
    scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Main),
    block: suspend CoroutineScope.() -> Unit
): Job = scope.launch {
    try {
        withTimeout(timeout, block)
    } catch (e: TimeoutCancellationException) {
        CoreLogger.e(tag, e)
    }
}
