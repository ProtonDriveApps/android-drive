/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.base.data.db.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil

/**
 * When using Room's [PagingSource] implementation, if the tables listened on are being updated
 * constantly, this leads to a storm of `PagingSource.invalidate()` which blocks the refresh and
 * makes it look like the loading takes a lot of time. Using this, we only invalidate when the data
 * we are observing changes. This solution is great for lists which are not too big and on which
 * paging from the database doesn't really matter.
 */
fun <T : Any> Flow<Result<List<T>>>.asPagingSource(
    stopOnFailure: Boolean = true,
    processPage: (suspend (List<T>) -> List<T>)? = null,
): PagingSource<Int, T> =
    object : PagingSource<Int, T>() {

        private val listFlow: StateFlow<Result<List<T>>?> =
            takeWhile { invalid.not() }
                .distinctUntilChanged()
                .mapWithPrevious { previous, current ->
                    if (previous != null && !(previous.isFailure && stopOnFailure)) {
                        invalidate()
                    }
                    current
                }
                .stateIn(PagingSourceScope, SharingStarted.Eagerly, null)

        override fun getRefreshKey(state: PagingState<Int, T>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
            }
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
            val list = listFlow.filterNotNull().first()
            val pageKey = params.getPageKey(list.getOrNull()?.size ?: 0)
            return try {
                val pageList = list
                    .onFailure { throwable ->
                        val error = throwable.cause ?: throwable
                        CoreLogger.d(LogTag.PAGING, throwable, "load (key=$pageKey) from flow failed with $error")
                        return LoadResult.Error(error)
                    }
                    .getOrThrow()

                val pages = pageList.chunked(params.loadSize)
                val page = pages.getOrNull(pageKey) ?: emptyList()
                val prevKey = (pageKey - 1).takeIf { key -> key >= 0 }
                val nextKey = (pageKey + 1).takeIf { key -> key <= pages.size - 1 }
                CoreLogger.d(
                    tag = LogTag.PAGING,
                    message = """
                        load (key=$pageKey, items=${page.size}) from flow (items=${pageList.size})
                        nextKey=$nextKey prevKey=$prevKey
                    """.trimIndent()
                )
                LoadResult.Page(
                    data = processPage?.invoke(page) ?: page,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            } catch (e: CancellationException) {
                CoreLogger.d(LogTag.PAGING, e, "load (key=$pageKey) from flow failed due to CancellationException")
                LoadResult.Invalid()
            } catch (e: Throwable) {
                val error = e.cause ?: e
                CoreLogger.d(LogTag.PAGING, e, "load (key=$pageKey) from flow failed with $error")
                LoadResult.Error(error)
            }
        }
    }

object PagingSourceScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> ((fromIndex: Int, count: Int) -> Flow<Result<List<T>>>).asPagingSource(
    sourceSize: Flow<Int>,
    observablePageSize: Int,
    stopOnFailure: Boolean = true,
    processPage: (suspend (List<T>) -> List<T>)? = null,
): PagingSource<Int, T> =
    object : PagingSource<Int, T>() {

        private val itemsCount: StateFlow<Int?> = sourceSize
            .takeWhile { invalid.not() }
            .distinctUntilChanged()
            .mapWithPrevious { previous, current ->
                if (previous != null) {
                    CoreLogger.d(
                        LogTag.PAGING,
                        "Invalidating due to items count change (previous = $previous, current = $current)",
                    )
                    invalidate()
                }
                current
            }
            .stateIn(PagingSourceScope, SharingStarted.Eagerly, null)

        private val fromIndex = MutableStateFlow<Int?>(null)

        private val observableList: StateFlow<Result<List<T>>?> = fromIndex
            .takeWhile { invalid.not() }
            .distinctUntilChanged()
            .filterNotNull()
            .transformLatest { fromIndex ->
                emitAll(
                    this@asPagingSource(fromIndex, observablePageSize)
                        .takeWhile { invalid.not() }
                        .distinctUntilChanged()
                        .mapWithPrevious { previous, current ->
                            if (previous != null && !(previous.isFailure && stopOnFailure)) {
                                CoreLogger.d(
                                    LogTag.PAGING,
                                    "Invalidating due to observable list change",
                                )
                                invalidate()
                            }
                            current
                        }
                )
            }
            .stateIn(PagingSourceScope, SharingStarted.Eagerly, null)

        override fun getRefreshKey(state: PagingState<Int, T>): Int? =
            state.anchorPosition?.let { anchorPosition ->
                (state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1))?.also {
                        CoreLogger.d(LogTag.PAGING, "getRefreshKey page $it anchorPosition $anchorPosition")
                }
            }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
            CoreLogger.d(LogTag.PAGING, "load params key = ${params.key} type = ${params.type}")
            require(observablePageSize >= params.loadSize * 4) {
                """
                    Observable page size ($observablePageSize) must be at least 4 times as big
                    as load page size (${params.loadSize})
                """.trimIndent()
            }
            val items = itemsCount.filterNotNull().first()
            val pageKey = params.getPageKey(items)
            val pageRange = rangeFromPage(pageKey, params.loadSize, items)
            val currentIndex = findIndexForRange(pageRange, observablePageSize, items)
            fromIndex.value = currentIndex
            return try {
                val pageList = this@asPagingSource(currentIndex, observablePageSize).first()
                    .onFailure { throwable ->
                        val error = throwable.cause ?: throwable
                        CoreLogger.d(LogTag.PAGING, throwable, "load (key=$pageKey) from flow failed with $error")
                        return LoadResult.Error(error)
                    }
                    .getOrThrow()
                    .drop((pageRange.first - currentIndex).coerceAtLeast(minimumValue = 0))
                    .take(params.loadSize)
                val prevKey = (pageKey - 1).takeIf { key -> key >= 0 }
                val nextKey = (pageKey + 1).takeIf { key -> key < ceil(items / params.loadSize.toDouble()).toInt() }
                val itemsBefore = params.loadSize * pageKey
                CoreLogger.d(
                    tag = LogTag.PAGING,
                    message = """
                        load (key=$pageKey, items=${pageList.size})
                        nextKey=$nextKey prevKey=$prevKey range=$pageRange itemsBefore=$itemsBefore
                    """.trimIndent()
                )
                LoadResult.Page(
                    data = processPage?.invoke(pageList) ?: pageList,
                    prevKey = prevKey,
                    nextKey = nextKey,
                    itemsBefore = itemsBefore,
                )
            } catch (e: CancellationException) {
                CoreLogger.d(LogTag.PAGING, e, "load (key=$pageKey) from flow failed due to CancellationException")
                LoadResult.Invalid()
            } catch (e: Throwable) {
                val error = e.cause ?: e
                CoreLogger.d(LogTag.PAGING, e, "load (key=$pageKey) from flow failed with $error")
                LoadResult.Error(error)
            }
        }

        private fun rangeFromPage(pageIndex: Int, pageSize: Int, itemsCount: Int): IntRange = takeIf { itemsCount > 0 }
            ?.let {
                IntRange(
                    minOf(pageIndex * pageSize, itemsCount - 1),
                    minOf(((pageIndex + 1) * pageSize) - 1, itemsCount - 1),
                )
            } ?: IntRange(0, 0)

        private fun findIndexForRange(pageRange: IntRange, observablePageSize: Int, itemsCount: Int): Int = when {
            itemsCount <= observablePageSize -> 0
            pageRange.first <= observablePageSize / 2 -> 0
            itemsCount - pageRange.last <= observablePageSize / 2 -> itemsCount - observablePageSize
            else -> pageRange.first - observablePageSize / 2
        }
    }

private fun PagingSource.LoadParams<Int>.getPageKey(items: Int): Int = when (val key = this.key) {
    null -> 0
    else -> {
        if (key * loadSize <= items) {
            key
        } else {
            (items / loadSize).also { pageKey ->
                CoreLogger.d(
                    tag = LogTag.PAGING,
                    message = "Requested page key $key was changed to $pageKey because of items $items",
                )
            }
        }
    }
}

private val PagingSource.LoadParams<Int>.type: String get() = when(this) {
    is PagingSource.LoadParams.Refresh -> "REFRESH"
    is PagingSource.LoadParams.Append -> "APPEND"
    is PagingSource.LoadParams.Prepend -> "PREPEND"
}
