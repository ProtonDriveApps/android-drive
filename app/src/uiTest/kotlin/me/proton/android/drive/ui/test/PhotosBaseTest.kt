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

package me.proton.android.drive.ui.test

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.test.rule.GrantPermissionRule
import me.proton.android.drive.ui.rules.ExternalFilesRule
import org.junit.Rule
import java.io.File

abstract class PhotosBaseTest: AuthenticatedBaseTest() {
    private val permissions =
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) + when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )

            else -> emptyList()
        }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(*permissions.toTypedArray())

    @get:Rule
    val dcimCameraFolder = ExternalFilesRule {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera",
        )
    }
    @get:Rule
    val pictureCameraFolder = ExternalFilesRule {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Camera",
        )
    }
}
