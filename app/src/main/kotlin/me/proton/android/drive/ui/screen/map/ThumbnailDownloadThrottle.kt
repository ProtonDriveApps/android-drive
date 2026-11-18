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
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class ThumbnailDownloadProgress(
    val queued: Int = 0,
    val active: Int = 0,
    val downloaded: Int = 0,
) {
    val loading: Boolean get() = queued + active > 0
}

internal class ThumbnailDownloadThrottle<K : Any, I : Any, V : Any>(
    parentScope: CoroutineScope,
    private val cached: (I) -> V?,
    private val runTransfer: suspend (suspend () -> Unit) -> Unit,
    private val prepareDownload: suspend (K) -> (List<I>) -> Flow<Pair<I, Result<V>>>,
) {
    private data class Key<K, I>(val group: K, val item: I)

    private class Request<K, I, V>(val key: Key<K, I>, val result: CompletableDeferred<V>) {
        var waiters = 0
        var batch: Batch<K, I, V>? = null
    }

    private class Batch<K, I, V>(val requests: List<Request<K, I, V>>) {
        lateinit var job: Job
        var started = false
    }

    private val lock = Any()
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val requests = linkedMapOf<Key<K, I>, Request<K, I, V>>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var scheduled = 0
    private var downloaded = 0
    private val mutableProgress = MutableStateFlow(ThumbnailDownloadProgress())
    val progress = mutableProgress.asStateFlow()

    init {
        scope.launch {
            for (ignored in wake) {
                delay(50.milliseconds)
                while (true) {
                    val batch = synchronized(lock) { nextBatch() } ?: break
                    batch.job.start()
                }
            }
        }
    }

    suspend fun load(group: K, item: I): V {
        coroutineContext.ensureActive()
        val request = synchronized(lock) {
            job.ensureActive()
            val key = Key(group, item)
            val pending = requests.getOrPut(key) { Request(key, CompletableDeferred(job)) }
            pending.waiters++
            publishProgress()
            wake.trySend(Unit)
            pending
        }
        try {
            return request.result.await()
        } finally {
            synchronized(lock) { removeWaiter(request) }
        }
    }

    fun cancel() {
        job.cancel()
        synchronized(lock) {
            requests.clear()
            publishProgress()
        }
    }

    private fun nextBatch(): Batch<K, I, V>? {
        if (scheduled >= MAX_SCHEDULED_BATCHES || !job.isActive) return null
        val waiting = requests.values.asSequence().filter { it.batch == null }
        val group = waiting.firstOrNull()?.key?.group ?: return null
        val batch = Batch(waiting.filter { it.key.group == group }.take(MAX_BATCH_SIZE).toList())
        batch.requests.forEach { it.batch = batch }
        scheduled++
        batch.job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                execute(batch)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                completeFailures(batch.requests, error)
            }
        }
        batch.job.invokeOnCompletion { cause ->
            synchronized(lock) {
                val failure = cause ?: IOException("Thumbnail is unavailable")
                batch.requests.forEach { finish(it, Result.failure(failure)) }
                scheduled--
                publishProgress()
                wake.trySend(Unit)
            }
        }
        return batch
    }

    private suspend fun execute(batch: Batch<K, I, V>) {
        val ready = checkCache(batch.requests)
        if (ready.isEmpty()) return
        val download = withTimeoutOrNull(30.seconds) { prepareDownload(ready.first().key.group) }
            ?: throw IOException("Thumbnail download timed out preparing the client")
        runTransfer {
            val items = checkCache(ready)
            if (items.isEmpty()) return@runTransfer
            val active = synchronized(lock) {
                batch.started = true
                items.filter { requests[it.key] === it && it.waiters > 0 }.also {
                    publishProgress()
                }
            }
            if (active.isEmpty()) return@runTransfer
            coroutineContext.ensureActive()
            val byItem = active.associateBy { it.key.item }
            val completed = withTimeoutOrNull(30.seconds) {
                download(byItem.keys.toList()).collect { (item, result) ->
                    val request = byItem[item] ?: return@collect
                    synchronized(lock) {
                        if (finish(request, result) && result.isSuccess) downloaded++
                        publishProgress()
                    }
                }
                true
            }
            if (completed == null) {
                completeFailures(active, IOException("Thumbnail download timed out"))
            }
        }
    }

    private fun checkCache(candidates: List<Request<K, I, V>>): List<Request<K, I, V>> {
        val checked = candidates.map { it to cached(it.key.item) }
        return synchronized(lock) {
            buildList {
                for ((request, value) in checked) {
                    if (requests[request.key] !== request || request.waiters == 0) continue
                    if (value == null) {
                        add(request)
                    } else {
                        finish(request, Result.success(value))
                    }
                }
                publishProgress()
            }
        }
    }

    private fun completeFailures(items: List<Request<K, I, V>>, error: Exception) {
        synchronized(lock) {
            items.forEach { finish(it, Result.failure(error)) }
            publishProgress()
        }
    }

    private fun finish(request: Request<K, I, V>, result: Result<V>): Boolean {
        if (requests[request.key] !== request) return false
        requests.remove(request.key)
        result.fold(request.result::complete, request.result::completeExceptionally)
        return true
    }

    private fun removeWaiter(request: Request<K, I, V>) {
        request.waiters--
        if (requests[request.key] !== request || request.waiters > 0) return
        val batch = request.batch
        if (batch?.started != true) {
            requests.remove(request.key)
            request.result.cancel()
        }
        if (batch != null && batch.requests.none { requests[it.key] === it && it.waiters > 0 }) {
            batch.requests.forEach { pending ->
                if (requests[pending.key] === pending) {
                    requests.remove(pending.key)
                    pending.result.cancel()
                }
            }
            batch.job.cancel()
        }
        publishProgress()
    }

    private fun publishProgress() {
        val pending = requests.values.count { it.waiters > 0 }
        val active = requests.values.count { it.waiters > 0 && it.batch?.started == true }
        mutableProgress.value = ThumbnailDownloadProgress(pending - active, active, downloaded)
    }

    private companion object {
        const val MAX_BATCH_SIZE = 16
        const val MAX_SCHEDULED_BATCHES = 4
    }
}
