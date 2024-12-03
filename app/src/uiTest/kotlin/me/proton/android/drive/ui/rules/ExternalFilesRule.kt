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

package me.proton.android.drive.ui.rules

import android.app.Application
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import me.proton.core.util.kotlin.CoreLogger
import org.junit.rules.ExternalResource
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class ExternalFilesRule(
    private val folderBuilder: (Context) -> File = {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ui-test"
        )
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
    }.also { file -> listOf(file).scan() }

    fun getFile(name: String) = File(dir, name)

    fun deleteFile(name: String) = File(dir, name).delete()

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
    }.also { file -> listOf(file).scan() }

    fun copyDirFromAssets(name: String, flatten: Boolean = true) : List<File> {
        val fileList = assetManager.list(name) ?: error("Could not list files in $name")

        return if (fileList.isEmpty()) {
            listOf(copyFileFromAssets(name, flatten))
        } else {
            fileList.map {
                if (!flatten) {
                    File(dir, name).mkdirs()
                }
                copyDirFromAssets("$name/$it", flatten)
            }.flatten()
        }.also { files -> files.scan() }
    }
}

fun List<File>.scan() {
    MediaScannerConnection.scanFile(
        ApplicationProvider.getApplicationContext<Application>(),
        this.map { it.path }.toTypedArray(), null
    ) { path, uri ->
        CoreLogger.d("ExternalFilesRule", "Add file to media: $path as $uri")
    }
}
