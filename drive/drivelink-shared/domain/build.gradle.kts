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
    namespace = "me.proton.core.drive.drivelink.shared.domain"
}

driveModule(
    hilt = true,
    socialTest = true,
) {
    api(project(":drive:drivelink:domain"))
    api(project(":drive:drivelink-crypto:domain"))
    api(project(":drive:drivelink-sorting:domain"))
    api(project(":drive:event-manager:base:domain"))
    api(project(":drive:key:domain"))
    api(project(":drive:share-crypto:domain"))
    api(project(":drive:share-url:base:domain"))
    api(project(":drive:share-url:crypto:domain"))
    api(libs.androidx.paging.common)
}
