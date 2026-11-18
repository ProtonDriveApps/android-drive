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

package me.proton.android.drive.ui.screen.map

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailDownloadThrottleTest {
    @Test
    fun `matching requests coalesce into bounded homogeneous batches`() = runTest {
        val batches = mutableListOf<Pair<String, List<Int>>>()
        val queue = batcher(
            cached = { null },
        ) { group, items ->
            flow {
                batches += group to items
                items.forEach { emit(it to Result.success(it)) }
            }
        }
        val photos = (1..17).map { item -> async { queue.load("preview", item) } }
        val otherType = async { queue.load("small", 1) }
        val duplicate = async { queue.load("preview", 1) }

        advanceTimeBy(50)
        runCurrent()

        assertEquals((1..17).toList(), photos.map { it.await() })
        assertEquals(1, otherType.await())
        assertEquals(1, duplicate.await())
        assertEquals(
            listOf("preview" to (1..16).toList(), "preview" to listOf(17), "small" to listOf(1)),
            batches,
        )
        assertEquals(ThumbnailDownloadProgress(downloaded = 18), queue.progress.value)
    }

    @Test
    fun `progress distinguishes queued and active requests and updates for each result`() = runTest {
        val startBatch = CompletableDeferred<Unit>()
        val finishBatch = CompletableDeferred<Unit>()
        val queue = batcher(
            cached = { null },
            runTransfer = { transfer ->
                startBatch.await()
                transfer()
            },
        ) { _, items ->
            flow {
                emit(items[0] to Result.success(100))
                finishBatch.await()
                emit(items[1] to Result.success(200))
            }
        }
        val first = async { queue.load("preview", 1) }
        val second = async { queue.load("preview", 2) }
        advanceTimeBy(50)
        runCurrent()
        assertEquals(ThumbnailDownloadProgress(queued = 2), queue.progress.value)

        startBatch.complete(Unit)
        assertEquals(100, first.await())
        assertEquals(ThumbnailDownloadProgress(active = 1, downloaded = 1), queue.progress.value)

        finishBatch.complete(Unit)
        assertEquals(200, second.await())
        assertEquals(ThumbnailDownloadProgress(downloaded = 2), queue.progress.value)
    }

    @Test
    fun `cancellation while waiting for transport drops the individual photo`() = runTest {
        val allowTransfer = CompletableDeferred<Unit>()
        val sent = mutableListOf<Int>()
        val queue = batcher(
            cached = { null },
            runTransfer = { transfer ->
                allowTransfer.await()
                transfer()
            },
        ) { _, items ->
            flow {
                sent += items
                items.forEach { emit(it to Result.success(it)) }
            }
        }
        val cancelled = async { queue.load("preview", 1) }
        val remaining = async { queue.load("preview", 2) }
        advanceTimeBy(50)
        runCurrent()

        cancelled.cancelAndJoin()
        assertEquals(ThumbnailDownloadProgress(queued = 1), queue.progress.value)
        allowTransfer.complete(Unit)
        assertEquals(2, remaining.await())
        assertEquals(listOf(2), sent)
    }

    @Test
    fun `an active batch is cancelled only after all waiters leave`() = runTest {
        var stopped = false
        val queue = batcher(
            cached = { null },
        ) { _, _ ->
            flow {
                try {
                    awaitCancellation()
                } finally {
                    stopped = true
                }
            }
        }
        val first = async { queue.load("preview", 1) }
        val duplicate = async { queue.load("preview", 1) }
        val second = async { queue.load("preview", 2) }
        advanceTimeBy(50)
        runCurrent()
        assertEquals(ThumbnailDownloadProgress(active = 2), queue.progress.value)

        first.cancelAndJoin()
        assertEquals(ThumbnailDownloadProgress(active = 2), queue.progress.value)
        duplicate.cancelAndJoin()
        assertEquals(ThumbnailDownloadProgress(active = 1), queue.progress.value)
        assertEquals(false, stopped)

        second.cancelAndJoin()
        runCurrent()
        assertTrue(stopped)
        assertEquals(ThumbnailDownloadProgress(), queue.progress.value)
    }

    @Test
    fun `session cancellation stops queued and running work`() = runTest {
        var submitted = 0
        var stopped = 0
        val queue = batcher(
            cached = { null },
            runTransfer = { transfer ->
                if (submitted++ == 0) transfer() else awaitCancellation()
            },
        ) { _, _ ->
            flow {
                try {
                    awaitCancellation()
                } finally {
                    stopped++
                }
            }
        }
        val first = async { queue.load("preview", 1) }
        val second = async { queue.load("small", 1) }
        advanceTimeBy(50)
        runCurrent()
        assertEquals(ThumbnailDownloadProgress(queued = 1, active = 1), queue.progress.value)

        queue.cancel()
        assertEquals(ThumbnailDownloadProgress(), queue.progress.value)
        first.join()
        second.join()
        assertTrue(first.isCancelled)
        assertTrue(second.isCancelled)
        assertEquals(1, stopped)
    }

    @Test
    fun `a newly cached thumbnail avoids network when the limiter admits it`() = runTest {
        val allowTransfer = CompletableDeferred<Unit>()
        var cached: Int? = null
        var downloads = 0
        val queue = batcher(
            cached = { cached },
            runTransfer = { transfer ->
                allowTransfer.await()
                transfer()
            },
        ) { _, _ ->
            downloads++
            emptyFlow()
        }
        val photo = async { queue.load("preview", 1) }
        advanceTimeBy(50)
        runCurrent()

        cached = 100
        allowTransfer.complete(Unit)
        assertEquals(100, photo.await())
        assertEquals(0, downloads)
        assertEquals(ThumbnailDownloadProgress(), queue.progress.value)
    }

    @Test
    fun `native failures and missing frames complete all callers`() = runTest {
        val queue = batcher(
            cached = { null },
        ) { group, _ ->
            flow { if (group == "preview") throw IOException("Transport failed") }
        }
        val thrown = async { runCatching { queue.load("preview", 1) } }
        val missing = async { runCatching { queue.load("small", 1) } }

        assertEquals("Transport failed", thrown.await().exceptionOrNull()?.message)
        assertEquals("Thumbnail is unavailable", missing.await().exceptionOrNull()?.message)
        assertEquals(ThumbnailDownloadProgress(), queue.progress.value)
    }

    @Test
    fun `client preparation times out before taking a transfer slot`() = runTest {
        var acquired = false
        val queue = ThumbnailDownloadThrottle<String, Int, Int>(
            parentScope = backgroundScope,
            cached = { null },
            runTransfer = { acquired = true },
            prepareDownload = { awaitCancellation() },
        )
        val photo = async { runCatching { queue.load("preview", 1) } }

        assertTrue(photo.await().exceptionOrNull() is IOException)
        assertEquals(30_050, testScheduler.currentTime)
        assertEquals(false, acquired)
        assertEquals(ThumbnailDownloadProgress(), queue.progress.value)
    }

    private fun TestScope.batcher(
        cached: (Int) -> Int?,
        runTransfer: suspend (suspend () -> Unit) -> Unit = { it() },
        download: (String, List<Int>) -> Flow<Pair<Int, Result<Int>>>,
    ) = ThumbnailDownloadThrottle<String, Int, Int>(
        parentScope = backgroundScope,
        cached = cached,
        runTransfer = runTransfer,
        prepareDownload = { group -> { items -> download(group, items) } },
    )
}
