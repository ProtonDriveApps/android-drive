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

package me.proton.android.drive.ui.rules

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.rules.ExternalResource
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class ExternalFilesRule : ExternalResource() {

    private lateinit var dir: File

    override fun before() {
        dir = File(
            ApplicationProvider.getApplicationContext<Application>().externalCacheDir,
            "ui-test"
        )
        dir.mkdirs()
    }

    @After
    override fun after() {
        dir.deleteRecursively()
    }

    fun createFile(name: String, size: Long = 0L) = File(dir, name).apply {
        createNewFile()
        if (size > 0L) {
            RandomAccessFile(this, "rw").use { it.setLength(size) }
        }
    }

    fun copyFileFromAssets(name: String) = File(dir, name).apply {
        InstrumentationRegistry.getInstrumentation().context.assets.open(name).use { input ->
            FileOutputStream(this).use { output ->
                input.copyTo(output)
            }
        }
    }
}