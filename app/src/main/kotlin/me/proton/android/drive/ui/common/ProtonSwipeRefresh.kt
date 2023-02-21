/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.files.presentation.state.ListContentState

@Composable
fun ProtonSwipeRefresh(
    listContentState: ListContentState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    swipeEnabled: Boolean = listContentState !is ListContentState.Loading,
    content: @Composable () -> Unit
) {
    SwipeRefresh(
        modifier = modifier,
        state = rememberSwipeRefreshState(listContentState.isRefreshing),
        onRefresh = onRefresh,
        swipeEnabled = swipeEnabled,
        indicatorPadding = PaddingValues(top = topPadding),
        indicator = { s, trigger ->
            SwipeRefreshIndicator(
                state = s,
                refreshTriggerDistance = trigger,
                contentColor = ProtonTheme.colors.brandNorm,
            )
        },
        content = content,
    )
}
