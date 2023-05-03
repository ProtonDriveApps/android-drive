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

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.drive.ui.viewmodel.SendFileViewModel
import me.proton.android.drive.ui.viewmodel.ShareState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionHint
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.headline
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.extension.currentLocale
import me.proton.core.drive.i18n.R as I18N

@Composable
@OptIn(ExperimentalCoroutinesApi::class)
fun SendFileDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<SendFileViewModel>()
    val driveLink by rememberFlowWithLifecycle(flow = viewModel.driveLink).collectAsState(initial = null)
    val downloadState by rememberFlowWithLifecycle(viewModel.downloadState).collectAsState(initial = null)
    when (val state = downloadState) {
        is ShareState.Error -> onDismiss()
        is ShareState.Ready -> {
            LocalContext.current.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setDataAndType(state.uri, state.mimeType)
                        .putExtra(Intent.EXTRA_STREAM, state.uri)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    null
                )
            )
            onDismiss()
        }
        else -> if (state != null) {
            ProgressDialog(
                state = state,
                title = driveLink?.name.orEmpty(),
                modifier = modifier,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
internal fun ProgressDialog(
    state: ShareState,
    title: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    ProtonAlertDialog(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = stringResource(id = I18N.string.common_downloading),
                    style = ProtonTheme.typography.headline,
                )
                Text(
                    text = title,
                    style = ProtonTheme.typography.defaultSmallWeak,
                )
            }
        },
        text = {
            ProgressDialogStatus(state = state)
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.common_cancel_action,
                onClick = onDismiss,
                modifier = Modifier.testTag(CancelButtonTestTag)
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}

@Composable
private fun ProgressDialogStatus(
    state: ShareState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ShareState.Decrypting -> ProgressDialogStatus(
            modifier = modifier,
            state = state,
            progress = null,
        )
        is ShareState.Downloading -> {
            val progress by rememberFlowWithLifecycle(flow = state.progress).collectAsState(initial = null)
            ProgressDialogStatus(
                modifier = modifier,
                state = state,
                progress = progress,
            )
        }
        else -> Unit
    }
}

@Composable
private fun ProgressDialogStatus(
    state: ShareState,
    progress: Percentage?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(CircularProgressIndicatorSize),
                strokeWidth = CircularProgressIndicatorStrokeWidth,
            )
            Text(
                modifier = Modifier.padding(start = SmallSpacing),
                maxLines = 1,
                text = when (state) {
                    is ShareState.Decrypting -> stringResource(id = I18N.string.title_decrypting)
                    is ShareState.Downloading -> progress?.let {
                        stringResource(id = I18N.string.common_percent_downloaded,
                            progress.toPercentString(LocalContext.current.currentLocale))
                    } ?: stringResource(id = I18N.string.common_downloading)
                    else -> ""
                },
                style = ProtonTheme.typography.captionHint,
            )
        }
        LinearProgressIndicator(
            modifier = Modifier.padding(top = LinearProgressIndicatorSpacing),
            progress = progress?.value,
            backgroundColor = ProtonTheme.colors.interactionWeakNorm,
        )
    }
}

private val CircularProgressIndicatorSize = 8.dp
private val CircularProgressIndicatorStrokeWidth = 2.dp
private val LinearProgressIndicatorSpacing = 12.dp
internal val CancelButtonTestTag = "cancel_button"

@Preview
@Composable
fun PreviewDialogDownloadingNotStarted() {
    ProgressDialog(
        state = ShareState.Downloading(emptyFlow()),
        title = "Photo1.jpg",
        onDismiss = {},
    )
}

@Preview
@Composable
fun PreviewDialogDownloading() {
    ProgressDialog(
        state = ShareState.Downloading(flowOf(Percentage(50))),
        title = "Photo1.jpg",
        onDismiss = {},
    )
}

@Preview
@Composable
fun PreviewDialogDecrypting() {
    ProgressDialog(
        state = ShareState.Decrypting,
        title = "Photo1.jpg",
        onDismiss = {},
    )
}
