/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import pl.droidsonroids.gradle.pitest.PitestPluginExtension
import java.io.File

/**
 * PIT mutation testing for the mutation-testing safeguard (.safeguards/config.yaml), run by
 * scripts/safeguards/mutation_gate.py through the `pitestDebug` task.
 *
 * Only plain JVM unit tests run under PIT: Robolectric and Hilt tests fail inside PIT's test
 * process, so their classes are excluded. Calls into Kotlin compiler plumbing and the logger are
 * not mutated, since no test can or should notice them.
 *
 * `-Psafeguards.pit.targetClasses=a.B*,c.D*` limits the classes to mutate (the gate passes the
 * classes of the files a merge request changes); without it every `me.proton` class of the module
 * is mutated (a bare `*` would match PIT itself, which PIT refuses).
 */
internal fun Project.configureMutationTesting() {
    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    apply(plugin = "pl.droidsonroids.pitest")
    val targets = providers.gradleProperty(TARGET_CLASSES_PROPERTY)
        .map { value -> value.split(',').map(String::trim).filter(String::isNotEmpty) }
        .orElse(listOf(PROJECT_PACKAGES))
    val testSources = File(projectDir, "src/test")
    extensions.configure<PitestPluginExtension> {
        pitestVersion.set(catalog.findVersion("pitest").get().requiredVersion)
        targetClasses.set(targets)
        // Without this, PIT runs only the tests whose names start with a target class, so a file
        // tested by a differently named test would count as unreached.
        targetTests.set(listOf(PROJECT_PACKAGES))
        excludedClasses.set(GENERATED_CLASSES)
        excludedMethods.set(COMPILER_GENERATED_METHODS)
        avoidCallsTo.set(UNOBSERVABLE_CALLS)
        excludedTestClasses.set(provider { robolectricTestClasses(testSources) })
        excludeMockableAndroidJar.set(false)
        threads.set(2)
        outputFormats.set(listOf("XML", "HTML"))
        timestampedReports.set(false)
        failWhenNoMutations.set(false)
    }
}

private const val TARGET_CLASSES_PROPERTY = "safeguards.pit.targetClasses"
private const val PROJECT_PACKAGES = "me.proton.*"

/** Hilt, Dagger and Room code generated into the module's class output. */
private val GENERATED_CLASSES = listOf(
    "*_Factory", "*_Factory$*", "*_MembersInjector", "*.Hilt_*", "*_HiltModules*",
    "*_GeneratedInjector", "*_Impl", "*_Impl$*", "*.BuildConfig", "hilt_aggregated_deps.*",
    "dagger.hilt.*",
)

/** Methods the Kotlin compiler writes for data classes. */
private val COMPILER_GENERATED_METHODS = listOf(
    "equals", "hashCode", "toString", "copy", "copy\$default", "component*",
)

/** Calls no test can observe: null checks, coroutine result plumbing, closing helpers, logging. */
private val UNOBSERVABLE_CALLS = listOf(
    "kotlin.jvm.internal",
    "kotlin.ResultKt",
    "kotlin.io.CloseableKt",
    "me.proton.core.util.kotlin.CoreLogger",
)

private val ROBOLECTRIC_MARKERS = Regex("""\b\w*RobolectricTestRunner\b|\bAndroidJUnit4\b|@HiltAndroidTest\b""")
private val PACKAGE = Regex("""(?m)^\s*package\s+([\w.]+)""")
private val TOP_LEVEL_CLASS = Regex("""(?m)^(?:@\S+\s+)*(?:[a-z]+\s+)*class\s+([A-Za-z_]\w*)""")

/** Class globs of the test files that run under Robolectric or Hilt. */
private fun robolectricTestClasses(testSources: File): List<String> =
    testSources.walkTopDown()
        .filter { file -> file.isFile && (file.extension == "kt" || file.extension == "java") }
        .mapNotNull { file ->
            val text = file.readText()
            if (!ROBOLECTRIC_MARKERS.containsMatchIn(text)) return@mapNotNull null
            val packagePrefix = PACKAGE.find(text)?.groupValues?.get(1)?.let { "$it." }.orEmpty()
            TOP_LEVEL_CLASS.findAll(text).map { match -> "$packagePrefix${match.groupValues[1]}*" }.toList()
        }
        .flatten()
        .toList()
