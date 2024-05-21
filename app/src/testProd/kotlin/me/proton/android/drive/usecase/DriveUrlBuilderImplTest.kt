package me.proton.android.drive.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import me.proton.android.drive.provider.BuildConfigurationProvider
import me.proton.android.drive.settings.DebugSettings
import me.proton.core.configuration.EnvironmentConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DriveUrlBuilderImplTest {

    @Test
    fun test() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val staticEnvironmentConfig = EnvironmentConfiguration.fromClass()
        val builder = DriveUrlBuilderImpl(
            DebugSettings(
                context,
                BuildConfigurationProvider(staticEnvironmentConfig)
            )
        )

        val url = builder {}

        assertEquals("https://drive.proton.me", url)
    }

}
