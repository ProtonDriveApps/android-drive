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

import java.util.Properties

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    kotlin("android")
    kotlin("kapt")
}

base {
    archivesName.set(Config.archivesBaseName)
}

tasks.register("getArchivesName") {
    doLast {
        println("[ARCHIVES_NAME]${Config.archivesBaseName}")
    }
}

tasks.register("getArchivesVersion") {
    doLast {
        println("[ARCHIVES_VERSION]${Config.versionName}")
    }
}

driveModule(
    hilt = true,
    room = true,
    compose = true,
    workManager = true,
    serialization = true,
) {
    implementation(files("../../proton-libs/gopenpgp/gopenpgp.aar"))
    implementation(project(":app-lock"))
    implementation(project(":app-ui-settings"))
    implementation(project(":drive"))
    implementation(project(":verifier"))
    implementation(project(":photos"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.compose.foundationLayout)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.bundles.accompanist)
    implementation(libs.bundles.core)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.plumber)
    implementation(libs.sentry)
    implementation(libs.timber)
    implementation(libs.treessence)

    androidTestImplementation(libs.dagger.hilt.android.testing)
    kapt(libs.dagger.hilt.android.compiler)
    kaptAndroidTest(libs.dagger.hilt.android.compiler)

    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)

    testImplementation(project(":drive:db-test"))
    androidTestImplementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.bundles.core.test)
    androidTestImplementation(libs.fusion)
    androidTestImplementation(project(":drive:backup:data"))
    androidTestUtil(libs.androidx.test.orchestrator)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

val privateProperties = Properties().apply {
    try {
        load(rootDir.resolve("private.properties").inputStream())
    } catch (exception: java.io.FileNotFoundException) {
        // Provide empty properties to allow the app to be built without secrets
        logger.warn("private.properties file not found", exception)
        Properties()
    }
}

val lastAlpha = tags.countSubstrings("${Config.versionName}-alpha")
val lastBeta = tags.countSubstrings("${Config.versionName}-beta")

android {
    namespace = "me.proton.android.drive"
    signingConfigs {
        create("release") {
            storeFile = file(privateProperties.getProperty("SIGN_KEY_STORE_FILE_PATH") ?: "protonkey.jks")
            storePassword = privateProperties.getProperty("SIGN_KEY_STORE_PASSWORD")
            keyAlias = privateProperties.getProperty("SIGN_KEY_ALIAS")
            keyPassword = privateProperties.getProperty("SIGN_KEY_ALIAS_PASSWORD")
        }
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    val gitHash = "git rev-parse --short HEAD".runCommand(workingDir = rootDir)
    defaultConfig {

        buildConfigField("String", "HOST", "\"proton.me\"")
        buildConfigField("String", "BASE_URL", "\"https://drive-api.proton.me/\"")
        buildConfigField("String", "APP_VERSION_HEADER", "\"android-drive@$versionName\"")
        buildConfigField("String", "FLAVOR_DEVELOPMENT", "\"dev\"")
        buildConfigField("String", "FLAVOR_DYNAMIC", "\"dynamic\"")
        buildConfigField("String", "FLAVOR_ALPHA", "\"alpha\"")
        buildConfigField("String", "FLAVOR_BETA", "\"beta\"")
        buildConfigField("String", "FLAVOR_PRODUCTION", "\"prod\"")
        buildConfigField("String", "SENTRY_DSN", "\"https://28f8df131f7a4ca4940e86972ba5038f@drive-api.proton.me/core/v4/reports/sentry/11\"")
        buildConfigField("String", "ACCOUNT_SENTRY_DSN", "\"${System.getenv("ACCOUNT_SENTRY_DSN").orEmpty()}\"")
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        buildConfigField("String", "PROXY_TOKEN", "\"${privateProperties.getProperty("PROXY_TOKEN")}\"")
        testInstrumentationRunner = "me.proton.android.drive.ui.HiltTestRunner"

    }
    flavorDimensions.add("default")
    productFlavors {
        create("dev") {
            versionCode = 1
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev ($gitHash)"

            val host = "proton.black"

            buildConfigField("String", "HOST", "\"$host\"")
            buildConfigField("String", "BASE_URL", "\"https://drive.$host/api/\"")
        }
        create("dynamic") {
            applicationIdSuffix = ".dynamic"
            versionNameSuffix = "-dynamic ($gitHash)"

            val host = privateProperties.getProperty("HOST")

            if (host.isNullOrEmpty()) {
                logger.error("HOST variable is not set! \n" +
                        "Make sure private.properties file exists and has HOST variable set " +
                        "eg. HOST=proton.me")
            }

            buildConfigField("String", "HOST", "\"$host\"")
            buildConfigField("String", "BASE_URL", "\"https://drive.$host/api/\"")

            testInstrumentationRunnerArguments["clearPackageData"] = "true"
        }
        create("alpha") {
            versionNameSuffix = "-alpha%02d".format(lastAlpha + 1)
        }
        create("beta") {
            versionNameSuffix = "-beta%02d".format(lastBeta + 1)
        }
        create("prod") {
        }
    }

    buildFeatures {
        viewBinding = true // required by Core presentation
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    hilt {
        enableAggregatingTask = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

tasks.create("publishGeneratedReleaseNotes") {
    doLast {
        val releaseNotesDir = File("${project.projectDir}/src/main/play/release-notes/en-US")
        releaseNotesDir.mkdirs()
        val releaseNotesFile = File(releaseNotesDir, "default.txt")
        // Limit of 500 chars on Google Play console for release notes
        releaseNotesFile.writeText(
            generateChangelog(
                rootDir,
                since = System.getenv("CI_COMMIT_BEFORE_SHA")
            )
        )
    }
}

tasks.create("printGeneratedChangelog") {
    doLast {
        println(generateChangelog(rootDir, since = System.getProperty("since")))
    }
}

configureJacoco(flavor = "dev")
