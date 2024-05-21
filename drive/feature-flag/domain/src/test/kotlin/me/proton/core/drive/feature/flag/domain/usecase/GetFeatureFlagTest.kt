/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.feature.flag.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.coreFeatures
import me.proton.core.drive.test.api.featureFrontend
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetFeatureFlagTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getFeatureFlag: GetFeatureFlag

    @Inject
    lateinit var featureFlagRepository: FeatureFlagRepository

    private val id = FeatureFlagId(userId = userId, id = "feature-a")
    private val idInDevelopment = FeatureFlagId(userId = userId, id = "feature-b",)

    @Before
    fun setUp() = runTest {
        driveRule.db.myFiles {  }
        driveRule.server.run {
            coreFeatures()
        }
        // Fragile: a method from core repository needs to be called
        // for /core/v4/features to be called and for missing ids to be inserted in database
        featureFlagRepository
            .observe(userId, FeatureId(id.id))
            .take(2)
            .launchIn(this)

        FeatureFlagId.developments = listOf(idInDevelopment.id)
    }

    @Test
    fun enabled() = runTest {
        driveRule.server.run {
            featureFrontend(id.id)
        }

        val featureFlag = getFeatureFlag(id)

        assertEquals(FeatureFlag(this@GetFeatureFlagTest.id, FeatureFlag.State.ENABLED), featureFlag)
    }

    @Test
    fun not_found() = runTest {
        driveRule.server.run {
            featureFrontend()
        }

        val featureFlag = getFeatureFlag(id)

        assertEquals(FeatureFlag(this@GetFeatureFlagTest.id, FeatureFlag.State.NOT_FOUND), featureFlag)
    }

    @Test
    fun not_found_development() = runTest {

        driveRule.server.run {
            featureFrontend(idInDevelopment.id)
        }

        val featureFlag = getFeatureFlag(idInDevelopment)

        assertEquals(FeatureFlag(idInDevelopment, FeatureFlag.State.NOT_FOUND), featureFlag)
    }

}
