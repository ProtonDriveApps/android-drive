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
package me.proton.core.drive.link.presentation.extension

import android.content.Context
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.lastModified
import me.proton.core.drive.base.presentation.extension.lastModifiedRelative
import me.proton.core.drive.link.domain.entity.BaseLink
import java.text.DateFormat

fun BaseLink.lastModified(format: Int = DateFormat.MEDIUM): String = lastModified.lastModified(format)

fun BaseLink.getSize(context: Context): String = size.asHumanReadableString(context)

fun BaseLink.lastModifiedRelative(context: Context, now: Long = System.currentTimeMillis()): CharSequence =
    lastModified.lastModifiedRelative(context, now)

fun BaseLink.getName(context: Context) = name.takeIf { parentId != null } ?: context.getString(R.string.title_my_files)
