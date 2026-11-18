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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.ui.screen.map.PhotoMapDownloadKind.LOCATION
import me.proton.android.drive.ui.screen.map.PhotoMapDownloadKind.THUMBNAIL
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoMapDownloadLimiterTest {
    @Test
    fun `mixed requests share four slots and one start per second`() = runTest {
        val limiter = PhotoMapDownloadLimiter(testScheduler.timeSource)
        val finish = CompletableDeferred<Unit>()
        val starts = mutableListOf<Pair<PhotoMapDownloadKind, Long>>()
        (List(3) { THUMBNAIL } + List(3) { LOCATION }).forEach { kind ->
            launch {
                limiter.download(kind) {
                    starts += kind to testScheduler.currentTime
                    finish.await()
                }
            }
        }

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(
            listOf(THUMBNAIL to 0L, THUMBNAIL to 1_000L, LOCATION to 2_000L, LOCATION to 3_000L),
            starts,
        )
        finish.complete(Unit)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(THUMBNAIL to 5_000L, LOCATION to 6_000L), starts.drop(4))
    }

    @Test
    fun `idle slots can be borrowed and a waiting kind claims the next released slot`() = runTest {
        val limiter = PhotoMapDownloadLimiter(testScheduler.timeSource)
        val starts = mutableListOf<PhotoMapDownloadKind>()
        val borrowing = List(5) {
            launch {
                limiter.download(THUMBNAIL) {
                    starts += THUMBNAIL
                    awaitCancellation()
                }
            }
        }
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(List(4) { THUMBNAIL }, starts)

        val locations = launch { limiter.download(LOCATION) { starts += LOCATION } }
        runCurrent()
        borrowing.first().cancelAndJoin()
        runCurrent()
        assertEquals(List(4) { THUMBNAIL } + LOCATION, starts)

        locations.join()
        borrowing.forEach { it.cancelAndJoin() }
    }

    @Test
    fun `cancellation removes a queued start without consuming the next turn`() = runTest {
        val limiter = PhotoMapDownloadLimiter(testScheduler.timeSource)
        val starts = mutableListOf<PhotoMapDownloadKind>()
        limiter.download(THUMBNAIL) { starts += THUMBNAIL }
        val cancelled = launch { limiter.download(THUMBNAIL) { starts += THUMBNAIL } }
        launch { limiter.download(LOCATION) { starts += LOCATION } }
        runCurrent()
        cancelled.cancelAndJoin()

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(THUMBNAIL, LOCATION), starts)
    }
}
