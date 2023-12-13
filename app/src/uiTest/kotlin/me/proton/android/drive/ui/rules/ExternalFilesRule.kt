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
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class ExternalFilesRule(
    private val folderBuilder: (Context) -> File = { context ->
        File(context.externalCacheDir, "ui-test")
    },
) : ExternalResource() {

    private lateinit var dir: File

    private val assetManager
        get() = InstrumentationRegistry
            .getInstrumentation()
            .context.assets ?: error("Could not load assets")

    override fun before() {
        dir = folderBuilder(ApplicationProvider.getApplicationContext<Application>())
        dir.deleteRecursively()
        dir.mkdirs()
    }

    override fun after() {
        dir.deleteRecursively()
    }

    fun createFile(name: String, size: Long) = File(dir, name).apply {
        createNewFile()
        if (size > 0L) {
            RandomAccessFile(this, "rw").use { it.setLength(size) }
        }
    }

    fun createEmptyFile(name: String) = createFile(name, size = 0L)

    fun create1BFile(name: String) = createFile(name, size = 1L)

    fun copyFileFromAssets(name: String, flatten: Boolean = false) = File(dir, name).let { file ->
        if (flatten) {
            File(dir, file.name)
        } else {
            file
        }
    }.apply {
        assetManager.open(name).use { input ->
            FileOutputStream(this).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun copyDirFromAssets(name: String, flatten: Boolean = false) {
        val fileList = assetManager.list(name) ?: error("Could not list files in $name")

        if (fileList.isEmpty())
            copyFileFromAssets(name, flatten)
        else
            fileList.forEach {
                if (!flatten) {
                    File(dir, name).mkdirs()
                }
                copyDirFromAssets("$name/$it", flatten)
            }
    }
}
