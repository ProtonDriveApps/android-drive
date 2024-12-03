package me.proton.android.drive.ui.rules

import kotlinx.coroutines.runBlocking
import me.proton.android.drive.repository.TestFeatureFlagRepositoryImpl
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.annotation.annotationTestData
import me.proton.android.drive.ui.extension.getFeatureFlagAnnotations
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
            featureFlagAnnotations.map { featureFlagAnnotation ->
                TestFeatureFlagRepositoryImpl.flags[featureFlagAnnotation.id] =
                    featureFlagAnnotation.state
            }
        }
    }

    override fun finished(description: Description) {
        TestFeatureFlagRepositoryImpl.flags.clear()
    }
}
