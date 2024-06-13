/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.log.presentation.component.Log
import me.proton.core.drive.log.presentation.viewmodel.LogViewModel
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.rememberCreateDocumentLauncher
import me.proton.core.drive.base.presentation.extension.launchWithNotFound
import me.proton.core.drive.log.presentation.entity.LogItem
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun LogScreen(
    navigateToLogOptions: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<LogViewModel>()
    val logs = rememberFlowWithLifecycle(flow = viewModel.logs)
    val createDocumentLauncher = rememberCreateDocumentLauncher(
        onDocumentCreated = { documentUri ->
            documentUri?.let {
                viewModel.onCreateLogResult(documentUri)
            }
        },
        mimeType = viewModel.mimeType,
    )
    val viewEvent = viewModel.viewEvent(
        navigateToLogOptions = navigateToLogOptions,
        showCreateLogPicker = { filename, onNotFound ->
            createDocumentLauncher.launchWithNotFound(filename, onNotFound)
        },
    )
    LogScreen(
        title = stringResource(id = I18N.string.log_title),
        logs = logs,
        onSave = viewEvent.onSave,
        onMoreOptions = viewEvent.onMoreOptions,
        navigateBack = navigateBack,
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize(),
    )
}

@Composable
private fun LogScreen(
    title: String,
    logs: Flow<PagingData<LogItem>>,
    modifier: Modifier = Modifier,
    onSave: () -> Unit,
    onMoreOptions: () -> Unit,
    navigateBack: () -> Unit,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding(),
    ) {
        TopAppBar(
            title = title,
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
        ) {
            ActionButton(
                icon = CorePresentation.drawable.ic_proton_arrow_up_from_square,
                contentDescription = I18N.string.content_description_save_log_file,
                onClick = onSave,
            )
            ActionButton(
                icon = CorePresentation.drawable.ic_proton_three_dots_vertical,
                contentDescription = I18N.string.content_description_more_log_options,
                onClick = onMoreOptions,
            )
        }
        Log(
            logs = logs,
        )
    }
}
