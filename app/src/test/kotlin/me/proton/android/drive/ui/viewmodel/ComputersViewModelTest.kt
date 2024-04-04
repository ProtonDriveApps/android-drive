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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.usecase.RefreshDevices
import me.proton.core.drive.drivelink.device.domain.usecase.GetDecryptedDevicesSortedByName
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import me.proton.core.test.kotlin.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import me.proton.core.drive.drivelink.device.presentation.R as DriveLinkDevicePresentation
import me.proton.core.drive.i18n.R as I18N

@RunWith(RobolectricTestRunner::class)
class ComputersViewModelTest {
    private lateinit var computersViewModel: ComputersViewModel
    private val getDevices: GetDecryptedDevicesSortedByName = mockk<GetDecryptedDevicesSortedByName>()
    private val savedStateHandle: SavedStateHandle = mockk<SavedStateHandle>()
    private val refreshDevices: RefreshDevices = mockk<RefreshDevices>()
    private val broadcastMessages: BroadcastMessages = mockk<BroadcastMessages>()
    private val configurationProvider: ConfigurationProvider = mockk<ConfigurationProvider>()
    private val shouldUpgradeStorage: ShouldUpgradeStorage = mockk<ShouldUpgradeStorage>()

    @Before
    fun setup() {
        coEvery { savedStateHandle.get<String>(any()) } returns "saved_state_handle_key_value"
        coEvery { getDevices.invoke(any()) } returns flowOf(emptyList<Device>().asSuccess)
        coEvery { shouldUpgradeStorage() } returns flowOf(ShouldUpgradeStorage.Result.NoUpgrade)

        computersViewModel = ComputersViewModel(
            appContext = ApplicationProvider.getApplicationContext(),
            getDevices = getDevices,
            savedStateHandle = savedStateHandle,
            refreshDevices = refreshDevices,
            broadcastMessages = broadcastMessages,
            configurationProvider = configurationProvider,
            shouldUpgradeStorage = shouldUpgradeStorage,
        )
    }

    @Test
    fun `empty computers tab resources check`() = runTest {
        // Given
        launch {
            computersViewModel.devices.collect()
        }

        // When
        val viewState = computersViewModel
            .viewState
            .filterNot { viewState -> viewState.listContentState is ListContentState.Loading }
            .first()

        // Then
        val emptyState = viewState.listContentState as ListContentState.Empty
        assertEquals(
            DriveLinkDevicePresentation.drawable.empty_devices_daynight,
            emptyState.imageResId,
        ) { "Wrong empty image resource" }
        assertEquals(
            I18N.string.computers_empty_title,
            emptyState.titleId,
        ) { "Wrong empty title resource" }
        assertEquals(
            I18N.string.computers_empty_description,
            emptyState.descriptionResId,
        ) { "Wrong empty description resource" }
    }
}
