/*
 * Copyright (c) 2023-2024 Proton AG.
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
import me.proton.android.drive.repository.TestFeatureFlagRepositoryImpl
import me.proton.android.drive.ui.annotation.Quota
import me.proton.android.drive.ui.extension.getFeatureFlagAnnotations
import me.proton.android.drive.ui.extension.populate
import me.proton.android.drive.ui.extension.quotaSetUsedSpace
import me.proton.android.drive.ui.extension.volumeCreate
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.loginTestHelper
import me.proton.core.domain.entity.UserId
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.command.seedNewSubscriber
import me.proton.core.test.quark.v2.command.userCreate
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.math.roundToInt

class UserLoginRule(
    var testUser: User,
    private val isDevice: Boolean = false,
    private val isPhotos: Boolean = false,
    private val quarkCommands: QuarkCommand,
) : TestWatcher() {

    var userId: UserId? = null
    var decryptedUserId: Long = -1
    var sharingUser = User()

    override fun starting(description: Description) {
        runBlocking {
            loginTestHelper.logoutAll()

            val userPlanAnnotation = description.getAnnotation(UserPlan::class.java)
            val scenarioAnnotation = description.getAnnotation(Scenario::class.java)
            val quotaAnnotation = description.getAnnotation(Quota::class.java)
            val featureFlagAnnotations = description.getFeatureFlagAnnotations()

            val user = testUser
                .updateWith(userPlanAnnotation)
                .updateWith(scenarioAnnotation)

            val device = scenarioAnnotation?.isDevice ?: isDevice
            val photos = scenarioAnnotation?.isPhotos ?: isPhotos
            val loginWithSharingUser = scenarioAnnotation?.loginWithSharingUser ?: false
            val withSharingUser = (scenarioAnnotation?.withSharingUser ?: false) || loginWithSharingUser

            if (user.isPaid)
                quarkCommands.seedNewSubscriber(user)
            else
                quarkCommands.userCreate(user).also { response ->
                    testUser = testUser.copy(email = response.email.orEmpty())
                    userId = response.userId.let(::UserId)
                    decryptedUserId = response.decryptedUserId
                }

            if (withSharingUser) {
                quarkCommands.userCreate(sharingUser).also { response ->
                    sharingUser = sharingUser.copy(
                        email = response.email.orEmpty()
                    )
                }
            }

            quotaAnnotation?.let {
                quarkCommands.volumeCreate(user)

                if (it.percentageFull in 1..100) {
                    val usedSpace = (it.value.toDouble() * it.percentageFull / 100).roundToInt()
                    quarkCommands.quotaSetUsedSpace(
                        userId = decryptedUserId.toString(),
                        usedSpace = "${usedSpace}${it.unit}",
                        product = it.product,
                    )
                }
            }

            if (user.dataSetScenario.let { it.isNotEmpty() && it != "0" }) {
                quarkCommands.populate(user, device, photos, sharingUser.takeIf { withSharingUser })
            }

            featureFlagAnnotations.map { featureFlagAnnotation ->
                TestFeatureFlagRepositoryImpl.flags[featureFlagAnnotation.id] =
                    featureFlagAnnotation.state
            }

            try {
                if (loginWithSharingUser) {
                    loginTestHelper.login(sharingUser.name, sharingUser.password)
                } else {
                    loginTestHelper.login(testUser.name, testUser.password)
                }
            } catch (ex: IllegalStateException) {
                val message =
                    "Possibly incorrect rule order, make sure you use @get:Rule(order = 1) for UserLoginRule\n${ex.message}"
                throw IllegalStateException(message)
            }
        }
    }

    override fun finished(description: Description) {
        TestFeatureFlagRepositoryImpl.flags.clear()
        loginTestHelper.logoutAll()
    }
}

private fun User.updateWith(
    userAnnotation: UserPlan?,
): User = updateWith(userAnnotation) { annotation ->
    copy(plan = annotation.value)
}

private fun User.updateWith(
    scenarioAnnotation: Scenario?,
): User = updateWith(scenarioAnnotation) { annotation ->
    copy(dataSetScenario = annotation.value.toString())
}

private fun <T> User.updateWith(value: T?, block: User.(T) -> User) = value?.let {
    block(it)
} ?: this
