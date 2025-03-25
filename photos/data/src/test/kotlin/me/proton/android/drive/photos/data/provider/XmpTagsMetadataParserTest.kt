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

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class XmpTagsMetadataParserTest {

    private val parser = XmpTagsMetadataParser()

    @Test
    fun isMotionPhoto() = runTest {
        val metadata = parser(
            // language=xml
            """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/"
                    xmlns:xmpNote="http://ns.adobe.com/xmp/note/"
                    xmlns:Container="http://ns.google.com/photos/1.0/container/"
                    xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                    xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  hdrgm:Version="1.0"
                  xmpNote:HasExtendedXMP="3D818C645F34BD58948AEA5B1ACE4FF1"
                  GCamera:MotionPhoto="1"
                  GCamera:MotionPhotoVersion="1"
                  GCamera:MotionPhotoPresentationTimestampUs="679709">
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Semantic="Primary"
                          Item:Mime="image/jpeg"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Semantic="GainMap"
                          Item:Mime="image/jpeg"
                          Item:Length="97312"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="video/mp4"
                          Item:Semantic="MotionPhoto"
                          Item:Length="1522490"
                          Item:Padding="0"/>
                      </rdf:li>
                    </rdf:Seq>
                  </Container:Directory>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
        )

        assertTrue(metadata!!.isMotionPhoto)
    }

    @Test
    fun isPanorama() = runTest {
        val metadata = parser(
            // language=xml
            """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description xmlns:GImage="http://ns.google.com/photos/1.0/image/" xmlns:GPano="http://ns.google.com/photos/1.0/panorama/" xmlns:xmpNote="http://ns.adobe.com/xmp/note/" GImage:Mime="image/jpeg" GPano:CroppedAreaImageHeightPixels="1641" GPano:CroppedAreaImageWidthPixels="2714" GPano:CroppedAreaLeftPixels="0" GPano:CroppedAreaTopPixels="1085" GPano:FullPanoHeightPixels="4234" GPano:FullPanoWidthPixels="8468" GPano:InitialViewHeadingDegrees="57" GPano:InitialViewPitchDegrees="0" GPano:InitialViewRollDegrees="0" GPano:ProjectionType="equirectangular" rdf:about="" xmpNote:HasExtendedXMP="f061df7fb872797b62816d8506b2ba59"/>
              </rdf:RDF>
            </x:xmpmeta>

        """.trimIndent()
        )

        assertTrue(metadata!!.isPanorama)
    }

    @Test
    fun isPortraitGoogle() = runTest {
        val metadata = parser(
            // language=xml
            """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/"
                    xmlns:xmpNote="http://ns.adobe.com/xmp/note/"
                    xmlns:Container="http://ns.google.com/photos/1.0/container/"
                    xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                    xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  hdrgm:Version="1.0"
                  xmpNote:HasExtendedXMP="BD837BB85B379D7D2C64BC7C9AD1CDD9">
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Length="0"
                          Item:Semantic="Primary"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Length="24921"
                          Item:Semantic="GainMap"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Length="1920859"
                          Item:Semantic="Original"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Length="183525"
                          Item:Semantic="Depth"/>
                      </rdf:li>
                      <rdf:li rdf:parseType="Resource">
                        <Container:Item
                          Item:Mime="image/jpeg"
                          Item:Length="65693"
                          Item:Semantic="Confidence"/>
                      </rdf:li>
                    </rdf:Seq>
                  </Container:Directory>
                  <GCamera:SpecialTypeID>
                    <rdf:Bag>
                      <rdf:li>com.google.android.apps.camera.gallery.specialtype.SpecialType-PORTRAIT</rdf:li>
                    </rdf:Bag>
                  </GCamera:SpecialTypeID>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>

        """.trimIndent()
        )

        assertTrue(metadata!!.isPortrait)
    }
    @Test
    fun isPortraitXiaomi() = runTest {
        val metadata = parser(
            // language=xml
            """
                <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:MiCamera="http://ns.xiaomi.com/photos/1.0/camera/"
                      MiCamera:XMPMeta="&lt;?xml version='1.0' encoding='UTF-8' standalone='yes' ?&gt;&lt;depthmap version=&quot;3&quot; focuspoint=&quot;2234,1698&quot; blurlevel=&quot;56&quot; depthsize=&quot;0,0&quot; shinethreshold=&quot;5&quot; shinelevel=&quot;40&quot; rawlength=&quot;3477491&quot; depthlength=&quot;786812&quot; mimovie=&quot;false&quot; depthOrientation=&quot;90&quot; vendor=&quot;1&quot; portraitLightingVersion=&quot;2&quot; cameraPreferredMode=&quot;0&quot; bokehMappingVersion=&quot;2&quot; productName=&quot;zeus_global&quot; /&gt;"/>
                  </rdf:RDF>
                </x:xmpmeta>
            """.trimIndent()
        )

        assertTrue(metadata!!.isPortrait)
    }
}
