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

package me.proton.android.drive.ui.test.flow.upload

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
@RunWith(Parameterized::class)
class UploadWithThumbnailFlowTest(
    private val fileName: String,
    private val hasThumbnail: () -> Boolean
) : ExternalStorageBaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadWithThumbnail() {
        val file = externalFilesRule.copyFileFromAssets(fileName)

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWithFunction {
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(Uri.fromFile(file))
                )
            }

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    count = 1,
                    folderName = StringUtils.stringFromResource(I18N.string.title_my_files)
                )
                itemIsDisplayed(
                    name = fileName,
                    hasThumbnail = hasThumbnail()
                )
            }
    }

    companion object {

        private val ALL_VERSIONS = { true }
        private val ABOVE_Q = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q }

        @get:Parameterized.Parameters(name = "{0}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("boat.mp3", ABOVE_Q),
            arrayOf("boat.mp4", ALL_VERSIONS),
            arrayOf("boat.jpg", ALL_VERSIONS),
            arrayOf("small.pdf", ALL_VERSIONS),
            arrayOf("small.svg", ALL_VERSIONS),
        )
    }
}
