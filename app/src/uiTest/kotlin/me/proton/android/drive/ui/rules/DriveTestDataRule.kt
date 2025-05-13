package me.proton.android.drive.ui.rules

import kotlinx.coroutines.runBlocking
import me.proton.android.drive.repository.TestFeatureFlagRepositoryImpl
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.annotation.annotationTestData
import me.proton.android.drive.ui.extension.getFeatureFlagAnnotations
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ONE_DOLLAR_PLAN_UPSELL
import me.proton.core.test.rule.annotation.AnnotationTestData
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.reflect.full.findAnnotations

class DriveTestDataRule : TestWatcher() {

    private var scenarioAnnotations: List<Scenario> = arrayListOf()
    var scenarioAnnotationTestData: MutableSet<AnnotationTestData<Scenario>> =
        mutableSetOf()

    override fun starting(description: Description) {
        runBlocking {
            val method = description.testClass.kotlin.members
                .firstOrNull {
                    it.name == description.methodName
                            // Handle Parametrized test cases.
                            || description.methodName.contains("${it.name}[")
                }
            scenarioAnnotations = method?.findAnnotations<Scenario>().orEmpty()
            scenarioAnnotations.forEach { annotation ->
                scenarioAnnotationTestData.add(annotation.annotationTestData)
            }

            val featureFlagAnnotations = description.getFeatureFlagAnnotations()
            (defaultFlags + featureFlagAnnotations.map { featureFlagAnnotation ->
                featureFlagAnnotation.id to featureFlagAnnotation.state
            }).onEach { (key, value) ->
                TestFeatureFlagRepositoryImpl.flags[key] = value
            }
        }
    }

    override fun finished(description: Description) {
        TestFeatureFlagRepositoryImpl.flags.clear()
    }

    private companion object {
        val defaultFlags = mapOf(
            DRIVE_ONE_DOLLAR_PLAN_UPSELL to NOT_FOUND
        )
    }
}
