/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.ui.screen.map

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal enum class PhotoMapDownloadKind { THUMBNAIL, LOCATION }

@Singleton
internal class PhotoMapDownloadLimiter(
    private val clock: TimeSource,
) {
    @Inject constructor() : this(TimeSource.Monotonic)

    private val lock = Mutex()
    private val changed = MutableStateFlow(0L)
    private val queue = ArrayDeque<Ticket>()
    private val active = IntArray(PhotoMapDownloadKind.entries.size)
    private var nextStart: TimeMark? = null

    suspend fun <T> download(kind: PhotoMapDownloadKind, block: suspend () -> T): T {
        val ticket = Ticket(kind)
        try {
            lock.withLock {
                queue.addLast(ticket)
                notifyWaiters()
            }
            awaitStart(ticket)
            currentCoroutineContext().ensureActive()
            return block()
        } finally {
            withContext(NonCancellable) {
                lock.withLock {
                    if (ticket.started) active[kind.ordinal]-- else queue.remove(ticket)
                    notifyWaiters()
                }
            }
        }
    }

    private suspend fun awaitStart(ticket: Ticket) {
        while (true) {
            val (version, delay) = lock.withLock {
                val wait = if (nextTicket() === ticket) {
                    nextStart?.let { (-it.elapsedNow()).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO
                } else {
                    null
                }
                if (wait == Duration.ZERO) {
                    queue.remove(ticket)
                    ticket.started = true
                    active[ticket.kind.ordinal]++
                    nextStart = clock.markNow() + START_INTERVAL
                    notifyWaiters()
                    return
                }
                changed.value to wait
            }
            if (delay == null) {
                changed.first { it != version }
            } else {
                withTimeoutOrNull(delay) { changed.first { it != version } }
            }
        }
    }

    private fun nextTicket(): Ticket? {
        if (active.sum() >= MAX_ACTIVE) return null
        val first = queue.firstOrNull() ?: return null
        val other = queue.firstOrNull { it.kind != first.kind }
        // A queue can borrow spare capacity until both kinds have work waiting.
        return if (other == null || active[first.kind.ordinal] < RESERVED_PER_KIND) first else other
    }

    private fun notifyWaiters() {
        changed.value++
    }

    private class Ticket(val kind: PhotoMapDownloadKind) {
        var started = false
    }

    private companion object {
        const val MAX_ACTIVE = 4
        const val RESERVED_PER_KIND = 2
        val START_INTERVAL = 1.seconds
    }
}
