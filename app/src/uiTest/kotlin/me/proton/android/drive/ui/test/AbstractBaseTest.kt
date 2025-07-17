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

package me.proton.android.drive.ui.test

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.robot.Robot
import me.proton.android.drive.ui.rules.DriveTestDataRule
import me.proton.android.drive.ui.rules.QuarkRule
import me.proton.android.drive.utils.screenshot
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
import me.proton.core.accountrecovery.domain.IsAccountRecoveryResetEnabled
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.core.configuration.ContentResolverConfigManager
import me.proton.core.domain.entity.UserId
import me.proton.core.notification.domain.usecase.IsNotificationsEnabled
import me.proton.core.test.rule.di.TestEnvironmentConfigModule.provideEnvironmentConfiguration
import me.proton.test.fusion.FusionConfig
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
abstract class AbstractBaseTest {

    val envConfig =
        provideEnvironmentConfiguration(ContentResolverConfigManager(targetContext))

    @get:Rule(order = 0)
    val quarkRule = QuarkRule(envConfig)

    @get:Rule(order = 1)
    val driveTestDataRule = DriveTestDataRule()

    abstract val mainUserId: UserId

    internal open val shouldShowOnboardingAfterLogin get() = false
    internal open val shouldShowWhatsNewAfterLogin get() = false
    internal open val shouldShowRatingBoosterAfterLogin get() = false
    internal open val shouldShowDrivePlusPromoAfterLogin get() = false
    internal open val shouldShowDriveLitePromoAfterLogin get() = false

    internal fun setOverlaysDisplayStateAfterLogin(userId: UserId) {
        CoroutineScope(Dispatchers.Main).launch {
            if (!shouldShowOnboardingAfterLogin) {
                Companion.uiTestHelper.doNotShowOnboardingAfterLogin()
            }
            if (!shouldShowWhatsNewAfterLogin) {
                Companion.uiTestHelper.doNotShowWhatsNewAfterLogin()
            }
            if (!shouldShowRatingBoosterAfterLogin) {
                Companion.uiTestHelper.doNotShowRatingBoosterAfterLogin()
            }
            if (!shouldShowDrivePlusPromoAfterLogin) {
                Companion.uiTestHelper.doNotShowDrivePlusPromoAfterLogin(userId)
            }
            if (!shouldShowDriveLitePromoAfterLogin) {
                Companion.uiTestHelper.doNotShowDriveLitePromoAfterLogin(userId)
            }
        }
    }

    internal val baseTestPermissions =
        listOf(
            Manifest.permission.CAMERA,
        ) + when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                listOf(Manifest.permission.POST_NOTIFICATIONS)

            else -> emptyList()
        }

    internal val externalStoragePermissions =
        baseTestPermissions + listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    val uiTestHelper by lazy { uiTestEntryPoint.uiTestHelper }

    internal val photosTestPermissions =
        externalStoragePermissions + when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )

            else -> emptyList()
        }

    @Inject
    internal lateinit var isAccountRecoveryEnabled: IsAccountRecoveryEnabled

    @Inject
    internal lateinit var isAccountRecoveryResetEnabled: IsAccountRecoveryResetEnabled

    @Inject
    internal lateinit var isNotificationsEnabled: IsNotificationsEnabled

    @Before
    fun setupMocks() {
        every { isAccountRecoveryEnabled(any()) } returns true
        every { isNotificationsEnabled(any<UserId>()) } returns true
    }

    fun <T : Robot> T.verify(block: T.() -> Any): T =
        apply { block() }

    companion object {

        fun configureFusion() {
            FusionConfig.Compose.useUnmergedTree.set(true)
            FusionConfig.Compose.onFailure = { screenshot() }
            FusionConfig.Compose.waitTimeout.set(60.seconds)
            FusionConfig.Compose.assertTimeout.set(60.seconds)
            FusionConfig.Compose.shouldPrintHierarchyOnFailure.set(true)
            me.proton.core.test.android.instrumented.FusionConfig.Compose.shouldPrintHierarchyOnFailure = true
            me.proton.core.test.android.instrumented.FusionConfig.Compose.shouldPrintToLog = true
            FusionConfig.Espresso.onFailure = { screenshot() }
            FusionConfig.Espresso.waitTimeout.set(60.seconds)
            FusionConfig.Espresso.assertTimeout.set(60.seconds)
            screenshotCounter.set(0)
        }

        private val protonTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(targetContext, ProtonTestEntryPoint::class.java)
        }

        private val uiTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(targetContext, UiTestEntryPoint::class.java)
        }

        val loginTestHelper by lazy { protonTestEntryPoint.loginTestHelper }
        val uiTestHelper by lazy { uiTestEntryPoint.uiTestHelper }

        val testName = TestName()
        val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotLocation
            get() =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    "/sdcard/Pictures/Screenshots/${testName.methodName}/"
                } else {
                    "${targetContext.getExternalFilesDir(null)?.path}/Screenshots/${testName.methodName}/"
                }
        val screenshotCounter = AtomicInteger(0)
    }
}

