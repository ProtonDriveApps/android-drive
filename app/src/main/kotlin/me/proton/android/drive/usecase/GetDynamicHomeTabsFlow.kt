/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.usecase.HasBusinessPlan
import me.proton.drive.android.settings.domain.entity.DynamicHomeTab
import me.proton.drive.android.settings.domain.entity.HomeTab
import me.proton.drive.android.settings.domain.usecase.GetHomeTab
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

class GetDynamicHomeTabsFlow @Inject constructor(
    private val getHomeTab: GetHomeTab,
    private val hasBusinessPlan: HasBusinessPlan,
) {
    operator fun invoke(userId: UserId): Flow<List<DynamicHomeTab>> = getHomeTab(userId)
        .map { userDefaultHomeTab ->
            HomeTab
                .entries
                .map { homeTab: HomeTab ->
                    DynamicHomeTab(
                        id = homeTab,
                        route = homeTab.route(),
                        order = homeTab.ordinal,
                        iconResId = homeTab.iconResId(),
                        titleResId = homeTab.titleResId,
                        isEnabled = true,
                        isUserDefault = homeTab.isUserDefault(
                            userDefaultHomeTab = userDefaultHomeTab,
                            hasBusinessPlan = hasBusinessPlan(userId).getOrNull(DriveLogTag.UI) ?: false,
                        ),
                    )
                }
                .ifNoDefaultMakeFirstDefault()
        }

    private fun HomeTab.route(): String = when (this) {
        HomeTab.FILES -> Screen.Files.route
        HomeTab.PHOTOS -> Screen.PhotosAndAlbums.route
        HomeTab.COMPUTERS -> Screen.Computers.route
        HomeTab.SHARED -> Screen.SharedTabs.route
    }

    private fun HomeTab.iconResId(): Int = when (this) {
        HomeTab.FILES -> CorePresentation.drawable.ic_proton_folder
        HomeTab.PHOTOS -> CorePresentation.drawable.ic_proton_image
        HomeTab.COMPUTERS -> CorePresentation.drawable.ic_proton_tv
        HomeTab.SHARED -> CorePresentation.drawable.ic_proton_users
    }

    private val HomeTab.titleResId: Int get() = when (this) {
        HomeTab.FILES -> I18N.string.title_files
        HomeTab.PHOTOS -> I18N.string.photos_title
        HomeTab.COMPUTERS -> I18N.string.computers_title
        HomeTab.SHARED -> I18N.string.title_shared
    }

    private fun List<DynamicHomeTab>.ifNoDefaultMakeFirstDefault(): List<DynamicHomeTab> =
        when (count { dynamicHomeTab -> dynamicHomeTab.isUserDefault }) {
            0 -> listOf(first().copy(isUserDefault = true)) + this.drop(1)
            1 -> this
            else -> error("""
                Unexpected number of default home tabs: ${this.filter { it.isUserDefault }.map { it.id }.joinToString()}
            """.trimIndent())
        }

    private fun HomeTab.isUserDefault(
        userDefaultHomeTab: HomeTab?,
        hasBusinessPlan: Boolean,
    ): Boolean = if (userDefaultHomeTab != null) {
        this == userDefaultHomeTab
    } else {
        if (hasBusinessPlan) {
            this == HomeTab.FILES
        } else {
            this == HomeTab.DEFAULT
        }
    }
}
