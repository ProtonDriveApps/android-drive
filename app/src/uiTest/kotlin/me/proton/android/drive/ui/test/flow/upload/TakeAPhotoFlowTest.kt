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

package me.proton.android.drive.ui.test.flow.upload

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.i18n.R
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.test.fusion.FusionConfig.targetContext
import org.junit.Rule
import org.junit.Test


@HiltAndroidTest
class TakeAPhotoFlowTest : AuthenticatedBaseTest() {

    @get:Rule
    val intentsTestRule = IntentsRule()

    @Test
    fun takeAPhotoAndUploadIt() {
        var uri: Uri? = null

        Intents
            .intending(IntentMatchers.hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
            .respondWithFunction { intent ->
                uri = getParcelableExtra(intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)
                copyFromAssets("boat.jpg", requireNotNull(uri))
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            }

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickTakePhoto()
            .verify {
                assertFilesBeingUploaded(1, StringUtils.stringFromResource(R.string.title_my_files))
                itemIsDisplayed(
                    name = requireNotNull(uri).getFileName(),
                    hasThumbnail = true,
                )
            }
    }

    private fun copyFromAssets(fileName: String, uri: Uri) {
        InstrumentationRegistry
            .getInstrumentation()
            .context
            .assets.open(fileName).use { input ->
                targetContext.contentResolver.openOutputStream(uri, "w")?.use { output ->
                        input.copyTo(output)
                    }
            }
    }

    private fun Uri.getFileName(): String {
        return requireNotNull(
            targetContext.contentResolver.query(
                this,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst()
                cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            })
    }
}
