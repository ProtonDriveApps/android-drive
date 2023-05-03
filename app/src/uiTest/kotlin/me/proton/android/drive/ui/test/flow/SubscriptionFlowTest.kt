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

package me.proton.android.drive.ui.test.flow

import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.SubscriptionRobot
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.extension.login
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.quark.data.User
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class SubscriptionFlowTest : BaseTest() {

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @Before
    fun preventHumanVerification() {
        quark.jailUnban()
    }

    @Before
    fun setPaymentMethods() {
        quark.setPaymentMethods()
    }

    @After
    fun resetPaymentMethods() {
        quark.setDefaultPaymentMethods()
    }

    @Test
    fun freeUserCanSeeAllPlans() {
        signInAndShowSubscriptionScreen(
            users.getUser { user -> user.plan == Plan.Free }
        )

        SubscriptionRobot
            .verify {
                robotDisplayed()
                currentPlanIsFree()
                hasUpgradeTitle()
                hasUpgradeToProtonUnlimited()
                hasUpgradeToDrivePlus()
            }
            .clickOnProtonUnlimited() // expand
            .verify { hasGetProtonUnlimitedButton() }
            .clickOnProtonUnlimited() // collapse
            .clickOnDrivePlus() // expand
            .verify { hasGetDrivePlusButton() }
            .clickOnDrivePlus() // collapse
    }

    @Test
    @Suppress("deprecation")
    fun payedUserCanSeeOnlyCurrentPlan() {
        signInAndShowSubscriptionScreen(
            users.getUser { user ->
                user.isPaid && user.plan in setOf(Plan.Professional, Plan.Unlimited, Plan.Visionary)
            }
        )

        SubscriptionRobot
            .verify {
                robotDisplayed()
                currentPlanIsNotFree()
                hasNotUpgradeTitle()
                //hasManageSubscriptionText() //TODO: uncomment this once CP-5766 is fixed
            }
    }

    @Test
    @Ignore("At the moment Mail Plus plan is shown instead of Proton Free")
    fun planWithoutDriveInScopeIsDisplayedAsFreePlan() {
        signInAndShowSubscriptionScreen(
            users.getUser { user -> user.plan == Plan.MailPlus }
        )

        SubscriptionRobot
            .verify {
                robotDisplayed()
                currentPlanIsFree()
            }
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
