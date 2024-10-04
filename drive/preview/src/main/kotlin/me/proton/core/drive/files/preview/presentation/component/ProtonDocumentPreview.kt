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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.startsWith
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ProtonDocumentPreview(
    modifier: Modifier = Modifier,
    onOpenInBrowser: () -> Unit,
) {
    PreviewPlaceholder(
        fileTypeCategory = FileTypeCategory.ProtonDoc,
        modifier = modifier,
        message = stringResource(id = I18N.string.preview_proton_doc_open_in_browser_button),
        onMessage = onOpenInBrowser,
    )
}

@Suppress("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ProtonDocumentPreview(
    uriString: String,
    title: String,
    host: String,
    appVersionHeader: String,
    modifier: Modifier = Modifier,
    onDownloadResult: (Result<String>) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, FileChooserParams?) -> Boolean,
) {
    val localContext = LocalContext.current
    val blobUrlDownloader = remember(title) {
        BlobUrlDownloader(
            title,
            localContext.contentResolver,
            onDownloadResult,
        )
    }
    val clientVersionProvider = remember(appVersionHeader) {
        ClientVersionProvider(appVersionHeader)
    }
    val localWebView = remember(uriString) { mutableStateOf<WebView?>(null) }
    val webViewBundle = rememberSaveable(uriString) { mutableStateOf<Bundle?>(null) }
    AndroidView(
        factory = { context ->
            localWebView.value ?: WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return request?.let {
                            if (request.url.host == "docs.$host" || request.url.host == "account.$host") {
                                super.shouldOverrideUrlLoading(view, request)
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, request.url)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    CoreLogger.d(LogTag.WEBVIEW, e, "Failed starting activity")
                                }
                                true
                            }
                        } ?: super.shouldOverrideUrlLoading(view, request)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage == null) return super.onConsoleMessage(consoleMessage)
                        val logger: (String, String) -> Unit = when (consoleMessage.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> CoreLogger::e
                            else -> CoreLogger::d
                        }
                        logger(LogTag.PROTON_DOCS, "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}")
                        return super.onConsoleMessage(consoleMessage)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        CoreLogger.d(LogTag.WEBVIEW, "WebChromeClient onShowFileChooser")
                        return onShowFileChooser(filePathCallback, fileChooserParams)
                    }
                }
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.domStorageEnabled = true
                addJavascriptInterface(blobUrlDownloader, BlobUrlDownloader.JS_INTERFACE_NAME)
                addJavascriptInterface(clientVersionProvider, ClientVersionProvider.JS_INTERFACE_NAME)
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    CoreLogger.d(
                        LogTag.WEBVIEW,
                        message = """
                            WebChromeClient onDownloadStarted url: $url, user-agent: $userAgent,
                            content-disposition: $contentDisposition, mime-type: $mimeType,
                            content-length: $contentLength
                        """.trimIndent(),
                    )
                    if (url.startsWith("blob:")) {
                        evaluateJavascript(
                            BlobUrlDownloader.fetchBlobScript(url, mimeType),
                            null,
                        )
                    } else {
                        onDownloadResult(
                            Result.failure(IllegalStateException("Url should start with \"blob:\", but was $url"))
                        )
                    }
                }
                webViewBundle.value?.let { bundle -> restoreState(bundle) } ?: loadUrl(uriString)
                localWebView.value = this
            }
        },
        onRelease = { webView ->
            val bundle = webViewBundle.value ?: bundleOf().also { bundle -> webViewBundle.value = bundle }
            webView.saveState(bundle)
        },
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    )
}

@Preview
@Composable
fun PreviewProtonDocumentPreview() {
    ProtonTheme {
        ProtonDocumentPreview {}
    }
}
