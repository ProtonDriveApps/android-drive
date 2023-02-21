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
package me.proton.core.drive.upload.data.extension

import android.content.Context
import me.proton.core.drive.base.presentation.extension.getDefaultMessage
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.upload.data.exception.UploadCleanupException

internal fun UploadCleanupException.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
): String = error.getDefaultMessage(context, useExceptionMessage)

internal fun UploadCleanupException.log(tag: String, message: String? = null): UploadCleanupException = also {
    message?.let { error.log(tag, message) } ?: error.log(tag)
}
