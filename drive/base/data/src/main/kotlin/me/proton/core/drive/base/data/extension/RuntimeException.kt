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
package me.proton.core.drive.base.data.extension

import android.content.Context
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.i18n.R as I18N

fun RuntimeException.getDefaultMessage(context: Context): String =
    context.getString(I18N.string.common_error_internal)

fun RuntimeException.log(
    tag: String,
    message: String? = null,
    level: LoggerLevel? = LoggerLevel.ERROR,
): RuntimeException = also {
    level.log(tag, this, message)
}
