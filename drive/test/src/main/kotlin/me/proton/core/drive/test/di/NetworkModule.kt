/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.test.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.test.network.TestDriveApiClient
import me.proton.core.network.data.client.ExtraHeaderProviderImpl
import me.proton.core.network.data.di.AlternativeApiPins
import me.proton.core.network.data.di.BaseProtonApiUrl
import me.proton.core.network.data.di.CertificatePins
import me.proton.core.network.data.di.Constants
import me.proton.core.network.data.di.DohProviderUrls
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.network.domain.serverconnection.DohAlternativesListener
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @BaseProtonApiUrl
    fun provideProtonApiUrl(configurationProvider: ConfigurationProvider): HttpUrl =
        configurationProvider.baseUrl.toHttpUrl()

    @Provides
    @Singleton
    fun provideExtraHeaderProvider(): ExtraHeaderProvider = ExtraHeaderProviderImpl()

    @DohProviderUrls
    @Provides
    fun provideDohProviderUrls(): Array<String> = Constants.DOH_PROVIDERS_URLS

    @CertificatePins
    @Provides
    fun provideCertificatePins(): Array<String> = emptyArray()


    @AlternativeApiPins
    @Provides
    fun provideAlternativeApiPins(): List<String> = emptyList()

    @Provides
    @Singleton
    fun provideDohAlternativesListener(): DohAlternativesListener? = null
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindModule {

    @Binds
    abstract fun bindApiClient(testDriveApiClient: TestDriveApiClient): ApiClient
}
