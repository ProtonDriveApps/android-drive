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

import groovy.xml.slurpersupport.Node
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File
import java.util.Locale

fun Project.configureJacoco(flavor: String = "", srcFolder: String = "kotlin") {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = extensions.getByType<VersionCatalogsExtension>().named("libs").findVersion("jaCoCo").get().requiredVersion
    }

    val taskName = if (flavor.isEmpty()) {
        "debug"
    } else {
        "${flavor}Debug"
    }

    tasks.withType<org.gradle.api.tasks.testing.Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    tasks.create<JacocoReport>("jacocoTestReport") {

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        val fileFilter = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "ch.protonmail.android.utils.nativelib",
            "**/ch/protonmail/**",
        )

        val debugTree = fileTree("$buildDir/tmp/kotlin-classes/$taskName") { exclude(fileFilter) }
        val mainSrc = "$projectDir/src/main/$srcFolder"

        sourceDirectories.setFrom(mainSrc)
        classDirectories.setFrom(debugTree)
        executionData.setFrom(fileTree(buildDir) { include(listOf("**/*.exec", "**/*.ec")) })
    }.dependsOn("test${taskName.capitalize(Locale.ENGLISH)}UnitTest")

    tasks.register("coverageReport") {
        dependsOn("jacocoTestReport")
        val reportFile = project.file("$buildDir/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        inputs.files(reportFile).withPropertyName("reportFile")
        onlyIf { reportFile.exists() }
        doLast {
            if (!reportFile.exists()) {
                println("${reportFile.path} doesn't exists, skipping coverageReport")
                return@doLast
            }
            val slurper = groovy.xml.XmlSlurper()
            slurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            slurper.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
            )
            val xml = slurper.parse(reportFile)
            val counter = (xml.childNodes().asSequence().first {
                (it as? Node)?.name() == "counter" && it.attributes()["type"] == "INSTRUCTION"
            } as Node)
            val missed = (counter.attributes()["missed"] as String).toLong()
            val covered = (counter.attributes()["covered"] as String).toLong()
            val total = missed + covered
            val percentage = (covered.toFloat() / total * 100).toInt()

            println("Missed %d branches".format(missed))
            println("Covered %d branches".format(covered))
            println("Total %d%%".format(percentage))
        }
    }

    tasks.register<Exec>("coberturaCoverageReport") {
        dependsOn("coverageReport")
        val jacocoFile = project.file("$buildDir/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        val coberturaFile = project.file( "$buildDir/reports/cobertura-coverage.xml")
        inputs.file(jacocoFile).withPropertyName("jacocoFile")
        outputs.file(coberturaFile)
        workingDir = File(rootDir, "buildSrc")
        commandLine(
            "python3",
            "jacocoConverter.py",
            jacocoFile.path,
            "$projectDir/src/main/$srcFolder"
        )
        onlyIf { jacocoFile.exists() }
        doFirst {
            standardOutput = coberturaFile.outputStream()
        }
    }
}
