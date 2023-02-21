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
import java.lang.StringBuilder

fun generateChangelog(
    workingDir: File = File("."),
    since: String? = null
): String {
    val commitDate = "git show --format=%cd ${since ?: ""}"
        .runCommand(workingDir)
        .trim()
    return "git --no-pager log --pretty=%s --no-decorate --abbrev-commit --since=\"$commitDate\""
        .runCommand(workingDir)
        .split("\n")
        .groupBy(Group::fromMessage) { message ->
            message.substringAfter(':')
        }
        .let { changes ->
            buildString {
                append("# Changelog\n\n")
                enumValues<Group>()
                    .filter { group -> group.isIncluded }
                    .forEach { group -> add(group, changes) }
            }
        }
}

private fun StringBuilder.add(group: Group, changes: Map<Group, List<String>>) {
    if (changes.containsKey(group)) {
        append("## ${group.title}\n")
        changes[group]?.forEach { message -> append("- $message\n") }
        append("\n")
    }
}

private enum class Group(val key: String, val title: String, val isIncluded: Boolean = false) {
    FEAT("feat", "New features", true),
    FIX("fix", "Bug fixes", true),
    CHORE("chore", "Maintenance"),
    BUILD("build", "Build"),
    CI("ci", "Continuous Integration"),
    DOCS("docs", "Documentation"),
    STYLE("style", "Style"),
    REFACTOR("refactor", "Refactoring", true),
    PERF("perf", "Performances"),
    TEST("test", "Tests"),
    IGNORED("", "IGNORED");

    companion object {
        fun fromMessage(message: String): Group {
            values().forEach { group ->
                if (message.startsWith(group.key)) {
                    return group
                }
            }
            return IGNORED
        }
    }
}
