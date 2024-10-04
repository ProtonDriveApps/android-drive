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

package me.proton.android.drive.ui.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.ProtonDocsInsertImageOptionsViewModel
import me.proton.core.compose.activity.rememberCameraLauncher
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.component.bottomsheet.BottomSheetEntry
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.drive.base.presentation.component.rememberFilePickerLauncher
import me.proton.core.drive.base.presentation.extension.captureWithNotFound
import me.proton.core.drive.base.presentation.extension.launchWithNotFound
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ProtonDocsInsertImageOptions(
    saveResult: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ProtonDocsInsertImageOptionsViewModel>()
    val filePickerLauncher = rememberFilePickerLauncher(
        mimeTypes = arrayOf("image/*"),
        onFilesPicked = { filesUri ->
            viewModel.onAddFileResult(
                uriStrings = filesUri.map { fileUri -> fileUri.toString() },
                dismiss = dismiss
            )
        },
        modifyIntent = { intent -> intent.addCategory(Intent.CATEGORY_OPENABLE) }
    )
    val cameraLauncher = rememberCameraLauncher(
        onCaptured = { isTaken ->
            viewModel.onCameraResult(
                isTaken = isTaken,
                dismiss = dismiss,
            )
        }
    )
    val entries = remember {
        viewModel.entries(
            showFilePicker = { onNotFound -> filePickerLauncher.launchWithNotFound(onNotFound) },
            takeAPhoto = { uri, onNotFound -> cameraLauncher.captureWithNotFound(uri, onNotFound) },
            saveResult = saveResult,
            dismiss = dismiss,
        )
    }
    // TODO: see if there is a better fix for vkb appearing over options menu
    val keyboardController = LocalSoftwareKeyboardController.current
    keyboardController?.hide()
    ProtonDocsInsertImageOptions(
        entries = entries,
        modifier = modifier
            .navigationBarsPadding(),
    )
}

@Composable
fun ProtonDocsInsertImageOptions(
    entries: List<OptionEntry<Unit>>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            ProtonDocsInsertImageOptionsHeader(
                title = stringResource(id = I18N.string.proton_docs_insert_image_header_title),
            )
        },
        content = {
            entries.forEach { entry ->
                BottomSheetEntry(
                    icon = entry.icon,
                    title = stringResource(id = entry.label),
                    onClick = { entry.onClick(Unit) }
                )
            }
        },
    )
}

@Composable
internal fun ProtonDocsInsertImageOptionsHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = ProtonTheme.typography.defaultSmallStrongNorm,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Preview
@Composable
fun PreviewProtonDocsInsertImageOptions() {
    ProtonTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ProtonDocsInsertImageOptions(
                modifier = Modifier.background(ProtonTheme.colors.backgroundSecondary),
                entries = listOf(
                    object : OptionEntry<Unit> {
                        override val icon = CorePresentation.drawable.ic_proton_image
                        override val label = I18N.string.proton_docs_option_select_file
                        override val onClick: (Unit) -> Unit = {}
                    },
                    object : OptionEntry<Unit> {
                        override val icon = CorePresentation.drawable.ic_proton_camera
                        override val label = I18N.string.proton_docs_option_take_photo
                        override val onClick: (Unit) -> Unit = {}
                    },
                )
            )
        }
    }
}
