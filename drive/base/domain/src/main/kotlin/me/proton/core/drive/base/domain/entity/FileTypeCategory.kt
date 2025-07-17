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
package me.proton.core.drive.base.domain.entity

import java.util.Locale

enum class FileTypeCategory {
    Audio,
    Calendar,
    Doc,
    Image,
    Keynote,
    Numbers,
    Pages,
    Pdf,
    Ppt,
    ProtonDoc,
    ProtonSheet,
    Text,
    TrustedKey,
    Unknown,
    Video,
    Xls,
    Xml,
    Zip,
}

fun String.toFileTypeCategory(): FileTypeCategory = with(lowercase(Locale.getDefault())) {
    when {
        startsWith("image/") || this in imageMimeTypes -> FileTypeCategory.Image
        startsWith("audio/") || this in audioMimeTypes -> FileTypeCategory.Audio
        startsWith("video/") || this in videoMimeTypes -> FileTypeCategory.Video
        this == "application/pdf" -> FileTypeCategory.Pdf
        this == "text/calendar" -> FileTypeCategory.Calendar
        this in zipMimeTypes -> FileTypeCategory.Zip
        this in docMimeTypes -> FileTypeCategory.Doc
        this in xlsMimeTypes -> FileTypeCategory.Xls
        this in pptMimeTypes -> FileTypeCategory.Ppt
        this in protonDocMimeTypes -> FileTypeCategory.ProtonDoc
        this in protonSheetMimeTypes -> FileTypeCategory.ProtonSheet
        this == "application/vnd.apple.keynote" -> FileTypeCategory.Keynote
        this == "application/vnd.apple.pages" -> FileTypeCategory.Pages
        this == "application/vnd.apple.numbers" -> FileTypeCategory.Numbers
        xmlMimeTypes.contains(this) -> FileTypeCategory.Xml
        this == "application/pgp-keys" -> FileTypeCategory.TrustedKey
        startsWith("text/") || this in textMimeTypes -> FileTypeCategory.Text
        else -> FileTypeCategory.Unknown
    }
}

// region MimeType lists
private val audioMimeTypes = listOf(
    "application/x-cdf"
)

private val docMimeTypes = listOf(
    "application/msword",
    "application/rtf",
    "application/vnd.ms-word.document.macroEnabled.12",
    "application/vnd.ms-word.template.macroEnabled.12",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
    "application/x-abiword",
)

private val imageMimeTypes = listOf(
    "application/vnd.visio"
)

private val pptMimeTypes = listOf(
    "application/vnd.ms-powerpoint",
    "application/vnd.ms-powerpoint.addin.macroEnabled.12",
    "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
    "application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
    "application/vnd.ms-powerpoint.template.macroEnabled.12",
    "application/vnd.oasis.opendocument.presentation",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
    "application/vnd.openxmlformats-officedocument.presentationml.template",
)

private val textMimeTypes = listOf(
    "application/json",
    "application/x-csh",
    "application/x-httpd-php",
    "application/x-sh",
)

private val videoMimeTypes = listOf(
    "application/ogg"
)

private val xlsMimeTypes = listOf(
    "application/vnd.ms-excel",
    "application/vnd.ms-excel.addin.macroEnabled.12",
    "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
    "application/vnd.ms-excel.sheet.macroEnabled.12",
    "application/vnd.ms-excel.template.macroEnabled.12",
    "application/vnd.oasis.opendocument.spreadsheet",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
    "text/csv",
)

private val xmlMimeTypes = listOf(
    "application/x-xliff+xml",
    "application/xhtml+xml",
    "application/xml",
    "text/html",
    "text/xml",
)

private val zipMimeTypes = listOf(
    "application/gzip",
    "application/java-archive",
    "application/vnd.apple.installer+xml",
    "application/vnd.rar",
    "application/x-7z-compressed",
    "application/x-bzip",
    "application/x-bzip2",
    "application/x-freearc",
    "application/x-rar-compressed",
    "application/x-tar",
    "application/x-zip-compressed",
    "application/zip",
    "multipart/x-zip",
)

private val protonDocMimeTypes = listOf(
    "application/vnd.proton.doc"
)

private val protonSheetMimeTypes = listOf(
    "application/vnd.proton.sheet"
)
// endregion
