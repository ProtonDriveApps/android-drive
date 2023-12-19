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
import me.proton.android.drive.ui.annotation.Quota
import me.proton.android.drive.ui.extension.populate
import me.proton.android.drive.ui.extension.quotaSetUsedSpace
import me.proton.android.drive.ui.extension.volumeCreate
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.loginTestHelper
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.command.seedNewSubscriber
import me.proton.core.test.quark.v2.command.userCreate
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.math.roundToInt

class UserLoginRule(
    private val testUser: User,
    private val isDevice: Boolean = false,
    private val isPhotos: Boolean = false,
    private val quarkCommands: QuarkCommand,
) : TestWatcher() {
    override fun starting(description: Description) {
        runBlocking {
            loginTestHelper.logoutAll()

            val scenarioAnnotation = description.getAnnotation(Scenario::class.java)
            val quotaAnnotation = description.getAnnotation(Quota::class.java)

            val scenarioUser = scenarioAnnotation
                ?.let {
                    testUser.copy(dataSetScenario = it.value.toString())
                } ?: testUser

            val device = scenarioAnnotation?.isDevice ?: isDevice
            val photos = scenarioAnnotation?.isPhotos ?: isPhotos

            if (testUser.isPaid)
                quarkCommands.seedNewSubscriber(testUser)
            else
                quarkCommands.userCreate(testUser)

            quotaAnnotation?.let {
                quarkCommands.volumeCreate(testUser, "${it.value}${it.unit}")

                if (it.percentageFull in 1..100) {
                    val usedSpace = (it.value.toDouble() * it.percentageFull / 100).roundToInt()
                    quarkCommands.quotaSetUsedSpace(testUser, "${usedSpace}${it.unit}")
                }
            }

            if (scenarioUser.dataSetScenario.let { it.isNotEmpty() && it != "0" }) {
                quarkCommands.populate(scenarioUser, device, photos)
            }
        }

        try {
            loginTestHelper.login(testUser.name, testUser.password)
        } catch (ex: IllegalStateException) {
            val message =
                "Possibly incorrect rule order, make sure you use @get:Rule(order = 1) for UserLoginRule\n${ex.message}"
            throw IllegalStateException(message)
        }
    }

    override fun finished(description: Description) {
        loginTestHelper.logoutAll()
    }
}
