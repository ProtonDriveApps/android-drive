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

object Config {
    const val applicationId = "me.proton.android.drive"
    const val compileSdk = 35
    const val minSdk = 26
    const val targetSdk = 35
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val versionName = "2.28.0"
    const val archivesBaseName = "ProtonDrive-$versionName"
    val supportedResourceConfigurations = listOf(
        "b+es+419",
        "be",
        "ca",
        "cs",
        "da",
        "de",
        "es-rES",
        "en",
        "fi",
        "fr",
        "id",
        "in",
        "it",
        "ja",
        "ka",
        "ko",
        "nb-rNO",
        "nl",
        "pl",
        "pt-rBR",
        "ro",
        "ru",
        "sk",
        "sl",
        "sv-rSE",
        "tr",
        "uk",
        "zh-rTW",
    )
    val incubatingResourceConfigurations = listOf<String>().checkSupported()
    val resourceConfigurations = listOf(
        "en",
        // Tier-1
        "de",
        "fr",
        "nl",
        // Tier-2
        "es-rES",
        "b+es+419",
        "it",
        "pl",
        "pt-rBR",
        "ru",
        "tr",
        // Tier-3
        "be",
        "ca",
        "cs",
        "da",
        "fi",
        "id",
        "ja",
        "ka",
        "ko",
        "ro",
        "sk",
        "sl",
        "sv-rSE",
        "uk",
        "zh-rTW",
        // Tier-4
        "nb-rNO",
    ).checkSupported()

    private fun List<String>.checkSupported(): List<String> =
        onEach { language -> check(language in supportedResourceConfigurations) }
}
