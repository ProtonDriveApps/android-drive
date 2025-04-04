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

package me.proton.android.drive.ui.extension

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.OngoingStubbing
import java.io.File


fun OngoingStubbing.respondWithFile(block:() -> File) {
    respondWithFunction {
        Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(block())))
    }
}

fun OngoingStubbing.respondWithFile(file: File) {
    respondWithFile { file }
}

fun OngoingStubbing.respondWithFiles(files: List<File>) {
    respondWithFunction {
        Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().apply {
            val items = files.map { file ->
                ClipData.Item(Uri.fromFile(file))
            }
            clipData = ClipData(
                ClipDescription(
                    "", files.map { "text/plain" }.toTypedArray()
                ),
                items.first()
            ).also { clipData ->
                (items - items.first()).forEach(clipData::addItem)
            }
        })
    }
}
