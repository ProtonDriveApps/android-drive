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

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.crypto.android.context.AndroidCryptoContext
import me.proton.core.crypto.android.srp.GOpenPGPSrpChallenge
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.srp.SrpChallenge
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.crypto.dagger.CoreCryptoModule
import me.proton.core.drive.test.crypto.FakeKeyStoreCrypto
import me.proton.core.drive.test.crypto.FakePGPCrypto
import me.proton.core.drive.test.crypto.FakeSrpChallenge
import me.proton.core.drive.test.crypto.FakeSrpCrypto
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreCryptoModule::class]
)
class TestCryptoModule {


    @Provides
    @Singleton
    fun provideKeyStoreCrypto(): KeyStoreCrypto = FakeKeyStoreCrypto()

    @Provides
    @Singleton
    fun provideCryptoContext(
        keyStoreCrypto: KeyStoreCrypto,
    ): CryptoContext =
        AndroidCryptoContext(keyStoreCrypto, FakePGPCrypto(), FakeSrpCrypto())

    @Provides
    @Singleton
    fun provideSrpCrypto(): SrpCrypto = FakeSrpCrypto()

    @Provides
    @Singleton
    public fun provideSrpChallenge(): SrpChallenge = FakeSrpChallenge()
}
