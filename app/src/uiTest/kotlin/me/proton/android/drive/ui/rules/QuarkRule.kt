/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.ui.rules

import androidx.test.platform.app.InstrumentationRegistry
import me.proton.android.drive.test.BuildConfig
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.command.systemEnv
import me.proton.core.util.kotlin.deserialize
import okhttp3.OkHttpClient
import org.junit.rules.ExternalResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class QuarkRule(
    envConfig: EnvironmentConfiguration,
    clientTimeout: Duration = 60.seconds
) : ExternalResource() {

    private val quarkClient by lazy {
        clientTimeout
            .toJavaDuration()
            .let {
                OkHttpClient
                    .Builder()
                    .connectTimeout(it)
                    .readTimeout(it)
                    .writeTimeout(it)
                    .build()
            }
    }

    @Deprecated("quark is deprecated", replaceWith = ReplaceWith("quarkCommands"))
    val quark = Quark(
        host = envConfig.host,
        proxyToken = envConfig.proxyToken,
        InstrumentationRegistry.getInstrumentation().context
            .assets
            .open("internal_api.json")
            .bufferedReader()
            .use { it.readText() }
            .deserialize())

    val quarkCommands: QuarkCommand = QuarkCommand(quarkClient)
        .baseUrl("https://${envConfig.host}/api/internal")
        .proxyToken(envConfig.proxyToken)

    override fun before() {
        // For /core/v4/keys that is deprecated
        quarkCommands.systemEnv("PROHIBIT_DEPRECATED_DEV_CLIENT_ENV", "0")
    }
}
