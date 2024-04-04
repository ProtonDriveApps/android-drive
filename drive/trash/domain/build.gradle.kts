/*
 * Copyright (c) 2021-2024 Proton AG.
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

android {
    namespace = "me.proton.core.drive.trash.domain"
}

driveModule(
    hilt = true,
    room = true,
    socialTest = true,
    i18n = true,
) {
    api(project(":drive:link-trash:domain"))
    api(libs.androidx.paging.common)
    implementation(project(":drive:crypto:domain"))
    implementation(project(":drive:message-queue:domain"))
    implementation(project(":drive:share:domain"))

    testImplementation(project(":drive:drivelink-download:data-test"))
    testImplementation(project(":drive:file:base:data"))
    testImplementation(project(":drive:message-queue:data"))
    testImplementation(project(":drive:trash:data-test"))
}

configureJacoco()
