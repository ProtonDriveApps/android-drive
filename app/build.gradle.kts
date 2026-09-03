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
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import configuration.extensions.protonEnvironment
import java.util.Properties

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    kotlin("android")
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
    implementation(project(":app-lock"))
    implementation(project(":app-ui-settings"))
    implementation(project(":drive"))
    implementation(project(":verifier"))
    implementation(project(":photos"))
    implementation(project(":document-scanner"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.compose.foundationLayout)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.bundles.accompanist)
    implementation(libs.bundles.core)
    implementation(libs.drive.sdk)
    implementation(libs.google.play.review)
    implementation(libs.lottie.compose)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.okhttpLoggingInterceptor)
    implementation(libs.sentry)
    implementation(libs.timber)
    implementation(libs.treessence)
    implementation(libs.ufc.android.payment.billing.google)
    implementation(libs.ufc.android.payment.ui)

    androidTestImplementation(libs.dagger.hilt.android.testing)
    add("kspAndroidTest", libs.dagger.hilt.android.compiler)

    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)

    testImplementation(project(":drive:db-test"))
    testImplementation(project(":drive:test"))
    testImplementation(testFixtures(project(":app-ui-settings")))
    testImplementation(testFixtures(project(":drive:feature-flag:domain")))
    androidTestImplementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.bundles.core.test)
    androidTestImplementation(libs.core.payment.iap)
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

// Drive SDK: dev & alpha use Rust crypto, beta & prod use Go crypto (catalog default).
val driveSdkRustFlavors = setOf("dev", "alpha")
val driveSdkRustVersion = "${libs.versions.drive.sdk.get()}-rust"

android {
    namespace = "me.proton.android.drive"

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
        ndk {
            abiFilters += listOf(
                // x86 will never be supported by drive SDK
                "x86_64",
                "armeabi-v7a",
                "arm64-v8a",
            )
        }
        androidLocales(Config.resourceConfigurations)

        buildConfigField("String", "APP_VERSION_HEADER", "\"android-drive@$versionName\"")
        buildConfigField("String", "FLAVOR_DEVELOPMENT", "\"dev\"")
        buildConfigField("String", "FLAVOR_ALPHA", "\"alpha\"")
        buildConfigField("String", "FLAVOR_BETA", "\"beta\"")
        buildConfigField("String", "FLAVOR_PRODUCTION", "\"prod\"")
        buildConfigField("String", "SENTRY_DSN", "\"${System.getenv("DRIVE_SENTRY_DSN").orEmpty()}\"")
        buildConfigField("String", "ACCOUNT_SENTRY_DSN", "\"${System.getenv("ACCOUNT_SENTRY_DSN").orEmpty()}\"")
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        buildConfigField("String", "SDK_VERSION_NAME", "\"${libs.versions.drive.sdk.get()}\"")

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
            buildConfigField("String", "SDK_VERSION_NAME", "\"$driveSdkRustVersion\"")
            // Rust crypto flavor when the SDK is a local composite build.
            missingDimensionStrategy("crypto", "rust")

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
            androidLocales(Config.supportedResourceConfigurations)

            setAssetLinksResValue(environment)
        }
        create("alpha") {
            versionCode = (versionCodeFromGitCommitCount * 10) + 3
            versionNameSuffix = "-alpha ($gitCommitCount)"
            buildConfigField("String", "SDK_VERSION_NAME", "\"$driveSdkRustVersion\"")
            missingDimensionStrategy("crypto", "rust")
            androidLocales(Config.incubatingResourceConfigurations)
        }
        create("beta") {
            versionCode = (versionCodeFromGitCommitCount * 10) + 2
            versionNameSuffix = "-beta ($gitCommitCount)"
        }
        create("prod") {
            versionCode = (versionCodeFromGitCommitCount * 10) + 1
        }
    }

    buildFeatures {
        viewBinding = true // required by Core presentation
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
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

// Force the "-rust" SDK version on dev & alpha classpaths, including the SDK pulled
// in transitively by the :drive modules. Applies only to the Maven artifact.
androidComponents {
    onVariants { variant ->
        if (driveSdkRustFlavors.any { variant.name.startsWith(it) }) {
            configurations
                .matching { it.name.startsWith(variant.name) }
                .configureEach {
                    resolutionStrategy.eachDependency {
                        if (requested.group == "me.proton.drive" && requested.name == "sdk") {
                            useVersion(driveSdkRustVersion)
                        }
                    }
                }
        }
    }
}

tasks.register("publishGeneratedReleaseNotes") {
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

tasks.register("printGeneratedChangelog") {
    doLast {
        println(generateChangelog(rootDir, since = System.getProperty("since")))
    }
}

configureJacoco(flavor = "dev")

fun ApplicationProductFlavor.generateEnvFile(buildType: ApplicationBuildType) {
    val apkNameBase = "${Config.archivesBaseName}-${name}-${buildType.name}"
    val commitRefName = System.getenv("CI_COMMIT_REF_NAME")
    val commitShortSha = System.getenv("CI_COMMIT_SHORT_SHA")

    val gcloudBucket = "gs://test-lab-drive-android"
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

fun BaseAppModuleExtension.androidLocales(locales: List<String>) {
    androidResources {
        localeFilters.addAll(locales)
    }
}
