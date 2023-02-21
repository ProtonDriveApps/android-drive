/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.base.presentation.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import me.proton.core.compose.activity.rememberLauncherWithInput

@Composable
fun rememberFilePickerLauncher(
    mimeTypes: Array<String> = arrayOf("*/*"),
    onFilesPicked: (List<Uri>) -> Unit,
    modifyIntent: ((Intent) -> Unit)? = null,
) = rememberLauncherWithInput(
    input = mimeTypes,
    contracts = object : ActivityResultContracts.OpenMultipleDocuments() {
        override fun createIntent(context: Context, input: Array<String>) = super.createIntent(context, input).apply {
            modifyIntent?.invoke(this)
        }
    },
    onResult = onFilesPicked,
)
