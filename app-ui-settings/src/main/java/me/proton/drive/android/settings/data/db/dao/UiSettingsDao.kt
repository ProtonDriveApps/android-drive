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

package me.proton.drive.android.settings.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.drive.android.settings.data.db.entity.UiSettingsEntity
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.entity.ThemeStyle

@Dao
abstract class UiSettingsDao : BaseDao<UiSettingsEntity>() {

    @Query("SELECT * FROM UiSettingsEntity WHERE user_id = :userId")
    abstract fun getFlow(userId: UserId): Flow<UiSettingsEntity?>

    @Query("UPDATE UiSettingsEntity SET layout_type = :layoutType WHERE user_id = :userId")
    abstract suspend fun updateLayoutType(userId: UserId, layoutType: LayoutType): Int

    @Query("UPDATE UiSettingsEntity SET theme_style = :themeStyle WHERE user_id = :userId")
    abstract suspend fun updateThemeStyle(userId: UserId, themeStyle: ThemeStyle): Int
}
