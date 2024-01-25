/*
 * Copyright (c) 2023 Proton AG.
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
plugins {
    id("com.android.library")
}
apply(plugin = "kotlin-android")
apply(plugin = "kotlin-kapt")

android {
    namespace = "me.proton.core.drive.test"
    defaultConfig {
        minSdk = Config.minSdk
        compileSdk = Config.compileSdk
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    (this as ExtensionAware).extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>("kotlinOptions") {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    api(libs.core.domain)
    api(libs.core.account.dagger)
    api(libs.core.accountManager.dagger) {
        exclude("me.proton.core", "notification-dagger")
        exclude("me.proton.core", "notification-presentation")
        exclude("me.proton.core", "account-recovery-presentation-compose")
    }
    api(libs.core.accountManager.data) {
        exclude("me.proton.core", "notification-presentation")
        exclude("me.proton.core", "account-recovery-presentation-compose")
    }
    api(libs.core.accountManager.domain)
    api(libs.core.crypto.dagger)
    api(libs.core.key.dagger)
    api(libs.core.user.dagger)
    api(libs.core.test.kotlin)
    api(libs.mockwebserver)
    api(libs.androidx.work.runtime.ktx)
    api(project(":drive:base:domain"))
    api(project(":drive:crypto-base:data"))
    api(project(":drive:db-test"))

    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.android.testing)
    add("kapt", libs.dagger.hilt.compiler)
    add("kapt", libs.androidx.hilt.compiler)

    api(libs.androidx.test.core.ktx)
    api(libs.junit)
}
