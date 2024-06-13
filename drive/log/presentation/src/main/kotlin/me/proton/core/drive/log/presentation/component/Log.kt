/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionStrongNorm
import me.proton.core.drive.log.presentation.entity.LogItem

@Composable
fun Log(
    logs: Flow<PagingData<LogItem>>,
    modifier: Modifier = Modifier,
) {
    val pagingData = logs.collectAsLazyPagingItems()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    )
    {
        items(
            count = pagingData.itemCount,
            key = pagingData.itemKey { logItem -> logItem.id },
        ) { index ->
            pagingData[index]?.let { logItem ->
                when (logItem) {
                    is LogItem.Separator -> SeparatorItem(
                        title = logItem.value,
                    )
                    is LogItem.Log -> LogItem(
                        time = logItem.creationTime,
                        message = logItem.message,
                        content = logItem.moreContent,
                        messageStyle = if (logItem.isError) {
                            ProtonTheme.typography.captionStrongNorm.copy(color = ProtonTheme.colors.notificationWarning)
                        } else {
                            ProtonTheme.typography.captionStrongNorm
                        }
                    )
                }
            }
        }
    }
}
