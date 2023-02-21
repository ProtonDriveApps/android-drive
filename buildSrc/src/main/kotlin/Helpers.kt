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

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(timeoutAmount, timeoutUnit) }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw IOException(error)
        }
        inputStream.bufferedReader().readText().trim()
    }

fun File.findModules(recursive: Boolean = false): Iterable<String> {
    val blacklist = setOf(
        ".git",
        ".gradle",
        ".idea",
        "buildSrc",
        "config",
        "build",
        "src"
    )

    fun File.childrenDirectories() = listFiles { _, name -> name !in blacklist }!!.filter { it.isDirectory }

    fun File.isProject() =
        File(this, "settings.gradle.kts").exists() || File(this, "settings.gradle").exists()

    fun File.isModule() = !isProject() &&
            File(this, "build.gradle.kts").exists() || File(this, "build.gradle").exists()

    val modules = mutableSetOf<String>()

    fun File.find(name: String? = null, includeModules: Boolean = true): List<File> = childrenDirectories().flatMap {
        val newName = (name ?: "") + it.name
        when {
            it.isProject() -> if (recursive) it.find("$newName:", includeModules = false) else emptySet()
            it.isModule() && includeModules -> {
                modules += ":$newName"
                it.find("$newName:")
            }
            else -> if (recursive) it.find("$newName:") else emptySet()
        }
    }

    find()

    return modules
}
