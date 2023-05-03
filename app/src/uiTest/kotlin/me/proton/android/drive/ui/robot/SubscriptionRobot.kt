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

package me.proton.android.drive.ui.robot

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import me.proton.core.test.quark.data.Plan
import me.proton.test.fusion.Fusion.view
import me.proton.test.fusion.FusionConfig
import me.proton.test.fusion.ui.espresso.wrappers.EspressoAssertions
import org.hamcrest.CoreMatchers
import kotlin.time.Duration.Companion.seconds
import me.proton.core.plan.presentation.R as PlanPresentation

/** Corresponds to [me.proton.core.plan.presentation.ui.UpgradeActivity]. */
object SubscriptionRobot : Robot {
    private const val PLAN_DRIVE_PLUS = "Drive Plus" //TODO: Use quark Plan once DrivePlus is added there
    private val currentPlan = view
        .withId(PlanPresentation.id.planNameText)
        .hasAncestor(
            view.withId(PlanPresentation.id.currentPlan)
        )
    private val upgradePlansTitle = view.withId(PlanPresentation.id.plansTitle)
    private val protonUnlimited = view
        .withText(Plan.Unlimited.text)
        .hasAncestor(
            view.withId(PlanPresentation.id.planListRecyclerView)
        )
    private val drivePlus = view
        .withText(PLAN_DRIVE_PLUS)
        .hasAncestor(
            view.withId(PlanPresentation.id.planListRecyclerView)
        )
    private val manageSubscriptionText = view.withId(PlanPresentation.id.manageSubscriptionText)

    fun clickOnProtonUnlimited() = apply {
        protonUnlimited
            .scrollTo()
            .click()
    }

    fun clickOnDrivePlus() = apply {
        drivePlus
            .scrollTo()
            .click()
    }

    fun currentPlanIsFree() {
        currentPlan.checkContainsText(Plan.Free.text)
    }

    fun currentPlanIsNotFree() {
        currentPlan.checkContainsNotText(Plan.Free.text)
    }

    fun hasUpgradeTitle() {
        upgradePlansTitle.checkIsDisplayed()
    }

    fun hasNotUpgradeTitle() {
        upgradePlansTitle.checkIsNotDisplayed()
    }

    fun hasUpgradeToProtonUnlimited() {
        protonUnlimited.checkIsDisplayed()
    }

    fun hasGetProtonUnlimitedButton() {
        view
            .withText(
                FusionConfig.targetContext.getString(
                    PlanPresentation.string.plans_get_proton,
                    Plan.Unlimited.text,
                )
            )
            .scrollTo()
            .checkIsDisplayed()
    }

    fun hasUpgradeToDrivePlus() {
        drivePlus.checkIsDisplayed()
    }

    fun hasGetDrivePlusButton() {
        view
            .withText(
                FusionConfig.targetContext.getString(
                    PlanPresentation.string.plans_get_proton,
                    PLAN_DRIVE_PLUS,
                )
            )
            .scrollTo()
            .checkIsDisplayed()
    }

    fun hasManageSubscriptionText() {
        manageSubscriptionText.checkContainsText(PlanPresentation.string.plans_can_not_upgrade_from_mobile)
        manageSubscriptionText.checkIsDisplayed()
    }

    override fun robotDisplayed() {
        currentPlan.await(30.seconds) { checkIsDisplayed() }
    }

    private fun EspressoAssertions.checkContainsNotText(text: String) = apply {
        interaction.check(matches(ViewMatchers.withText(CoreMatchers.not(CoreMatchers.containsString(text)))))
    }
}
