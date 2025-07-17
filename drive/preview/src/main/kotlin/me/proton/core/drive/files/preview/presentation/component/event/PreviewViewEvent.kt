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

package me.proton.core.drive.files.preview.presentation.component.event

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams

interface PreviewViewEvent {
    val onTopAppBarNavigation: () -> Unit
    val onMoreOptions: () -> Unit
    val onSingleTap: () -> Unit
    val onRenderFailed: (Throwable, Any) -> Unit
    val mediaControllerVisibility: (Boolean) -> Unit
    val onOpenInBrowser: () -> Unit
    val onProtonDocsDownloadResult: (Result<String>) -> Unit
    val onProtonDocsShowFileChooser: (ValueCallback<Array<Uri>>?, FileChooserParams?) -> Boolean
    val onWebViewRelease: (String) -> Unit
}
