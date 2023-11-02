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

package me.proton.android.drive.verifier.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.verifier.data.api.VerifierApiDataSource
import me.proton.android.drive.verifier.data.factory.VerifierFactoryImpl
import me.proton.android.drive.verifier.domain.factory.VerifierFactory
import me.proton.core.drive.base.domain.usecase.GetCacheTempFolder
import me.proton.core.drive.crypto.domain.usecase.file.DecryptFiles
import me.proton.core.network.data.ApiProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VerifierModule {
    @Singleton
    @Provides
    fun provideVerifierApiDataSource(apiProvider: ApiProvider) =
        VerifierApiDataSource(apiProvider)

    @Singleton
    @Provides
    fun provideVerifierFactory(
        decryptFiles: DecryptFiles,
        getCacheTempFolder: GetCacheTempFolder,
    ): VerifierFactory =
        VerifierFactoryImpl(decryptFiles, getCacheTempFolder)
}
