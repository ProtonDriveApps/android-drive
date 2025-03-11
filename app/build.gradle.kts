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

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationProductFlavor
import configuration.extensions.protonEnvironment
import java.util.Properties

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    kotlin("android")
    kotlin("kapt")
    id("me.proton.core.gradle-plugins.environment-config") version "1.3.0"
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
    buildConfig = true,
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
    implementation(libs.google.play.review)
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
    testImplementation(project(":drive:test"))
    androidTestImplementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.bundles.core.test)
    androidTestImplementation(libs.fusion)
    androidTestImplementation(libs.okhttpLoggingInterceptor)
    androidTestImplementation(project(":drive:backup:data"))
    androidTestImplementation(libs.core.test.android.test.rule)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlin.reflect)
    androidTestUtil(libs.androidx.test.orchestrator)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Configuration
    releaseImplementation(libs.core.config.dagger.staticDefaults)
    debugImplementation(libs.core.config.dagger.contentResolver)
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
        animationsDisabled = true
        managedDevices {
            localDevices {
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp"
                }
            }
        }
    }

    val gitHash = "git rev-parse --short HEAD".runCommand(workingDir = rootDir)
    val proxyToken = privateProperties.getProperty("PROXY_TOKEN", "")

    defaultConfig {
        protonEnvironment {
            host = "proton.me"
            apiPrefix = "drive-api"

            // Should be replaced with 'userProxy=true' after TPE-511 is fixed
            buildConfigField("String", "proxyToken", "\"$proxyToken\"")
        }

        buildConfigField("String", "APP_VERSION_HEADER", "\"android-drive@$versionName\"")
        buildConfigField("String", "FLAVOR_DEVELOPMENT", "\"dev\"")
        buildConfigField("String", "FLAVOR_ALPHA", "\"alpha\"")
        buildConfigField("String", "FLAVOR_BETA", "\"beta\"")
        buildConfigField("String", "FLAVOR_PRODUCTION", "\"prod\"")
        buildConfigField("String", "SENTRY_DSN", "\"${System.getenv("DRIVE_SENTRY_DSN").orEmpty()}\"")
        buildConfigField("String", "ACCOUNT_SENTRY_DSN", "\"${System.getenv("ACCOUNT_SENTRY_DSN").orEmpty()}\"")
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")

        setAssetLinksResValue("proton.me")

        testInstrumentationRunner = "me.proton.android.drive.ui.HiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["proxyToken"] = proxyToken
        val androidConfig = System.getenv("ANDROID_CONFIG")
        if (androidConfig != null) {
            testInstrumentationRunnerArguments["androidConfig"] = androidConfig
        }
    }
    flavorDimensions.add("default")
    productFlavors {
        create("dev") {
            versionCode = 1
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev ($gitHash)"
            isDefault = true

            val testEnvironment = System.getenv("TEST_ENV_DOMAIN")
            val dynamicEnvironment = privateProperties.getProperty("HOST", "proton.black")
            val environment = testEnvironment ?: dynamicEnvironment

            protonEnvironment {
                if (environment.startsWith("10.0.2.2")) {
                    baseUrl = "http://$environment"
                } else {
                    host = environment
                    apiPrefix = "drive-api"
                }
            }

            testInstrumentationRunnerArguments["clearPackageData"] = "true"
            if (environment.startsWith("10.0.2.2")) {
                testInstrumentationRunnerArguments["baseUrl"] = "http://$environment"
            } else {
                testInstrumentationRunnerArguments["host"] = environment
            }

            // If running on gitlab CI
            if (!System.getenv("CI_SERVER_NAME").isNullOrEmpty()) {
                generateEnvFile(buildTypes.getByName("debug"))
            }
            resourceConfigurations.addAll(Config.supportedResourceConfigurations)

            setAssetLinksResValue(environment)
        }
        create("alpha") {
            versionCode = (versionCodeFromGitCommitCount * 10) + 1
            versionNameSuffix = "-alpha ($gitCommitCount)"
            resourceConfigurations.addAll(Config.incubatingResourceConfigurations)
        }
        create("beta") {
            versionCode = (versionCodeFromGitCommitCount * 10) + 2
            versionNameSuffix = "-beta ($gitCommitCount)"
        }
        create("prod") {
            versionCode = (versionCodeFromGitCommitCount * 10) + 3
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

    lint {
        abortOnError = true
    }

    androidResources {
        generateLocaleConfig = true
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

fun ApplicationProductFlavor.generateEnvFile(buildType: ApplicationBuildType) {
    val apkNameBase = "${Config.archivesBaseName}-${name}-${buildType.name}"
    val commitRefName = System.getenv("CI_COMMIT_REF_NAME")
    val commitShortSha = System.getenv("CI_COMMIT_SHORT_SHA")

    val gcloudBucket = "gs://test-lab-u7cps962nd0a4-kx5m7jhd4pki6"
    val resultsDir = "$commitRefName/$commitShortSha"
    val localPath = "app/build/outputs/apk"

    val apkName = "${apkNameBase}.apk"
    val testApkName = "${apkNameBase}-androidTest.apk"

    val localApkPath = "$localPath/${name}/${buildType.name}/$apkName"
    val localTestApkPath = "$localPath/androidTest/${name}/${buildType.name}/$testApkName"
    val remoteApkPath = "$gcloudBucket/$resultsDir/$apkName"
    val remoteTestApkPath = "$gcloudBucket/$resultsDir/$testApkName"

    val variables = mapOf(
        "ARCHIVES_VERSION" to Config.versionName,
        "LOCAL_APK" to localApkPath,
        "LOCAL_TEST_APK" to localTestApkPath,
        "GCLOUD_BUCKET_URL" to gcloudBucket,
        "FIREBASE_RESULT_ROOT" to resultsDir,
        "REMOTE_APK" to remoteApkPath,
        "REMOTE_TEST_APK" to remoteTestApkPath,
    )

    file(layout.buildDirectory.file("variables.env")).apply {
        mkdirs()
        delete()
        createNewFile()
        variables.forEach {
            appendText("${it.key}=${it.value}\n")
        }
    }
}
