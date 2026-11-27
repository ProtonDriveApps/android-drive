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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import me.proton.core.account.domain.entity.Account
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.presentation.extension.conditional

@Composable
fun AppLock(
    locked: Flow<Boolean>,
    primaryAccount: Flow<Account?>,
    content: @Composable () -> Unit,
) {
    val isLocked by locked.collectAsStateWithLifecycle(false)
    val account by primaryAccount.collectAsStateWithLifecycle(null)
    val lockedUserId: UserId? = if (isLocked) account?.userId else null

    // Keeps the last locked user so the lock is still drawn while it fades out.
    var unlockUserId by remember { mutableStateOf(lockedUserId) }
    if (lockedUserId != null) unlockUserId = lockedUserId

    val showLock = lockedUserId != null

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hides the content from accessibility services while locked.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .conditional(showLock) { clearAndSetSemantics {} }
        ) {
            content()
        }

        // Covers the content at once when locking, so it never shows through;
        // fades out only after a successful unlock.
        AnimatedVisibility(
            visible = showLock,
            enter = EnterTransition.None,
            exit = fadeOut(),
        ) {
            // Keeps touches from reaching the content while locked.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .conditional(showLock) { pointerInput(Unit) {} }
            ) {
                unlockUserId?.let { userId ->
                    Unlock(
                        userId = userId,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
