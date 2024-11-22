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

package me.proton.android.drive.ui.test.flow.settings

import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.extension.respondWithFile
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ANDROID_USER_LOG_DISABLED
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LogFlowTest : AuthenticatedBaseTest() {

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @get:Rule
    val externalFilesRule = ExternalFilesRule()

    @Test
    fun downloadLog() {
        val filename = "log.zip"
        Intents.intending(
            allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType("application/zip"),
                hasExtra(Intent.EXTRA_TITLE, filename),
            )
        ).respondWithFile {
            externalFilesRule.createEmptyFile(filename)
        }

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickToShowLog()
            .clickDownload()
            .verify {
                assertLogExported()
                val file = externalFilesRule.getFile(filename)
                check(file.exists()) { "File ${file.name} does not exist" }
                check(file.length() > 0) { "File ${file.name} is empty" }
            }
    }

    @Test
    @FeatureFlag(DRIVE_ANDROID_USER_LOG_DISABLED, ENABLED)
    fun killSwitch() {
        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .verify {
                assertUserLogIsNotDisplayed()
            }
    }
}
