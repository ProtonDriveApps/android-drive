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

package me.proton.core.drive.files.preview.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ProtonDocumentPreview(
    modifier: Modifier = Modifier,
    onOpenInBrowser: () -> Unit,
) {
    PreviewPlaceholder(
        fileTypeCategory = FileTypeCategory.ProtonDoc,
        modifier = modifier,
        message = stringResource(id = I18N.string.preview_proton_doc_open_in_browser_button),
        onMessage = onOpenInBrowser,
    )
}

@Preview
@Composable
fun PreviewProtonDocumentPreview() {
    ProtonTheme {
        ProtonDocumentPreview {}
    }
}
