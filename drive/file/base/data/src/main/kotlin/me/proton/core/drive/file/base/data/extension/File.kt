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
package me.proton.core.drive.file.base.data.extension

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.drive.file.base.data.api.okhttp3.ProgressRequestBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

internal fun File.asProgressRequestBody(contentType: MediaType?, progress: MutableStateFlow<Long>) =
    ProgressRequestBody(asRequestBody(contentType), progress)

internal fun File.createBlockFormData(progress: MutableStateFlow<Long>): MultipartBody.Part =
    MultipartBody.Part.createFormData(
        name = "Block",
        filename = "blob",
        body = asProgressRequestBody("application/octet-stream".toMediaTypeOrNull(), progress)
    )
