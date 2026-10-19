/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.worker.data

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.selects.select

internal sealed interface BlockedOr<out C, out R> {
    data class Blocked<C>(val by: C) : BlockedOr<C, Nothing>
    data class Completed<R>(val result: R) : BlockedOr<Nothing, R>
}

internal suspend fun <C : Any, R> runUntilBlocked(
    block: Flow<C>,
    work: suspend () -> R,
): BlockedOr<C, R> = coroutineScope {
    val blocking = async(start = CoroutineStart.UNDISPATCHED) { block.firstOrNull() }
    if (blocking.isCompleted) {
        blocking.await()?.let { blockedBy -> return@coroutineScope BlockedOr.Blocked(blockedBy) }
    }
    val working = async { work() }
    select {
        working.onAwait { result ->
            blocking.cancel()
            BlockedOr.Completed(result)
        }
        blocking.onAwait { blockedBy ->
            if (blockedBy == null) {
                BlockedOr.Completed(working.await())
            } else {
                working.cancel()
                BlockedOr.Blocked(blockedBy)
            }
        }
    }
}
