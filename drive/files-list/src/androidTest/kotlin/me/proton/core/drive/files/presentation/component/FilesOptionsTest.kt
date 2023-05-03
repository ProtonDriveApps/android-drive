/*
 * Copyright (c) 2021-2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.files.presentation.component

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.files.FilesOptions
import me.proton.core.drive.files.presentation.component.folder.ParentFolderOptions
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@RunWith(AndroidJUnit4::class)
class FilesOptionsTest {

    private val lastModified =  TimestampS(1_603_631_640L) // October 25 2020, 14:14
    private val linkFile = BASE_FILE_LINK.copy(
        lastModified = lastModified,
        size = Bytes(123),
    )
    private val linkFolder = BASE_FOLDER_LINK.copy(
        lastModified = lastModified,
        size = Bytes(123),
    )

    private var savedLocale = Locale.getDefault()
    private val context: Context
        get() = composeTestRule.activity

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Before
    fun setup() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun tearDown() {
        Locale.setDefault(savedLocale)
    }

    @Test
    fun folderOptions() {
        // region Arrange
        val entry = SimpleTestEntry("Test entry 1")
        val link = linkFolder.toDriveLink()
        assert(!entry.wasClicked)
        // endregion
        // region Act
        composeTestRule.setContent {
            WithLocale(Locale.ENGLISH) {
                ParentFolderOptions(
                    folder = link as DriveLink.Folder,
                    entries = listOf(entry),
                )
            }
        }
        assert(!entry.wasClicked)
        composeTestRule.onNodeWithText("Test entry 1")
            .performClick()
        // endregion
        // region Assert
        assert(entry.wasClicked)
        val title = composeTestRule.activity.getString(I18N.string.folder_options_header_title, "FOLDER")
        composeTestRule.onNodeWithText(title).assertExists()
        // Note: the space between 123 and B is an non-breakable space, not a regular one
        composeTestRule.onNodeWithText("123 B | Oct 25, 2020").assertDoesNotExist()
        composeTestRule.onNodeWithText("Test entry 1").assertExists()
        composeTestRule.onNodeWithText("Test entry 2").assertDoesNotExist()
        // endregion
    }

    @Test
    fun fileOptions() {
        // region Arrange
        val entry1 = SimpleTestEntry("Test entry 1")
        val entry2 = SimpleTestEntry("Test entry 2")
        val link = linkFile.toDriveLink()
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        // endregion
        // region Act
        composeTestRule.setContent {
            WithLocale(Locale.ENGLISH) {
                FilesOptions(
                    file = link as DriveLink.File,
                    entries = listOf(entry1, entry2),
                )
            }
        }
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        composeTestRule.onNodeWithText("Test entry 2").performClick()
        // endregion
        // region Assert

        assert(!entry1.wasClicked)
        assert(entry2.wasClicked)
        composeTestRule.onNodeWithText("FILE").assertExists()
        // Note: the space between 123 and B is an non-breakable space, not a regular one
        composeTestRule.onNodeWithText("123 B | Oct 25, 2020").assertExists()
        composeTestRule.onNodeWithText("Test entry 1").assertExists()
        composeTestRule.onNodeWithText("Test entry 2").assertExists()
        // endregion
    }


    @Test
    fun stateBasedFileOptionNotAvailableOffline() {
        // region Arrange
        val entry1 = SimpleTestEntry("Test entry 1")
        val entry2 = StateBaseTestEntry()
        val driveLink = (linkFile.toDriveLink() as DriveLink.File).copy(isMarkedAsOffline = false)
        assert(!driveLink.isMarkedAsOffline)
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        // endregion
        // region Act
        composeTestRule.setContent {
            WithLocale(Locale.ENGLISH) {
                FilesOptions(
                    file = driveLink,
                    entries = listOf(entry1, entry2),
                )
            }
        }
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        composeTestRule.onNodeWithText("ONLINE").performClick()
        // endregion
        // region Assert

        assert(!entry1.wasClicked)
        assert(entry2.wasClicked)
        composeTestRule.onNodeWithText("Test entry 1").assertExists()
        composeTestRule.onNodeWithText("ONLINE").assertExists()
        // endregion
    }

    @Test
    fun stateBasedFileOptionLinkAvailableOffline() {
        // region Arrange
        val entry1 = SimpleTestEntry("Test entry 1")
        val entry2 = StateBaseTestEntry()
        val driveLink = (linkFile.toDriveLink() as DriveLink.File).copy(isMarkedAsOffline = true)
        assert(driveLink.isMarkedAsOffline)
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        // endregion
        // region Act
        composeTestRule.setContent {
            WithLocale(Locale.ENGLISH) {
                FilesOptions(
                    file = driveLink,
                    entries = listOf(entry1, entry2),
                )
            }
        }
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        composeTestRule.onNodeWithText("OFFLINE").performClick()
        // endregion
        // region Assert

        assert(!entry1.wasClicked)
        assert(entry2.wasClicked)
        composeTestRule.onNodeWithText("Test entry 1").assertExists()
        composeTestRule.onNodeWithText("OFFLINE").assertExists()
        // endregion
    }

    @Test
    fun stateBasedFileOptionLinkAncestorAvailableOffline() {
        // region Arrange
        val entry1 = SimpleTestEntry("Test entry 1")
        val entry2 = StateBaseTestEntry()
        val driveLink = (linkFile.toDriveLink() as DriveLink.File).copy(
            isMarkedAsOffline = false,
            isAnyAncestorMarkedAsOffline = true
        )
        assert(!driveLink.isMarkedAsOffline)
        assert(driveLink.isAnyAncestorMarkedAsOffline)
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        // endregion
        // region Act
        composeTestRule.setContent {
            WithLocale(Locale.ENGLISH) {
                FilesOptions(
                    file = driveLink,
                    entries = listOf(entry1, entry2),
                )
            }
        }
        assert(!entry1.wasClicked)
        assert(!entry2.wasClicked)
        composeTestRule.onNodeWithText("ANCESTOR_OFFLINE").performClick()
        // endregion
        // region Assert

        assert(!entry1.wasClicked)
        assert(entry2.wasClicked)
        composeTestRule.onNodeWithText("Test entry 1").assertExists()
        composeTestRule.onNodeWithText("ANCESTOR_OFFLINE").assertExists()
        // endregion
    }

    @Composable
    private inline fun WithLocale(locale: Locale, crossinline block: @Composable () -> Unit) {
        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val configuration = Configuration()
            configuration.setLocale(locale)
            LocalContext.current.createConfigurationContext(configuration)
        } else {
            val resources = LocalContext.current.resources
            val configuration = resources.configuration
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
            context
        }
        CompositionLocalProvider(LocalContext provides context) {
            block()
        }
    }

    private class SimpleTestEntry(private val label: String) : FileOptionEntry.SimpleEntry<DriveLink> {

        var wasClicked = false
        override val onClick: (DriveLink) -> Unit = {
            wasClicked = true
        }

        override val icon: Int = CorePresentation.drawable.ic_proton_bug

        @Composable
        override fun getLabel() = label
    }

    private class StateBaseTestEntry : FileOptionEntry.StateBasedEntry<DriveLink> {

        @Composable
        override fun getLabel(driveLink: DriveLink) = if (driveLink.isMarkedAsOffline) {
            "OFFLINE"
        } else if (driveLink.isAnyAncestorMarkedAsOffline) {
            "ANCESTOR_OFFLINE"
        } else {
            "ONLINE"
        }

        override fun getIcon(driveLink: DriveLink) = CorePresentation.drawable.ic_proton_bug

        var wasClicked = false
        override val onClick: (DriveLink) -> Unit = {
            wasClicked = true
        }
    }
}
