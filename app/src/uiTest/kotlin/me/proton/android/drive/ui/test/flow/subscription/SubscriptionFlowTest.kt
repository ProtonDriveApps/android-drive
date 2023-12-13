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

package me.proton.android.drive.ui.test.flow.subscription

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.plan.test.MinimalSubscriptionTests
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.quark.data.User
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SubscriptionFlowTest : BaseTest(), MinimalSubscriptionTests {

    private val freeUser = User()
    private val paidUser = User(plan = Plan.MailPlus)

    override val quark: Quark = quarkRule.quark
    override val users: User.Users = User.Users(
        listOf(freeUser, paidUser)
    )

    override fun startSubscription(user: User) {
        quark.seedNewSubscriber(user)
        signInAndShowSubscriptionScreen(user)
    }

    private fun signInAndShowSubscriptionScreen(user: User) {
        AddAccountRobot
            .clickSignIn()
            .login(user)

        FilesTabRobot
            .verify { robotDisplayed() }
            .openSidebarBySwipe()
            .verify { robotDisplayed() }
            .clickSubscription()
    }
}
