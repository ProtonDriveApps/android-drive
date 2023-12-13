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
package me.proton.core.drive.files.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class ListEffect {
    REFRESH, RETRY
}

@Composable
fun <T : Any> Flow<ListEffect>.HandleListEffect(items: LazyPagingItems<T>) {
    LaunchedEffect(this, LocalContext.current) {
        this@HandleListEffect
            .onEach { effect ->
                when (effect) {
                    ListEffect.REFRESH -> items.apply { refresh() }
                    ListEffect.RETRY -> items.apply { retry() }
                }
            }
            .launchIn(this)
    }
}
