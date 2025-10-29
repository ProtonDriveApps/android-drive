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
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.drivelink.domain.entity.DriveLink

/**
 * [PagingData] doesn't allow to fetch its content directly. Yet, as we use it to fetch the content and
 * [android.content.ContentProvider]s require us to provide the data in [Cursor]s, we need a way to extract the content
 * from the [PagingData] and transform it into a [Cursor].
 * This method allows us to wait until the content is available and extract it into a [Cursor].
 * On following changes in the data, it notifies the [android.content.ContentProvider] through the [uri] provided.
 */
suspend fun Flow<PagingData<DriveLink>>.asCursor(
    context: Context,
    uri: Uri,
    projection: Array<out String>,
    transform: MatrixCursor.RowBuilder.(DriveLink) -> Unit,
): Cursor {

    val mutex = Mutex(locked = true)
    var list: List<DriveLink?>? = null

    val collector = CollectPagingDiffer { values ->
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
            collector.differ.submitData(pagingData)
            //collector.differ.collectFrom(pagingData)
        }
    }
    CoroutineScope(job).launch {
        collector.differ.loadStateFlow.collectLatest { loadState ->
            if (job.isCancelled) {
                return@collectLatest
            }
            if (list != null && !loadState.isLoading) {
                context.contentResolver.notifyChange(uri, null)
            }
        }
    }
    return mutex.withLock { // We wait for the first collect to happen
        val loadState = collector.differ.loadStateFlow.first()
        val isLoading = loadState.isLoading
        list?.let { list ->
            if (list.isNotEmpty()) {
                // We want to trigger the next call if needed
                collector.differ.getItem(list.size - 1)
                //collector.differ[list.size - 1]
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

private class CollectPagingDiffer(
    private val onList: (List<DriveLink?>) -> Unit,
) {

    private val callback = NotifyOnListUpdateCallback(::notifyOnList)

    val differ = AsyncPagingDataDiffer(
        diffCallback = object : DiffUtil.ItemCallback<DriveLink>() {
            override fun areItemsTheSame(oldItem: DriveLink, newItem: DriveLink): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DriveLink, newItem: DriveLink): Boolean =
                oldItem == newItem
        },
        updateCallback = callback,
    )

    private fun notifyOnList() {
        differ.snapshot()
        onList(differ.snapshot())
    }
}

private class NotifyOnListUpdateCallback(
    private val notifyOnList: () -> Unit
) : ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {
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

    override fun onMoved(fromPosition: Int, toPosition: Int) {}
}
