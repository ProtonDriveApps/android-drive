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
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.command.jailUnban
import me.proton.core.test.quark.v2.command.systemEnv
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.deserialize
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class QuarkRule(
    private val envConfig: EnvironmentConfiguration,
    clientTimeout: Duration = 60.seconds
) : ExternalResource() {

    private var isDriveTest: Boolean = false
    private val quarkClient by lazy {
        clientTimeout
            .toJavaDuration()
            .let {
                OkHttpClient
                    .Builder()
                    .connectTimeout(it)
                    .readTimeout(it)
                    .writeTimeout(it)
                    .addInterceptor(HttpLoggingInterceptor { message ->
                        CoreLogger.d(QuarkCommand.quarkCommandTag, message)
                    }.apply { level = HttpLoggingInterceptor.Level.BODY })
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

    val quarkCommands: QuarkCommand
        get() = QuarkCommand(quarkClient)
            .baseUrl("https://${envConfig.host}/api/internal")
            .proxyToken(envConfig.proxyToken)

    override fun apply(base: Statement, description: Description): Statement {
        isDriveTest =
            description.isTest && !description.testClass.name.contains("account|subscription".toRegex())
        return super.apply(base, description)
    }

    override fun before() {
        // For /core/v4/keys that is deprecated
        var attempt = 0
        with(quarkCommands) {
            onResponse(
                condition = { code >= 300 },
                handlerBlock = {
                    if (code >= 500 && attempt < 5) {
                        attempt += 1
                        val time = 30 * attempt
                        CoreLogger.d(
                            QuarkCommand.quarkCommandTag,
                            "Waiting for env to be stable for: ${time}s ($attempt)"
                        )
                        Thread.sleep(time * 1000L)
                        systemEnv("PROHIBIT_DEPRECATED_DEV_CLIENT_ENV", "0")
                    } else {
                        error("Quark response failed after $attempt attempts with status code: $code:\n$message")
                    }
                },
            ).systemEnv("PROHIBIT_DEPRECATED_DEV_CLIENT_ENV", "0")
        }

        if (isDriveTest) {
            quarkCommands.jailUnban()
        }
    }
}
