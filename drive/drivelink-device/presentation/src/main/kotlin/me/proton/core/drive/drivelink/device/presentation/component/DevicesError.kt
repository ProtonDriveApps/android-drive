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

package me.proton.core.drive.drivelink.device.presentation.component

import androidx.annotation.StringRes
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.i18n.R as I18N

@Composable
fun DevicesError(
    errorMessage: String,
    @StringRes actionResId: Int?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    ListError(
        message = errorMessage,
        actionResId = actionResId,
        modifier = modifier,
        onAction = onAction,
    )
}

@Preview
@Composable
fun PreviewDevicesError() {
    ProtonTheme {
        Surface {
            DevicesError(
                errorMessage = "No connection",
                actionResId = null,
            ) {}
        }
    }
}

@Preview
@Composable
fun PreviewDevicesErrorWithRetry() {
    ProtonTheme {
        Surface {
            DevicesError(
                errorMessage = "Server error occurred",
                actionResId = I18N.string.common_retry_action,
            ) {}
        }
    }
}
