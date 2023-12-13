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

package me.proton.android.drive.photos.data.extension

import android.content.Context
import me.proton.core.drive.share.domain.exception.ShareException
import me.proton.core.drive.base.data.extension.getDefaultMessage as baseGetDefaultMessage
import me.proton.core.drive.i18n.R as I18N

fun Throwable.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
): String = when (this) {
    is RuntimeException -> when (cause) {
        is ShareException.CreatingShareNotAllowed -> context.getString(
            I18N.string.error_creating_photo_share_not_allowed
        )
        else -> baseGetDefaultMessage(context, useExceptionMessage)
    }
    else -> baseGetDefaultMessage(context, useExceptionMessage)
}
