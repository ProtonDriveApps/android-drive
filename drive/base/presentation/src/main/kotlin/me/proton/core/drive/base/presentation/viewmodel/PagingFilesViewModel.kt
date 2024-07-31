/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.base.presentation.viewmodel

import android.content.Context
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.drive.i18n.R as I18N

fun onLoadState(
    appContext: Context,
    useExceptionMessage: Boolean,
    listContentState: MutableStateFlow<ListContentState>,
    listAppendContentState: MutableStateFlow<ListContentAppendingState>,
    coroutineScope: CoroutineScope,
    emptyState: StateFlow<ListContentState.Empty>,
    onError: (message: String) -> Unit,
): (CombinedLoadStates, Int) -> Unit {
    val flow = MutableSharedFlow<Pair<CombinedLoadStates, Int>>()
    // Even when combining the [LoadState], there is a split second where the state is not loading and the itemCount
    // is still 0 but should not, therefore, the debounce is here to prevent this unwanted state.
    flow
        //.debounce(500L)
        .onEach { (loadState, itemCount) ->
            listContentState.processRefreshState(
                appContext = appContext,
                useExceptionMessage = useExceptionMessage,
                refresh = loadState.refresh.combine(loadState.source.refresh),
                isRemoteRefreshLoading = loadState.mediator?.refresh == LoadState.Loading,
                endOfPaginationReached = loadState.append.endOfPaginationReached,
                itemCount = itemCount,
                emptyState = emptyState.value,
                onError = onError,
            )
            listAppendContentState.processAppendState(loadState.append, loadState.refresh)
        }.launchIn(coroutineScope)
    return { loadState: CombinedLoadStates, itemCount: Int ->
        coroutineScope.launch {
            flow.emit(loadState to itemCount)
        }
    }
}

private fun MutableStateFlow<ListContentState>.processRefreshState(
    appContext: Context,
    useExceptionMessage: Boolean,
    refresh: LoadState,
    isRemoteRefreshLoading: Boolean,
    endOfPaginationReached: Boolean,
    itemCount: Int,
    emptyState: ListContentState.Empty,
    onError: (String) -> Unit,
) {
    value = when (refresh) {
        LoadState.Loading -> if (itemCount > 0) {
            ListContentState.Content(isRefreshing = isRemoteRefreshLoading)
        } else {
            value.setRefreshing(isRemoteRefreshLoading)
        }
        is LoadState.NotLoading -> {
            if (itemCount == 0) {
                if (!endOfPaginationReached) {
                    value // we're waiting for loading
                } else {
                    emptyState
                }
            } else {
                ListContentState.Content()
            }
        }
        is LoadState.Error -> {
            refresh.error.message?.let {
                val message = refresh.error.getDefaultMessage(appContext, useExceptionMessage)
                if (itemCount > 0) {
                    onError(message)
                    ListContentState.Content()
                } else {
                    ListContentState.Error(message, I18N.string.common_retry)
                }
            } ?: ListContentState.Content()
        }
    }.exhaustive
}

fun ListContentState.setRefreshing(isRemoteRefreshLoading: Boolean) = when (this) {
    is ListContentState.Content -> copy(isRefreshing = isRemoteRefreshLoading)
    is ListContentState.Empty -> copy(isRefreshing = isRemoteRefreshLoading)
    is ListContentState.Error -> copy(isRefreshing = isRemoteRefreshLoading)
    ListContentState.Loading -> this
}

private fun MutableStateFlow<ListContentAppendingState>.processAppendState(
    append: LoadState,
    refresh: LoadState,
) {
    value = if (refresh is LoadState.Loading) {
        ListContentAppendingState.Idle
    } else {
        when (append) {
            is LoadState.NotLoading -> ListContentAppendingState.Idle
            is LoadState.Loading -> ListContentAppendingState.Loading
            is LoadState.Error -> append.error.message?.run {
                ListContentAppendingState.Error(this, I18N.string.common_retry)
            } ?: ListContentAppendingState.Idle
        }.exhaustive
    }
}

/**
 *  We need to show a loading while the refresh state and the source's refresh state are loading.
 *  This method helps combine them together to make the detection of the state easier
 */
private fun LoadState.combine(loadState: LoadState): LoadState {
    if (this is LoadState.Loading || loadState is LoadState.Loading) {
        return LoadState.Loading
    }
    if (this is LoadState.Error) {
        return this
    }
    if (loadState is LoadState.Error) {
        return loadState
    }
    return LoadState.NotLoading(this.endOfPaginationReached || loadState.endOfPaginationReached)
}
