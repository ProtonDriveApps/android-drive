/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.data.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.util.kotlin.CoreLogger
import org.w3c.dom.Document
import javax.inject.Inject
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


internal class XmpTagsMetadataParser @Inject constructor() {
    suspend operator fun invoke(xmp: String): XmpTagsMetadata? = withContext(Dispatchers.IO) {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val builder = factory.newDocumentBuilder()
        builder.parse(xmp.byteInputStream()).apply {
            documentElement.normalize()
        }
    }?.let { document ->
        XmpTagsMetadata(document)
    }
}

internal class XmpTagsMetadata(val document: Document) {
    val isMotionPhoto: Boolean
        get() = getXMPProperty("//@GCamera:MotionPhoto") == "1"
    val isPanorama: Boolean
        get() = getXMPProperty("//@GPano:ProjectionType") != null
    val isPortrait: Boolean
        get() = getXMPProperty("//GCamera:SpecialTypeID[1]/*") ==
                "com.google.android.apps.camera.gallery.specialtype.SpecialType-PORTRAIT"
                || getXMPProperty("//@MiCamera:XMPMeta")
                    .orEmpty().contains("<depthmap")

    private fun getXMPProperty(xPathExpression: String): String? {
        val xPath = XPathFactory.newInstance().newXPath()
        xPath.namespaceContext = XMPNamespaceContext()
        val result = xPath.evaluate(xPathExpression, document, XPathConstants.STRING) as String
        return result.trim().ifEmpty { null }?.also {
            CoreLogger.d(UPLOAD, "Found expression: $xPathExpression in xmp (value: $it)")
        }
    }
}

private class XMPNamespaceContext : NamespaceContext {
    private val namespaces = mapOf(
        "GCamera" to "http://ns.google.com/photos/1.0/camera/",
        "GPano" to "http://ns.google.com/photos/1.0/panorama/",
        "MiCamera" to "http://ns.xiaomi.com/photos/1.0/camera/",
    )

    override fun getNamespaceURI(prefix: String): String? {
        return namespaces[prefix]
    }

    override fun getPrefix(namespaceURI: String): String? {
        return namespaces.entries.find { it.value == namespaceURI }?.key
    }

    override fun getPrefixes(namespaceURI: String): Iterator<String>? {
        return namespaces.entries.filter { it.value == namespaceURI }
            .map { it.key }
            .takeUnless { it.isEmpty() }?.iterator()
    }
}
