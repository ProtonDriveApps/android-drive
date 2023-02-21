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
package me.proton.core.drive.base.domain.extension

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

private val SUFFIX_REGEX = "(\\([0-9]+\\))?$".toRegex()
val FORBIDDEN_CHARS_REGEX = "[\\\\]|[/]|[\\u0000-\\u001F]|[\\u2000-\\u200F]|[\\u202E-\\u202F]".toRegex()

fun String.trimForbiddenChars(): String =
    FORBIDDEN_CHARS_REGEX.replace(
        input = this,
        replacement = ""
    )

fun String.avoidDuplicateFileName(): String {
    val extension = substringAfterLast('.', "")
    val name = substringBeforeLast('.').trimEnd()
    val suffix = SUFFIX_REGEX.find(name)?.groupValues
        ?.getOrNull(0)
        ?.takeIf { string -> string.isNotBlank() }
    val number = suffix?.let {
        suffix.subSequence(1, suffix.length - 1).toString().toIntOrNull()?.inc()
    } ?: 1
    val baseName = name.subSequence(0, name.length - (suffix?.length ?: 0)).trimEnd()
    val increasedName = "$baseName ($number)"
    return when {
        extension.isBlank() -> if (contains('.')) "$increasedName." else increasedName
        else -> "$increasedName.$extension"
    }
}

fun String.ellipsizeMiddle(maxLength: Int, ellipsize: String = "…"): String {
    require(maxLength > 2 * ellipsize.length) { "String max length must be greater then 2 times ellipsize size" }
    return if (length <= maxLength) {
        this
    } else {
        val half = (maxLength - ellipsize.length) / 2
        "${take(half)}$ellipsize${takeLast(maxLength - ellipsize.length - half)}"
    }
}

val String.extensionOrEmpty: String
    get() = substringAfterLast('.', "")

fun String.gzipCompress(): ByteArray = ByteArrayOutputStream().use { outputStream ->
    GZIPOutputStream(outputStream).bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(this) }
    outputStream.toByteArray()
}
