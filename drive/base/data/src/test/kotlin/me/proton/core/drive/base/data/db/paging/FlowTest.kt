/*
 * Copyright (c) 2023 Proton AG.
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.assertEquals
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagingSourceTest {
    private val backingData = listOf(
        "first",
        "second",
        "third",
        "fourth",
        "fifth",
        "sixth",
        "seventh",
        "eighth",
        "ninth",
        "tenth",
        "eleventh",
        "twelfth",
        "thirteenth",
        "fourteenth",
        "fifteenth",
    )
    private val source = MutableStateFlow(backingData)
    private fun flowFromBackingData(fromIndex: Int, count: Int): Flow<Result<List<String>>> = flow {
        val source = this@PagingSourceTest.source.value
        emit(
            when {
                fromIndex < 0 || fromIndex >= source.count() -> Result.failure(IllegalArgumentException())
                else -> Result.success(source.subList(fromIndex, (fromIndex + count).coerceIn(0, source.size)))
            }
        )
    }
    private fun pagingSource(observablePageSize: Int = DEFAULT_OBSERVABLE_PAGE_SIZE_S) = { fromIndex: Int, count: Int ->
        flowFromBackingData(fromIndex, count)
    }.asPagingSource(
        sourceSize = source.map { backingData -> backingData.size },
        observablePageSize = observablePageSize,
    )

    @Test
    fun `refresh paging list when observable page size is bigger then underlying data`() = runTest {
        val pagingSource = pagingSource(maxOf(DEFAULT_OBSERVABLE_PAGE_SIZE_L, backingData.size))
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("first", "second", "third"),
                prevKey = null,
                nextKey = 1,
            ),
            actual = pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = LOAD_SIZE_M,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("thirteenth", "fourteenth", "fifteenth"),
                prevKey = 1,
                nextKey = null,
            ),
            actual = pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = 2,
                    loadSize = LOAD_SIZE_L,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
    }

    @Test
    fun `refresh paging list`() = runTest {
        val pagingSource = pagingSource(DEFAULT_OBSERVABLE_PAGE_SIZE_M)
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("first", "second"),
                prevKey = null,
                nextKey = 1,
            ),
            actual = pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = LOAD_SIZE_S,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("first", "second", "third"),
                prevKey = null,
                nextKey = 1,
            ),
            actual = pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = LOAD_SIZE_M,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
    }

    @Test
    fun `append list with second page`() = runTest {
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("third", "fourth"),
                prevKey = 0,
                nextKey = 2,
            ),
            actual = pagingSource().load(
                PagingSource.LoadParams.Append(
                    key = 1,
                    loadSize = LOAD_SIZE_S,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
    }

    @Test
    fun `append middle pages`() = runTest {
        val pagingSource = pagingSource(DEFAULT_OBSERVABLE_PAGE_SIZE_M)
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("seventh", "eighth", "ninth"),
                prevKey = 1,
                nextKey = 3,
            ),
            actual = pagingSource.load(
                PagingSource.LoadParams.Append(
                    key = 2,
                    loadSize = LOAD_SIZE_M,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("tenth", "eleventh", "twelfth"),
                prevKey = 2,
                nextKey = 4,
            ),
            actual = pagingSource.load(
                PagingSource.LoadParams.Append(
                    key = 3,
                    loadSize = LOAD_SIZE_M,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
    }

    @Test
    fun `append list with last page`() = runTest {
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("fifteenth"),
                prevKey = 6,
                nextKey = null,
            ),
            actual = pagingSource().load(
                PagingSource.LoadParams.Append(
                    key = 7,
                    loadSize = LOAD_SIZE_S,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
    }

    @Test
    fun `prepend list with third page`() = runTest {
        assertEquals(
            expected = PagingSource.LoadResult.Page(
                data = listOf("fifth", "sixth"),
                prevKey = 1,
                nextKey = 3,
            ),
            actual = pagingSource().load(
                PagingSource.LoadParams.Prepend(
                    key = 2,
                    loadSize = LOAD_SIZE_S,
                    placeholdersEnabled = false,
                )
            ),
            message = { "" },
        )
    }

    @Test
    fun `load page size must be less then observablePageSize`() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            runTest {
                pagingSource(DEFAULT_OBSERVABLE_PAGE_SIZE_S).load(
                    PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = DEFAULT_OBSERVABLE_PAGE_SIZE_S,
                        placeholdersEnabled = false,
                    )
                )
            }
        }
    }

    companion object {
        const val LOAD_SIZE_S = 2
        const val LOAD_SIZE_M = 3
        const val LOAD_SIZE_L = 6
        const val DEFAULT_OBSERVABLE_PAGE_SIZE_S = LOAD_SIZE_S * 4
        const val DEFAULT_OBSERVABLE_PAGE_SIZE_M = LOAD_SIZE_M * 4
        const val DEFAULT_OBSERVABLE_PAGE_SIZE_L = LOAD_SIZE_L * 4
    }
}
