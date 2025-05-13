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

package me.proton.android.drive.extension

import android.content.Context
import me.proton.android.drive.lock.domain.exception.LockException
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.exception.DriveException
import me.proton.core.drive.link.domain.exception.LinksResultException
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.exception.ShareException
import me.proton.android.drive.lock.presentation.extension.getDefaultMessage as lockGetDefaultMessage
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.drive.link.presentation.extension.getDefaultMessage as linkGetDefaultMessage

fun DriveException.getDefaultMessage(context: Context): String = when (val exception = this) {
    is ShareException.ShareLocked -> when (exception.shareType) {
        Share.Type.MAIN -> context.getString(I18N.string.error_main_share_locked)
        else -> context.getString(I18N.string.error_share_locked)
    }
    is ShareException.ShareNotFound -> when (exception.shareType) {
        Share.Type.MAIN -> context.getString(I18N.string.error_main_share_not_found)
        else -> context.getString(I18N.string.error_share_not_found)
    }
    is ShareException.CreatingShareNotAllowed -> when (exception.shareType) {
        Share.Type.PHOTO -> context.getString(I18N.string.error_creating_photo_share_not_allowed)
        else -> context.getString(I18N.string.error_creating_share_not_allowed)
    }
    is LockException -> exception.lockGetDefaultMessage(context)
    is LinksResultException -> exception.linkGetDefaultMessage(context)
    else -> error("Default message for exception is missing")
}

fun DriveException.log(
    tag: String,
    message: String? = null,
    level: LoggerLevel? = null,
): DriveException = also {
    level.log(tag, this, message)
}
