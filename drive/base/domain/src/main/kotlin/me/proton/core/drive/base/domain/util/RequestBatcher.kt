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

package me.proton.core.drive.base.domain.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * Batches individual [enqueue] calls by key [K], collecting items [I] for up to [delay]
 * before issuing a single [process] call. Each caller suspends until the [process] flow
 * emits a matching result for its item.
 *
 * @param K key that groups requests into a single batch
 * @param I item identifier; must implement [equals] correctly for result matching —
 *   duplicate items in the same batch all receive the same result
 * @param R result type returned per item
 * @param scope scope used to launch the delayed batch jobs
 * @param delay window during which items are accumulated before the batch is processed
 * @param timeout maximum duration to wait for [process] to emit all results; callers whose
 *   item was not resolved in time receive a [TimeoutCancellationException]. Null means no timeout.
 * @param process called with the batch key and deduplicated items; must emit one
 *   [Pair]<[I], [Result]<[R]>> per unique item — all callers sharing the same item receive the same result
 */
class RequestBatcher<K, I, R>(
    private val scope: CoroutineScope,
    private val delay: Duration,
    private val timeout: Duration? = null,
    private val process: suspend (key: K, items: List<I>) -> Flow<Pair<I, Result<R>>>,
) {
    private class PendingRequest<I, R>(
        val item: I,
        val deferred: CompletableDeferred<R>,
    )

    private val mutex = Mutex()
    private val pendingRequests = mutableMapOf<K, MutableList<PendingRequest<I, R>>>()
    private val pendingKeys = mutableSetOf<K>()

    suspend fun enqueue(key: K, item: I): R {
        val deferred = CompletableDeferred<R>()
        mutex.withLock {
            pendingRequests.getOrPut(key) { mutableListOf() }.add(PendingRequest(item, deferred))
            if (pendingKeys.add(key)) {
                scope.launch {
                    delay(delay)
                    val requests = mutex.withLock {
                        pendingKeys.remove(key)
                        pendingRequests.remove(key) ?: emptyList()
                    }
                    processBatch(key, requests)
                }
            }
        }
        return deferred.await()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun processBatch(key: K, requests: List<PendingRequest<I, R>>) {
        if (requests.isEmpty()) {
            return
        }
        val requestsByItem = requests.groupBy { it.item }
        val uniqueItems = requestsByItem.keys.toList()
        suspend fun collect() = process(key, uniqueItems)
            .take(uniqueItems.size)
            .collect { (item, result) ->
                requestsByItem[item]?.forEach { request ->
                    if (request.deferred.isActive) {
                        result
                            .onSuccess(request.deferred::complete)
                            .onFailure(request.deferred::completeExceptionally)
                    }
                }
            }
        try {
            if (timeout != null) {
                withTimeout(timeout) { collect() }
            } else {
                collect()
            }
            requests.completeExceptionally { req ->
                IllegalStateException("No result received for item ${req.item}")
            }
        } catch (timeoutException: TimeoutCancellationException) {
            requests.completeExceptionally { timeoutException }
        } catch (cancellationException: CancellationException) {
            requests.completeExceptionally { cancellationException }
            throw cancellationException
        } catch (error: Exception) {
            requests.completeExceptionally { error }
        } catch (error: Throwable) {
            requests.completeExceptionally { error }
            throw error
        }
    }

    private inline fun List<PendingRequest<I, R>>.completeExceptionally(
        block: (PendingRequest<I, R>) -> Throwable
    ) = forEach { req ->
        if (req.deferred.isActive) {
            req.deferred.completeExceptionally(block(req))
        }
    }
}
