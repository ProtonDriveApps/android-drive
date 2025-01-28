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

package me.proton.android.drive.loadtesting

import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.ui.extension.respondWithFiles
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.drive.base.domain.extension.KiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.i18n.R
import me.proton.test.fusion.Fusion.node
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@HiltAndroidTest
@RunWith(Parameterized::class)
class UploadLoadTest(
    private val count: Long,
    private val size: Long,
) : ExternalStorageBaseTest() {

    @Before
    fun setUp() {
        AddAccountRobot
            .uiElementsDisplayed()
            .clickSignIn()
            .login("tester@proton.ch", "a")
    }

    @Test
    fun test() = runTest(timeout = 15.minutes) {
        val now = Instant.now()
        fun filename(index: Long) = "file$index-${now.epochSecond}.txt"
        val files = (1..count).map {
            externalFilesRule.createFile(filename(it), size)
        }

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFiles(files)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                node.withText(R.string.files_upload_stage_uploading)
                    .await(1.minutes) { assertIsDisplayed() }
                node.withText(R.string.files_upload_stage_uploading)
                    .await(10.minutes) { assertIsNotDisplayed() }
            }
            .pullToRefresh(FilesTabRobot)
            .scrollToItemWithName(filename(count))
            .verify { itemIsDisplayed(filename(count)) }
    }



    companion object {

        @get:Parameterized.Parameters(name = "Upload_{0}_files_of_size_{1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(1000, 1.bytes.value),
            arrayOf(200, 512.KiB.value),
            arrayOf(3, 512.MiB.value),
        )
    }
}
