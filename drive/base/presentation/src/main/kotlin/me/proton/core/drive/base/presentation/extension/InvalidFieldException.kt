/*
 * Copyright (c) 2023 Proton AG.
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
package me.proton.core.drive.base.presentation.extension

import android.content.Context
import me.proton.core.drive.base.domain.exception.InvalidFieldException
import me.proton.core.util.kotlin.CoreLogger

@Suppress("UNUSED_PARAMETER")
fun InvalidFieldException.getDefaultMessage(context: Context): String = message

fun InvalidFieldException.log(
    tag: String, message: String = this.message
): InvalidFieldException = also {
    CoreLogger.d(tag, this, message)
}
