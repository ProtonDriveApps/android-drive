/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.test.flow.account

import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import me.proton.android.drive.ui.robot.SharedTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.accountmanager.data.AccountStateHandler
import me.proton.core.accountrecovery.dagger.CoreAccountRecoveryFeaturesModule
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
import me.proton.core.accountrecovery.test.MinimalAccountRecoveryNotificationTest
import me.proton.core.auth.test.usecase.WaitForPrimaryAccount
import me.proton.core.eventmanager.domain.EventManagerProvider
import me.proton.core.eventmanager.domain.repository.EventMetadataRepository
import me.proton.core.network.data.ApiProvider
import me.proton.core.notification.dagger.CoreNotificationFeaturesModule
import me.proton.core.notification.domain.repository.NotificationRepository
import me.proton.core.notification.domain.usecase.IsNotificationsEnabled
import me.proton.core.test.quark.Quark
import org.junit.Rule
import org.junit.rules.ExternalResource
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(
    CoreAccountRecoveryFeaturesModule::class,
    CoreNotificationFeaturesModule::class,
)
class AccountRecoveryFlowTest : BaseTest(), MinimalAccountRecoveryNotificationTest {
    @get:Rule(order = 1)
    val initFeaturesRule = object : ExternalResource() {
        override fun before() {
            every { isAccountRecoveryEnabled(any()) } returns true
            every { isNotificationsEnabled(any()) } returns true
        }
    }

    @Inject
    internal lateinit var isAccountRecoveryEnabled: IsAccountRecoveryEnabled

    @Inject
    internal lateinit var isNotificationsEnabled: IsNotificationsEnabled

    @Inject
    override lateinit var accountStateHandler: AccountStateHandler

    @Inject
    override lateinit var apiProvider: ApiProvider

    @Inject
    override lateinit var eventManagerProvider: EventManagerProvider

    @Inject
    override lateinit var eventMetadataRepository: EventMetadataRepository

    @Inject
    override lateinit var notificationRepository: NotificationRepository

    @Inject
    override lateinit var waitForPrimaryAccount: WaitForPrimaryAccount

    override val quark: Quark = quarkRule.quark

    init { initFusion(fusionComposeRule) }

    override fun verifyAfterLogin() {
        SharedTabRobot.homeScreenDisplayed()
    }
}
