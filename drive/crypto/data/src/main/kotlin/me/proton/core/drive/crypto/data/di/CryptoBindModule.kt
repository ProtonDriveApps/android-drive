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
package me.proton.core.drive.crypto.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.crypto.data.repository.DecryptedTextRepositoryImpl
import me.proton.core.drive.crypto.data.usecase.DecryptDataImpl
import me.proton.core.drive.crypto.data.usecase.EncryptDataImpl
import me.proton.core.drive.crypto.data.usecase.base.ReencryptKeyPacketImpl
import me.proton.core.drive.crypto.data.usecase.share.ReencryptSharePassphraseWithUrlPasswordImpl
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.crypto.domain.usecase.DecryptData
import me.proton.core.drive.crypto.domain.usecase.EncryptData
import me.proton.core.drive.crypto.domain.usecase.base.ReencryptKeyPacket
import me.proton.core.drive.crypto.domain.usecase.share.ReencryptSharePassphraseWithUrlPassword
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CryptoBindModule {
    @Binds
    @Singleton
    fun bindsReencryptKeyPacket(impl: ReencryptKeyPacketImpl): ReencryptKeyPacket

    @Binds
    @Singleton
    fun bindsReencryptSharePassphraseWithUrlPassword(
        impl: ReencryptSharePassphraseWithUrlPasswordImpl
    ): ReencryptSharePassphraseWithUrlPassword

    @Binds
    @Singleton
    fun bindsEncryptData(impl: EncryptDataImpl): EncryptData

    @Binds
    @Singleton
    fun bindsDecryptData(impl: DecryptDataImpl): DecryptData

    @Binds
    @Singleton
    fun bindsDecryptedTextRepositoryImpl(impl: DecryptedTextRepositoryImpl): DecryptedTextRepository
}
