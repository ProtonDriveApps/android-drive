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

package me.proton.android.drive.ui.toolkits

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onFirst
import androidx.test.platform.app.InstrumentationRegistry
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.core.test.android.instrumented.ProtonTest
import me.proton.core.util.kotlin.CoreLogger
import me.proton.test.fusion.FusionConfig
import java.io.FileNotFoundException
import java.io.FileOutputStream

fun getRandomString(length: Int = 10): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9').shuffled()
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun AbstractBaseTest.Companion.screenshot() {
    val screenshotNumber = screenshotCounter.getAndIncrement()
    val fileName = "${screenshotLocation}/${screenshotNumber}_${testName.methodName}.png"

    if (screenshotNumber == 0) {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("mkdir -p $screenshotLocation")
    }

    try {
        FusionConfig.Compose.testRule.get()
            .onAllNodes(SemanticsMatcher("isRoot") { it.isRoot })
            .onFirst()
            .captureToImage()
            .asAndroidBitmap()
            .let { bitmap ->
                FileOutputStream(fileName).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }

    } catch (throwable: Throwable) {
        when (throwable) {
            is FileNotFoundException -> "File not found"
            is OutOfMemoryError -> "Out of memory"
            else -> "Unknown error"
        }.let {
            CoreLogger.e(ProtonTest.testTag, throwable, "Could not take screenshot: $it")
        }
    }
}
