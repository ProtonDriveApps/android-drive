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
package me.proton.core.drive.base.presentation.entity

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FileTypeCategoryKtTest(
    private val mimeType: String,
    private val expectedCategory: FileTypeCategory
) {

    @Test
    fun toFileCategory() {
        // region Arrange
        // Nothing to do here...
        // endregion
        // region Act
        val category = mimeType.toFileTypeCategory()
        // endregion
        // region Assert
        assert(category == expectedCategory) { "$mimeType got mapped to $category but it doesn't equal $expectedCategory" }
        // endregion
    }

    companion object {
        // List from internal documentation: https://confluence.protontech.ch/x/O6LVAg
        @get:Parameterized.Parameters(name = "{0} should be mapped to {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("image/*", FileTypeCategory.Image),
            arrayOf("image/png", FileTypeCategory.Image),
            arrayOf("image/jpeg", FileTypeCategory.Image),
            arrayOf("audio/*", FileTypeCategory.Audio),
            arrayOf("audio/midi", FileTypeCategory.Audio),
            arrayOf("audio/ogg", FileTypeCategory.Audio),
            arrayOf("video/*", FileTypeCategory.Video),
            arrayOf("video/x-msvideo", FileTypeCategory.Video),
            arrayOf("video/ogg", FileTypeCategory.Video),
            arrayOf("application/x-abiword", FileTypeCategory.Doc),
            arrayOf("application/x-freearc", FileTypeCategory.Zip),
            arrayOf("application/vnd.amazon.ebook", FileTypeCategory.Unknown),
            arrayOf("application/octet-stream",	FileTypeCategory.Unknown),
            arrayOf("application/x-bzip", FileTypeCategory.Zip),
            arrayOf("application/x-bzip2", FileTypeCategory.Zip),
            arrayOf("application/x-cdf", FileTypeCategory.Audio),
            arrayOf("application/x-csh", FileTypeCategory.Text),
            arrayOf("text/*", FileTypeCategory.Text),
            arrayOf("text/css", FileTypeCategory.Text),
            arrayOf("text/csv", FileTypeCategory.Xls),
            arrayOf("application/msword", FileTypeCategory.Doc),
            arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileTypeCategory.Doc),
            arrayOf("application/vnd.ms-fontobject", FileTypeCategory.Unknown),
            arrayOf("application/epub+zip", FileTypeCategory.Unknown),
            arrayOf("application/gzip", FileTypeCategory.Zip),
            arrayOf("text/html", FileTypeCategory.Xml),
            arrayOf("text/calendar", FileTypeCategory.Calendar),
            arrayOf("application/java-archive", FileTypeCategory.Zip),
            arrayOf("text/javascript", FileTypeCategory.Text),
            arrayOf("application/json", FileTypeCategory.Text),
            arrayOf("application/ld+json", FileTypeCategory.Unknown),
            arrayOf("text/javascript", FileTypeCategory.Text),
            arrayOf("application/vnd.apple.installer+xml", FileTypeCategory.Zip),
            arrayOf("application/vnd.oasis.opendocument.presentation", FileTypeCategory.Ppt),
            arrayOf("application/vnd.oasis.opendocument.spreadsheet", FileTypeCategory.Xls),
            arrayOf("application/vnd.oasis.opendocument.text", FileTypeCategory.Doc),
            arrayOf("application/ogg", FileTypeCategory.Video),
            arrayOf("font/otf", FileTypeCategory.Unknown),
            arrayOf("application/pdf", FileTypeCategory.Pdf),
            arrayOf("application/x-httpd-php", FileTypeCategory.Text),
            arrayOf("application/vnd.ms-powerpoint", FileTypeCategory.Ppt),
            arrayOf("application/vnd.openxmlformats-officedocument.presentationml.presentation", FileTypeCategory.Ppt),
            arrayOf("application/vnd.rar", FileTypeCategory.Zip),
            arrayOf("application/rtf", FileTypeCategory.Doc),
            arrayOf("application/x-sh", FileTypeCategory.Text),
            arrayOf("application/x-shockwave-flash", FileTypeCategory.Unknown),
            arrayOf("application/x-tar", FileTypeCategory.Zip),
            arrayOf("font/ttf", FileTypeCategory.Unknown),
            arrayOf("text/plain", FileTypeCategory.Text),
            arrayOf("application/vnd.visio", FileTypeCategory.Image),
            arrayOf("font/woff", FileTypeCategory.Unknown),
            arrayOf("font/woff2", FileTypeCategory.Unknown),
            arrayOf("application/xhtml+xml", FileTypeCategory.Xml),
            arrayOf("application/vnd.ms-excel", FileTypeCategory.Xls),
            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileTypeCategory.Xls),
            arrayOf("application/xml", FileTypeCategory.Xml),
            arrayOf("text/xml", FileTypeCategory.Xml),
            arrayOf("application/vnd.mozilla.xul+xml", FileTypeCategory.Unknown),
            arrayOf("application/zip", FileTypeCategory.Zip),
            arrayOf("application/x-7z-compressed", FileTypeCategory.Zip),
            arrayOf("application/vnd.apple.pages", FileTypeCategory.Pages),
            arrayOf("application/vnd.apple.numbers", FileTypeCategory.Numbers),
            arrayOf("application/vnd.apple.keynote", FileTypeCategory.Keynote),
        )
    }
}
