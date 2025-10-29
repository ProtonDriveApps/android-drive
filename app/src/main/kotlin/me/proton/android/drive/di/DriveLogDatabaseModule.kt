/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.log.data.db.DriveLogDatabase
import me.proton.core.drive.log.data.db.LogDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveLogDatabaseModule {

    @Provides
    @Singleton
    fun provideDriveLogDatabase(@ApplicationContext context: Context): DriveLogDatabase =
        DriveLogDatabase.buildDatabase(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveLogDatabaseBindsModule {

    @Binds
    abstract fun provideLogDatabase(db: DriveLogDatabase): LogDatabase
}