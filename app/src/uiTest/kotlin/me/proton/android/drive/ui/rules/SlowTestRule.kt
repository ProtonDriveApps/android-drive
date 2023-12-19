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

package me.proton.android.drive.ui.rules

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.ui.annotation.Slow
import me.proton.core.util.kotlin.CoreLogger
import me.proton.test.fusion.FusionConfig
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Sets a delay before every action or .await { assertion }
 * Only use for not critical, automation-only reproducible tests
 */
class SlowTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        description.getAnnotation(Slow::class.java)?.let {
            FusionConfig.Compose.before = {
                runBlocking {
                    CoreLogger.i(DriveLogTag.UI_TEST, "Delaying UI action by ${it.delayMs}ms")
                    delay(it.delayMs.toDuration(DurationUnit.MILLISECONDS))
                }
            }
        }
        return base
    }
}
