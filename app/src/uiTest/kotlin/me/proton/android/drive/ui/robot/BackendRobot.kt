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

package me.proton.android.drive.ui.robot

import me.proton.test.fusion.FusionConfig.targetContext
import java.util.Locale

object BackendRobot {
    private val language: String get() = targetContext.resources.configuration.locales.get(0).language

    // Tier-1 languages (plus English)
    private val FRENCH = Locale.FRENCH.language
    private val GERMAN = Locale.GERMAN.language

    val nameAlreadyExist: String
        get() = when (language) {
            FRENCH -> "Un fichier ou un dossier portant ce nom existe déjà."
            GERMAN -> "Datei oder Ordner mit diesem Namen existiert bereits."
            else -> "A file or folder with that name already exists"
        }
    val linkNoLongerAvailableParentDeleted: String
        get() = when (language) {
            FRENCH -> "Ce fichier ou dossier n'est plus disponible. Un dossier dans lequel il se trouvait a été supprimé."
            GERMAN -> "Diese Datei oder Ordner ist nicht mehr verfügbar. Ein übergeordneter Ordner wurde gelöscht."
            else -> "This file or folder is no longer available. A parent folder has been deleted."
        }
}
