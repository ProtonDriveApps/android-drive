/*
 * Copyright (c) 2026 Proton AG.
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

import com.android.build.api.dsl.TestExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

fun Project.driveTestModule(
    targetProjectPath: String,
    dependencies: DependencyHandler.() -> Unit = {},
) {
    apply(plugin = "kotlin-android")

    extensions.configure<TestExtension> {
        compileSdk = Config.compileSdk
        this.targetProjectPath = targetProjectPath

        defaultConfig {
            minSdk = Config.minSdk
            targetSdk = Config.targetSdk
            testInstrumentationRunner = Config.testInstrumentationRunner
        }

        sourceSets {
            getByName("main").java.srcDirs("src/main/kotlin")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configureJvmTarget()

    dependencies {
        dependencies()
    }
}
