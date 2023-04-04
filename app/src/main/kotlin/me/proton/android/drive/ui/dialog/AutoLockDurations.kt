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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewevent.AutoLockDurationsViewEvent
import me.proton.android.drive.ui.viewmodel.AutoLockDurationsViewModel
import me.proton.android.drive.ui.viewstate.AutoLockDurationsViewState
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.drive.settings.presentation.extension.toString

@Composable
fun AutoLockDurations(
    runAction: RunAction,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AutoLockDurationsViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    AutoLockDurations(
        viewState = viewState,
        viewEvent = viewModel.viewEvent(runAction, dismiss),
        modifier = modifier.navigationBarsPadding(),
    )
}

@Composable
fun AutoLockDurations(
    viewState: AutoLockDurationsViewState,
    viewEvent: AutoLockDurationsViewEvent,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            Text(text = viewState.title)
        },
        content = {
            viewState.durations.forEach { duration ->
                Duration(
                    title = duration.toString(LocalContext.current),
                    isSelected = duration == viewState.selected,
                ) {
                    viewEvent.onDuration(duration)
                }
            }
        }
    )
}

@Composable
private fun Duration(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .padding(start = DefaultSpacing)
                .weight(1f),
        )
        RadioButton(
            selected = isSelected,
            onClick = { onClick() },
        )
    }
}

@Preview
@Composable
private fun PreviewSelectedDuration() {
    Duration(title = "Immediately", isSelected = true) {}
}

@Preview
@Composable
private fun PreviewUnselectedDuration() {
    Duration(title = "15 minutes", isSelected = false) {}
}
