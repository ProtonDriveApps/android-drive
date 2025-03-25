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
package me.proton.core.drive.files.presentation.component.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.compose.component.ErrorPadding
import me.proton.core.compose.component.ProtonErrorMessageWithAction

@Composable
fun FilesListFooter(
    listContentAppendingState: ListContentAppendingState,
    onErrorAction: () -> Unit
) {
    when (listContentAppendingState) {
        ListContentAppendingState.Idle -> Unit
        ListContentAppendingState.Loading -> FilesListFooterProgress()
        is ListContentAppendingState.Error -> FilesListFooterError(
            errorMessage = listContentAppendingState.message,
            action = stringResource(id = listContentAppendingState.actionResId),
            onErrorAction = onErrorAction
        )
    }
}

@Composable
fun FilesListFooterProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Height),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun FilesListFooterError(
    errorMessage: String,
    action: String,
    onErrorAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ErrorPadding),
        contentAlignment = Alignment.Center
    ) {
        ProtonErrorMessageWithAction(
            errorMessage = errorMessage,
            action = action,
            onAction = onErrorAction
        )
    }
}
