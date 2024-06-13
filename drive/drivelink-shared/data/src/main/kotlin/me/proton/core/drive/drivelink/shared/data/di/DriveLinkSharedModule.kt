/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.drivelink.shared.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.drivelink.shared.data.repository.DriveLinkSharedRepositoryImpl
import me.proton.core.drive.drivelink.shared.data.usecase.GetPagedSharedByMeLinkIdsImpl
import me.proton.core.drive.drivelink.shared.data.usecase.GetPagedSharedWithMeLinkIdsImpl
import me.proton.core.drive.drivelink.shared.domain.repository.DriveLinkSharedRepository
import me.proton.core.drive.drivelink.shared.domain.usecase.GetPagedSharedByMeLinkIds
import me.proton.core.drive.drivelink.shared.domain.usecase.GetPagedSharedWithMeLinkIds

@Module
@InstallIn(SingletonComponent::class)
interface DriveLinkSharedModule {

    @Binds
    fun bindsRepository(repository: DriveLinkSharedRepositoryImpl): DriveLinkSharedRepository

    @Binds
    fun bindGetPagedSharedWithMeLinkIds(
        getPagedSharedWithMeLinkIdsImpl: GetPagedSharedWithMeLinkIdsImpl
    ): GetPagedSharedWithMeLinkIds

    @Binds
    fun bindGetPagedSharedByMeLinkIds(
        getPagedSharedByMeLinkIdsImpl: GetPagedSharedByMeLinkIdsImpl
    ): GetPagedSharedByMeLinkIds
}
