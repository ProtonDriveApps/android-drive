/*
 * Copyright (c) 2021-2023 Proton AG.
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

driveModule(
    compose = true
) {
    api(project(":drive:base:domain"))
    api(libs.androidx.activity.compose)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.uiTooling)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.core.network)
    api(libs.core.presentation)
    api(libs.core.presentation.compose)

    implementation(libs.retrofit)
}

configureJacoco()
