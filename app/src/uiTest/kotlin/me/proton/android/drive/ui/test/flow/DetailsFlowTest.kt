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

package me.proton.android.drive.ui.test.flow

import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.presentation.extension.lastModified
import me.proton.core.drive.file.info.presentation.FileInfoTestTag
import me.proton.core.test.quark.data.User
import me.proton.test.fusion.FusionConfig.targetContext
import org.junit.Rule
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

class DetailsFlowTest : BaseTest() {
    private val user
        get() = User(
            dataSetScenario = "4",
            name = "proton_drive_${getRandomString(20)}"
        )

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user, shouldSeedUser = true)

    @Test
    fun checkFileDetailsAndCloseDetails() {
        FilesTabRobot
            .verify { robotDisplayed() }
            .scrollToItemWithName(sharedImage.name)
            .clickMoreOnItem(sharedImage.name)
            .clickFileDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(sharedImage.name)
                hasHeaderWithIconType(FileInfoTestTag.Header.HeaderIconType.THUMBNAIL)
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_name_entry),
                    value = sharedImage.name,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = sharedImage.uploadedBy,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_location_entry),
                    value = sharedImage.location,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = sharedImage.modified,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_type_entry),
                    value = requireNotNull(sharedImage.type),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_mime_type_entry),
                    value = requireNotNull(sharedImage.mimeType),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_size_entry),
                    value = requireNotNull(sharedImage.size),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_shared_entry),
                    value = sharedImage.isShared,
                )
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun checkFolderDetailsAndCloseDetails() {
        FilesTabRobot
            .verify { robotDisplayed() }
            .scrollToItemWithName(sharedFolder.name)
            .clickMoreOnItem(sharedFolder.name)
            .clickFolderDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(sharedFolder.name)
                hasHeaderWithIconType(FileInfoTestTag.Header.HeaderIconType.PLACEHOLDER)
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_name_entry),
                    value = sharedFolder.name,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = sharedFolder.uploadedBy,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_location_entry),
                    value = sharedFolder.location,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = sharedFolder.modified,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_shared_entry),
                    value = sharedFolder.isShared,
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_type_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_mime_type_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_size_entry),
                )
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
    }

    data class LinkDetails(
        val name: String,
        val uploadedBy: String,
        val location: String,
        val modified: String,
        val isShared: String,
        val type: String? = null,
        val mimeType: String? = null,
        val size: String? = null,
    )

    private val sharedImage get() = LinkDetails(
        name = "shared.jpg",
        uploadedBy = "${user.name}@${uiTestHelper.configurationProvider.host}",
        location = "/My files",
        modified = TimestampS().lastModified(),
        isShared = "Yes",
        type = "JPG image",
        mimeType = "image/jpeg",
        size = "12.73 KB",
    )

    private val sharedFolder get() = LinkDetails(
        name = "sharedFolder",
        uploadedBy = "${user.name}@${uiTestHelper.configurationProvider.host}",
        location = "/My files",
        modified = TimestampS().lastModified(),
        isShared = "Yes",
    )
}
