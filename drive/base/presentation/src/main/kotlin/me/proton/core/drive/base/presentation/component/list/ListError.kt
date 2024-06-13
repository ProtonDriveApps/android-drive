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

package me.proton.core.drive.base.presentation.component.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.ErrorPadding
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonErrorMessageWithAction

@Composable
fun ListError(
    message: String,
    @StringRes actionResId: Int?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(ErrorPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (actionResId != null) {
            ProtonErrorMessageWithAction(
                errorMessage = message,
                action = stringResource(id = actionResId),
                onAction = onAction
            )
        } else {
            ProtonErrorMessage(errorMessage = message)
        }
    }
}
