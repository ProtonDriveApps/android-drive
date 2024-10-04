/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.files.preview.presentation.component

import android.content.ContentResolver
import android.util.Base64
import android.util.Base64InputStream
import android.webkit.JavascriptInterface
import me.proton.core.drive.base.data.extension.exportToMediaStoreDownloads
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger

class BlobUrlDownloader(
    private val title: String,
    private val contentResolver: ContentResolver,
    private val onDownloadResult: (Result<String>) -> Unit,
) {

    @JavascriptInterface
    fun processBase64Content(
        base64Content: String,
        mimeType: String,
    ) = runCatching {
        CoreLogger.d(
            tag = LogTag.WEBVIEW,
            message ="BlobUrlDownloader processing base64 content, type $mimeType, size ${base64Content.length}",
        )
        val filename = fileExtension(mimeType)?.let { extension -> "$title.$extension" } ?: title
        base64Content
            .inputStream
            .exportToMediaStoreDownloads(
                contentResolver = contentResolver,
                filename = filename,
                mimeType = mimeType,
            )
        CoreLogger.d(
            tag = LogTag.WEBVIEW,
            message = "BlobUrlDownloader base64 content successfully exported, file \"$filename\"",
        )
        filename
    }.also { result ->
        onDownloadResult(result)
    }.getOrNull()

    private val String.inputStream: Base64InputStream get() = Base64InputStream(
        byteInputStream(),
        Base64.DEFAULT,
    )

    private fun fileExtension(mimeType: String): String? = when(mimeType) {
        MIME_TYPE_DOCX -> FILE_EXTENSION_DOCX
        MIME_TYPE_HTML -> FILE_EXTENSION_HTML
        MIME_TYPE_TXT -> FILE_EXTENSION_TXT
        MIME_TYPE_MD -> FILE_EXTENSION_MD
        MIME_TYPE_PDF -> FILE_EXTENSION_PDF
        else -> null
    }

    companion object {
        private const val MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        private const val MIME_TYPE_HTML = "text/html"
        private const val MIME_TYPE_TXT = "text/plain"
        private const val MIME_TYPE_MD = "text/markdown"
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val FILE_EXTENSION_DOCX = "docx"
        private const val FILE_EXTENSION_HTML = "html"
        private const val FILE_EXTENSION_TXT = "txt"
        private const val FILE_EXTENSION_MD = "md"
        private const val FILE_EXTENSION_PDF = "pdf"

        const val JS_INTERFACE_NAME = "Android"

        fun fetchBlobScript(
            blobUrl: String,
            mimeType: String
        ): String =
            """
                (async () => {
                  const response = await fetch('${blobUrl}', {
                    headers: {
                      'Content-Type': '${mimeType}',
                    }
                  });
                  const blob = await response.blob();

                  const reader = new FileReader();
                  reader.addEventListener('load', () => {
                    const base64 = reader.result.replace(/^data:.+;base64,/, '');
                    ${JS_INTERFACE_NAME}.processBase64Content(
                      base64,
                      '${mimeType}'
                    );
                  });
                  reader.readAsDataURL(blob);
                })();
            """.trimIndent()
    }
}
