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

import kotlinx.coroutines.runBlocking
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.loginTestHelper
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.quark
import me.proton.core.test.android.instrumented.ProtonTest.Companion.testTag
import me.proton.core.test.quark.data.User
import me.proton.core.util.kotlin.CoreLogger
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class UserLoginRule(
    private val testUser: User = User(),
    private val shouldSeedUser: Boolean = true
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        runBlocking {
            if (shouldSeedUser) {
                if (testUser.isPaid)
                    quark.seedNewSubscriber(testUser)
                else {
                    try {
                        quark.userCreate(testUser)
                    } catch (e: java.lang.Exception) {
                        // FIXME: Bug in Core quark.userCreate
                        CoreLogger.e(testTag, e)
                    }
                }
            }

            if (testUser.dataSetScenario.isNotEmpty()) {
                quark.populateUserWithData(testUser)
            }

            loginTestHelper.login(testUser.name, testUser.password)
        }
        return base
    }
}
