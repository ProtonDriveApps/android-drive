/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.lifecycle.coroutineScope
import androidx.startup.Initializer
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger

class WebViewInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WebViewInitializerEntryPoint::class.java
            )
        ) {
            appLifecycleProvider.lifecycle.coroutineScope.launch(Dispatchers.Default) {
                runCatching {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
                        @Suppress("DEPRECATION")
                        WebViewCompat.startSafeBrowsing(context.applicationContext) { isSuccess ->
                            CoreLogger.d(LogTag.WEBVIEW, "Start safe browsing: $isSuccess")
                        }
                    }
                }.getOrNull(LogTag.WEBVIEW, "startSafeBrowsing failed")
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        LoggerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WebViewInitializerEntryPoint {
        val appLifecycleProvider: AppLifecycleProvider
    }
}
