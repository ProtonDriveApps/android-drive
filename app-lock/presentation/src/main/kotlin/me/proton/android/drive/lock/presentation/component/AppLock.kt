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

package me.proton.android.drive.lock.presentation.component

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.account.domain.entity.Account
import me.proton.core.domain.entity.UserId

@Composable
fun AppLock(
    locked: Flow<Boolean>,
    primaryAccount: Flow<Account?>,
    content: @Composable () -> Unit,
) {
    var isLocked by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<UserId?>(null) }
    LaunchedEffect(Unit) {
        locked
            .onEach { locked ->
                isLocked = locked
            }
            .launchIn(this)
        primaryAccount
            .onEach {  account ->
                userId = account?.userId
            }
            .launchIn(this)
    }
    Crossfade(targetState = isLocked) { appLocked ->
        if (appLocked) {
            Unlock(
                userId = userId,
                modifier = Modifier,
            )
        } else {
            content()
        }
    }
}
