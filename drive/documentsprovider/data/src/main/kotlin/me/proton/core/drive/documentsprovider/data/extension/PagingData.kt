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

package me.proton.core.drive.documentsprovider.data.extension

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.paging.CombinedLoadStates
import androidx.paging.DifferCallback
import androidx.paging.LoadState
import androidx.paging.NullPaddedList
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [PagingData] doesn't allow to fetch its content directly. Yet, as we use it to fetch the content and
 * [android.content.ContentProvider]s require us to provide the data in [Cursor]s, we need a way to extract the content
 * from the [PagingData] and transform it into a [Cursor].
 * This method allows us to wait until the content is available and extract it into a [Cursor].
 * On following changes in the data, it notifies the [android.content.ContentProvider] through the [uri] provided.
 */
suspend fun <T : Any> Flow<PagingData<T>>.asCursor(
    context: Context,
    uri: Uri,
    projection: Array<out String>,
    transform: MatrixCursor.RowBuilder.(T) -> Unit,
): Cursor {

    val mutex = Mutex(locked = true)
    var list: List<T?>? = null

    val collector = CollectPagingDiffer<T> { values ->
        val savedList = list
        if (savedList != null) {
            context.contentResolver.notifyChange(uri, null)
        } else {
            list = values
            mutex.unlock()
        }
    }
    val job = Job()
    CoroutineScope(job).launch {
        collectLatest { pagingData ->
            if (job.isCancelled) {
                return@collectLatest
            }
            collector.differ.collectFrom(pagingData)
        }
    }
    CoroutineScope(job).launch {
        collector.differ.loadStateFlow.collectLatest { loadState ->
            if (job.isCancelled) {
                return@collectLatest
            }
            if (list != null && loadState != null && !loadState.isLoading) {
                context.contentResolver.notifyChange(uri, null)
            }
        }
    }
    return mutex.withLock { // We wait for the first collect to happen
        val loadState = collector.differ.loadStateFlow.first()
        val isLoading = loadState?.isLoading ?: false
        list?.let { list ->
            if (list.isNotEmpty()) {
                // We want to trigger the next call if needed
                collector.differ[list.size - 1]
            }
        }
        object : MatrixCursor(projection, list?.size ?: 0) {

            override fun getExtras(): Bundle = Bundle(1).apply {
                putBoolean(DocumentsContract.EXTRA_LOADING, isLoading)
            }

            override fun close() {
                job.cancel()
                super.close()
            }
        }.apply {
            setNotificationUri(context.contentResolver, uri)
            list?.forEach { item ->
                if (item != null) {
                    with(newRow()) { transform(item) }
                }
            }
        }
    }
}

private val CombinedLoadStates.isLoading: Boolean get() = append == LoadState.Loading || refresh == LoadState.Loading

private class CollectPagingDiffer<T : Any>(
    private val onList: (List<T?>) -> Unit,
) {

    private val callback = object : DifferCallback {
        override fun onChanged(position: Int, count: Int) {
            if (count > 0) {
                notifyOnList()
            }
        }

        override fun onInserted(position: Int, count: Int) {
            if (count > 0) {
                notifyOnList()
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            if (count > 0) {
                notifyOnList()
            }
        }
    }

    val differ = object : PagingDataDiffer<T>(
        differCallback = callback,
    ) {
        override suspend fun presentNewList(
            previousList: NullPaddedList<T>,
            newList: NullPaddedList<T>,
            lastAccessedIndex: Int,
            onListPresentable: () -> Unit,
        ): Int? {
            onListPresentable()
            notifyOnList()
            return null
        }
    }

    private fun notifyOnList() {
        onList(differ.snapshot())
    }
}
