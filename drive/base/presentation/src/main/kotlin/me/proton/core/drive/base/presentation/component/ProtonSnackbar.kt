/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.presentation.component

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun ProtonSnackbarHost(
    hostState: ProtonSnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { data ->
        ProtonSnackbar(snackbarData = data, hostState.type)
    },
) {
    SnackbarHost(
        hostState = hostState.snackbarHostState,
        modifier = modifier,
        snackbar = snackbar
    )
}

@Stable
class ProtonSnackbarHostState(
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    defaultType: ProtonSnackbarType = ProtonSnackbarType.WARNING
) {

    private val mutex = Mutex()

    var type by mutableStateOf(defaultType)
        private set

    suspend fun showSnackbar(
        type: ProtonSnackbarType,
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ): SnackbarResult = mutex.withLock {
        this.type = type
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration,
        )
    }
}

@Composable
fun ProtonSnackbar(
    snackbarData: SnackbarData,
    type: ProtonSnackbarType,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = ProtonTheme.shapes.medium,
    contentColor: Color = ProtonTheme.colors.textInverted,
    actionColor: Color = ProtonTheme.colors.textInverted,
    elevation: Dp = 6.dp,
) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier.shadow(elevation, shape),
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = when (type) {
            ProtonSnackbarType.SUCCESS -> ProtonTheme.colors.notificationSuccess
            ProtonSnackbarType.WARNING -> ProtonTheme.colors.notificationWarning
            ProtonSnackbarType.ERROR -> ProtonTheme.colors.notificationError
            ProtonSnackbarType.NORM -> ProtonTheme.colors.notificationNorm
        },
        contentColor = contentColor,
        actionColor = actionColor,
        actionContentColor = actionColor,
        dismissActionContentColor = contentColor,
    )
}

enum class ProtonSnackbarType {
    SUCCESS, WARNING, ERROR, NORM
}
