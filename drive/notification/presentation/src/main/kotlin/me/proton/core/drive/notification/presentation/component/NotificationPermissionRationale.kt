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

package me.proton.core.drive.notification.presentation.component

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.MediumLargeSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.notification.presentation.viewevent.NotificationPermissionRationaleViewEvent
import me.proton.core.drive.notification.presentation.viewmodel.NotificationPermissionRationaleViewModel
import me.proton.core.drive.notification.presentation.viewstate.NotificationPermissionRationaleViewState

@RequiresApi(TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionRationale(
    runAction: RunAction,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<NotificationPermissionRationaleViewModel>()
    val permissionState = rememberPermissionState(permission = POST_NOTIFICATIONS)
    val viewEvent = remember {
        viewModel.viewEvent(permissionState, runAction, dismiss)
    }
    NotificationPermissionRationale(
        viewState =viewModel.initialViewState,
        viewEvent = viewEvent,
        modifier = modifier,
    )
}

@Composable
fun NotificationPermissionRationale(
    viewState: NotificationPermissionRationaleViewState,
    viewEvent: NotificationPermissionRationaleViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding(),
    ) {
        Title(title = viewState.title)
        Description(description = viewState.description)
        BottomActions(
            primaryActionTitle = viewState.acceptActionTitle,
            secondaryActionTitle = viewState.rejectActionTitle,
            onPrimaryAction = viewEvent.onAccept,
            onSecondaryAction = viewEvent.onReject,
        )
    }
}

@Composable
internal fun Title(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        textAlign = TextAlign.Center,
        style = ProtonTheme.typography.headlineNorm,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MediumSpacing, bottom = SmallSpacing)
            .padding(horizontal = MediumSpacing)
    )
}

@Composable
internal fun Description(
    description: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = description,
        textAlign = TextAlign.Center,
        style = ProtonTheme.typography.body1Regular,
        color = ProtonTheme.colors.textWeak,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MediumSpacing)
    )
}

@Composable
internal fun BottomActions(
    primaryActionTitle: String,
    secondaryActionTitle: String,
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    val buttonModifier = Modifier
        .conditional(isPortrait) {
            fillMaxWidth()
        }
        .conditional(isLandscape) {
            widthIn(min = ButtonMinWidth)
        }
        .heightIn(min = ListItemHeight)
    Column(
        modifier = modifier
            .padding(top = MediumLargeSpacing, bottom = MediumSpacing)
            .padding(horizontal = MediumSpacing),
    ) {
        ProtonSolidButton(
            modifier = buttonModifier,
            onClick = onPrimaryAction,
        ) {
            Text(
                text = primaryActionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediumSpacing)
        )
        ProtonTextButton(
            modifier = buttonModifier,
            onClick = onSecondaryAction,
        ) {
            Text(
                text = secondaryActionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ButtonMinWidth = 300.dp

@Preview
@Composable
fun NotificationPermissionRationalePreview() {
    ProtonTheme {
        NotificationPermissionRationale(
            viewState = NotificationPermissionRationaleViewState(
                title = "Get notify on backup status",
                description = "Receive alerts on the progress and status of your backups",
                acceptActionTitle = "Allow notification",
                rejectActionTitle = "No thanks",
            ),
            viewEvent = object : NotificationPermissionRationaleViewEvent {
                override val onAccept = {}
                override val onReject = {}
            }
        )
    }
}
