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

android {
    namespace = "me.proton.core.drive.drivelink.shared.data"
}

driveModule(
    hilt = true,
    room = true,
    socialTest = true,
    workManager = true,
) {
    api(project(":drive:drivelink-shared:domain"))
    api(project(":drive:drivelink:data"))
    api(project(":drive:share-url:base:data"))
    api(project(":drive:share-user:data"))
    api(libs.androidx.room.paging)
}
