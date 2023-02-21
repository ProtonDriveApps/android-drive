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

import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.dependencies
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.TestedExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

@Suppress("LongMethod")
fun Project.driveModule(
    hilt: Boolean = false,
    serialization: Boolean = false,
    room: Boolean = false,
    workManager: Boolean = false,
    compose: Boolean = false,
    includeSubmodules: Boolean = false,
    kapt: Boolean = hilt || room,
    dependencies: DependencyHandler.() -> Unit = {},
) {
    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    apply(plugin = "kotlin-android")
    if (kapt) {
        apply(plugin = "kotlin-kapt")
    }
    if (hilt) {
        apply(plugin = "dagger.hilt.android.plugin")
    }
    if (serialization) {
        apply(plugin = "kotlinx-serialization")
    }
    apply<DeleteTestPlugin>()

    extensions.findByType<ApplicationExtension>()?.apply {
        compileSdk = Config.compileSdk

        defaultConfig {
            applicationId = Config.applicationId
            versionCode = versionCodeFromTags
            versionName = Config.versionName
            resourceConfigurations.addAll(Config.resourceConfigurations)

            javaCompileOptions {
                annotationProcessorOptions {
                    arguments["room.schemaLocation"] = "$projectDir/schemas"
                }
            }
        }

        buildTypes {
            debug {
                isMinifyEnabled = false
                isDebuggable = true
                enableUnitTestCoverage = true
                enableAndroidTestCoverage = true
            }
            release {
                isMinifyEnabled = true
                //isShrinkResources = true // should be replaced by useResourceShrinker
                proguardFiles(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        buildFeatures {
            this.compose = compose
        }

        (this as ExtensionAware).extensions.configure<KotlinJvmOptions>("kotlinOptions") {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    extensions.findByType<LibraryExtension>()?.apply {
        compileSdk = Config.compileSdk
        buildTypes {
            debug {
                enableUnitTestCoverage = true
                enableAndroidTestCoverage = true
            }
            release {
                isMinifyEnabled = false
            }
        }
        buildFeatures {
            this.compose = compose
        }

        (this as ExtensionAware).extensions.configure<KotlinJvmOptions>("kotlinOptions") {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    extensions.configure<TestedExtension> {

        defaultConfig {
            minSdk = Config.minSdk
            targetSdk = Config.targetSdk
            testInstrumentationRunner = Config.testInstrumentationRunner
        }

        sourceSets {
            getByName("main").java.srcDirs("src/main/kotlin")
            getByName("test").java.srcDirs("src/test/kotlin")
            getByName("androidTest").java.srcDirs("src/androidTest/kotlin", "src/uiTest/kotlin")
            getByName("androidTest").assets.srcDirs("src/uiTest/assets")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        if (compose) {
            composeOptions {
                kotlinCompilerExtensionVersion = catalog.findVersion("androidx.compose.compiler").get().requiredVersion
            }
        }

        packagingOptions {
            resources.excludes.add("META-INF/licenses/**")
            resources.excludes.add("META-INF/AL2.0")
            resources.excludes.add("META-INF/LGPL2.1")
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }

    dependencies {
        dependencies()

        add("implementation", catalog.findLibrary("kotlinx.coroutines.core").get())
        add("implementation", catalog.findLibrary("core.utilKotlin").get())
        if (compose) {
            add("debugImplementation", catalog.findLibrary("androidx.compose.uiTestManifest").get())
            if (hilt) {
                add("implementation",  catalog.findLibrary("androidx.hilt.navigation.compose").get())
            }
        }
        if (room) {
            add("kapt", catalog.findLibrary("androidx.room.compiler").get())
            add("implementation", catalog.findLibrary("core.dataRoom").get())
            add("implementation", catalog.findLibrary("androidx.room.ktx").get())
        }
        if (hilt) {
            add("implementation", catalog.findLibrary("dagger.hilt.android").get())
            add("kapt", catalog.findLibrary("dagger.hilt.compiler").get())
            add("kapt", catalog.findLibrary("androidx.hilt.compiler").get())
        }
        if (workManager) {
            if (hilt) {
                add("implementation", catalog.findLibrary("androidx.hilt.work").get())
            }
            add("implementation", catalog.findLibrary("androidx.work.runtime.ktx").get())
        }
        if (serialization) {
            add("implementation", catalog.findLibrary("kotlinx.serialization.json").get())
        }
        if (includeSubmodules) {
            val fullName = project.fullName
            projectDir.findModules().forEach { module ->
                add("api", project(":$fullName$module"))
            }
        }

        // region Test
        add("testImplementation", catalog.findBundle("test.jvm").get())
        add("androidTestImplementation", catalog.findBundle("test.android").get())
        // endregion
    }
}

// first alpha and fourth beta were not tagged in git so we add them to list of all git tags
val Project.tags get() = "1.0.0-alpha01\n1.0.0_cancelled(16)\n1.0.0_cancelled(18)\n1.0.0_cancelled(20)\n1.0.0-beta04\n" + "git tag".runCommand(workingDir = rootDir)

val Project.versionCodeFromTags: Int get() = tags.countSubstrings("\n") + 2 // last new line + next tag

fun String.countSubstrings(substring: String): Int =
    split(substring).dropLastWhile { it.isEmpty() }.size - 1

private val Project.fullName: String
    get() = "${parent?.run { if (this == rootProject) null else "$fullName:" } ?: ""}$name"
