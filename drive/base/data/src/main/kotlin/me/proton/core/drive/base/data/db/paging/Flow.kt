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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import kotlin.coroutines.CoroutineContext

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
            val pageKey = params.key ?: 0
            val list = listFlow.filterNotNull().first()
            return try {
                val pageList = list
                    .onFailure { throwable ->
                        val error = throwable.cause ?: throwable
                        CoreLogger.d(LogTag.PAGING, "load (key=$pageKey) from flow failed with $error")
                        return LoadResult.Error(error)
                    }
                    .getOrThrow()

                val pages = pageList.chunked(params.loadSize)
                val page = pages.getOrNull(pageKey) ?: emptyList()
                val prevKey = (pageKey - 1).takeIf { key -> key >= 0 }
                val nextKey = (pageKey + 1).takeIf { key -> key <= pages.size - 1 }
                CoreLogger.d(
                    tag = LogTag.PAGING,
                    message = "load (key=$pageKey, items=${page.size}) from flow (items=${pageList.size}) nextKey=$nextKey prevKey=$prevKey"
                )
                LoadResult.Page(
                    data = processPage?.invoke(page) ?: page,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            } catch (e: Throwable) {
                CoreLogger.d(LogTag.PAGING, "load (key=$pageKey) from flow failed with ${e.cause ?: e}")
                LoadResult.Invalid()
            }
        }
    }

object PagingSourceScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()
}
