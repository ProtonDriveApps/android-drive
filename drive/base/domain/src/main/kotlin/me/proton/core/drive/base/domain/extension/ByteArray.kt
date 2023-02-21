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
package me.proton.core.drive.base.domain.extension

import me.proton.core.drive.base.domain.util.coRunCatching
import java.util.zip.GZIPInputStream

fun ByteArray.gzipDecompress(): Result<String> = coRunCatching {
    GZIPInputStream(inputStream()).bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
}

fun ByteArray.toHex(): String = joinToString("") { byte -> "%02X".format(byte) }
