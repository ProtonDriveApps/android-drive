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

package me.proton.android.drive.cross.platform

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.loginTestHelper
import me.proton.core.domain.entity.UserId
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ConfigurationRule : TestRule {

    lateinit var configuration: ConfigurationRoot
    lateinit var mainUserId: UserId

    fun getArgString(key: String): String {
        return configuration.info.args.getValue(key).jsonPrimitive.content
    }

    fun getArgStringList(key: String): List<String> =
        configuration.info.args.getValue(key).jsonArray.map {
            it.jsonPrimitive.content
        }

    override fun apply(base: Statement, description: Description): Statement {
        val configData = requireNotNull(
            InstrumentationRegistry.getArguments().getString("androidConfig")
        ) { "androidConfig arguments not found" }
        val configurations = json.decodeFromString<List<ConfigurationRoot>>(configData)
        val testId = description.getAnnotation(TestId::class.java).value
        val testConfiguration = configurations.firstOrNull { configuration ->
            configuration.info.id == testId
        }

        return if (testConfiguration == null) {
            object : Statement() {
                override fun evaluate() {
                    println("Ignoring test: ${description.displayName}")
                }
            }
        } else {
            configuration = testConfiguration
            object : Statement() {
                override fun evaluate() {
                    configuration.user.run {
                        mainUserId = loginTestHelper.login(username, password).userId
                    }

                    base.evaluate()

                    loginTestHelper.logoutAll()
                }
            }
        }
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
        }
    }

}
