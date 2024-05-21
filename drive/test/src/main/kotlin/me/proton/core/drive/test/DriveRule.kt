/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import me.proton.android.drive.db.DriveDatabase
import me.proton.core.drive.test.api.DriveDispatcher
import me.proton.core.drive.test.log.PrintStreamLogger
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.DispatcherProvider
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalCoroutinesApi::class)
class DriveRule(testInstance: Any) : ExternalResource() {

    lateinit var db: DriveDatabase
    val server = MockWebServer()
    private var hiltRule = HiltAndroidRule(testInstance)

    override fun apply(base: Statement, description: Description): Statement {
        val statement = super.apply(base, description)
        return hiltRule.apply(statement, description)
    }

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CoreLogger.set(PrintStreamLogger())

        server.start()
        server.dispatcher = DriveDispatcher()
        TestConfigurationProvider.testBaseUrl = server.url("/").toString()

        hiltRule.inject()
        with(EntryPointAccessors.fromApplication<DriveRuleEntryPoint>(context)) {
            db = driveDatabase
            Dispatchers.setMain(dispatcherProvider.Main)
        }
    }

    override fun after() {
        db.close()
        server.close()
        Dispatchers.resetMain()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DriveRuleEntryPoint {
        val driveDatabase: DriveDatabase
        val dispatcherProvider: DispatcherProvider
    }
}
