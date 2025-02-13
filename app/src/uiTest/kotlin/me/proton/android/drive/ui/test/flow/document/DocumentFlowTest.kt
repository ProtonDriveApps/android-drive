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

package me.proton.android.drive.ui.test.flow.document

import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.UriMatchers.hasHost
import androidx.test.espresso.intent.matcher.UriMatchers.hasParamWithName
import androidx.test.espresso.intent.matcher.UriMatchers.hasPath
import androidx.test.espresso.intent.matcher.UriMatchers.hasScheme
import androidx.test.espresso.intent.rule.IntentsRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.MoveToFolderRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState.Downloaded
import me.proton.core.test.rule.annotation.PrepareUser
import org.hamcrest.Matchers.allOf
import org.junit.Test

@HiltAndroidTest
class DocumentFlowTest : BaseTest() {

    private val expectedIntent = allOf(
        hasAction(Intent.ACTION_VIEW),
        hasData(
            allOf(
                hasScheme("https"),
                hasHost("docs.${envConfig.host}"),
                hasPath("/doc"),
                hasParamWithName("volumeId"),
                hasParamWithName("linkId"),
                hasParamWithName("email"),
            )
        ),
    )

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 8)
    fun openDocumentFromFiles() {
        val file = "doc9.protondoc"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed(file)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 8)
    fun openDocumentFromOffline() {
        val file = "doc1.protondoc"
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline(FilesTabRobot)
            .verify {
                itemIsDisplayed(folder, downloadState = Downloaded)
            }
            .openSidebarBySwipe()
            .clickOffline()
            .clickOnFolder(folder)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed(file)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 8)
    fun previewDocumentFromFiles() {

        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(0, null));

        val file = "doc9.protondoc"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .clickOnContextualButton()
            .clickOpenInBrowserButton()
            .verify {
                intended(expectedIntent)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 8)
    fun openDocumentFromMenu() {

        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(0, null));

        val file = "doc9.protondoc"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickOpenInBrowserButton()
            .verify {
                intended(expectedIntent)
            }
    }
}
