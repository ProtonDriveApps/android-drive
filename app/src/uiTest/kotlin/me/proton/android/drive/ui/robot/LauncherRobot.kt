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

package me.proton.android.drive.ui.robot

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import me.proton.android.drive.ui.MainActivity
import me.proton.test.fusion.FusionConfig.targetContext
import java.io.File
import java.nio.file.Files

object LauncherRobot {
    /**
     * Deeplink to any route in MainActivity with an intent VIEW
     * Test class should inherit from EmptyBaseTest to use it.
     * @param redirection the redirection to go to
     */
    fun <T : Robot> deeplinkTo(route: String, robot: T): T {
        ActivityScenario.launch<MainActivity>(Intent(targetContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("drive://proton.me/$route")
        })
        return robot
    }
    /**
     * Launch the MainActivity with an intent VIEW and the redirection.
     * Test class should inherit from EmptyBaseTest to use it.
     * @param redirection the redirection to go to
     */
    fun <T : Robot> launch(redirection: String, robot: T): T {
        return deeplinkTo("launcher?redirection=$redirection", robot)
    }
    /**
     * Launch the MainActivity with an intent SEND and the file.
     * Test class should inherit from EmptyBaseTest to use it.
     * @param file the file to upload
     */
    fun uploadTo(file: File): UploadToRobot {
        ActivityScenario.launch<MainActivity>(Intent(targetContext, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        })
        return UploadToRobot
    }
    /**
     * Launch the MainActivity with an intent SEND_MULTIPLE and the files.
     * Test class should inherit from EmptyBaseTest to use it.
     * @param files the files to upload
     */
    fun uploadTo(files: List<File>): UploadToRobot {
        ActivityScenario.launch<MainActivity>(Intent(targetContext, MainActivity::class.java).apply {
            val uris = files.map { file -> Uri.fromFile(file) }
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = Files.probeContentType(files.first().toPath())
            clipData = ClipData(
                ClipDescription(
                    "tests",
                    files.map { file ->
                        Files.probeContentType(file.toPath())
                    }.toTypedArray(),
                ),
                ClipData.Item(uris.first()),
            ).apply {
                (uris - uris.first()).forEach { uri ->
                    addItem(ClipData.Item(uri))
                }
            }
        })
        return UploadToRobot
    }
}
