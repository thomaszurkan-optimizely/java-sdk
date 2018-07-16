/**
 *
 * Copyright 2017, Optimizely and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.internal

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Experiment.ExperimentStatus
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.Condition
import org.junit.BeforeClass
import org.junit.Test

import java.io.IOException
import java.util.Collections

import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4
import com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_NATIONALITY_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_WITH_MISSING_VALUE_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY
import com.optimizely.ab.internal.ExperimentUtils.isExperimentActive
import com.optimizely.ab.internal.ExperimentUtils.isUserInExperiment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Test the Experiment Utils methods.
 */
class ExperimentUtilsTest {

    /**
     * If the [Experiment.status] is [ExperimentStatus.RUNNING],
     * then [ExperimentUtils.isExperimentActive] should return true.
     */
    @Test
    fun isExperimentActiveReturnsTrueWhenTheExperimentIsRunning() {
        val mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.RUNNING)

        assertTrue(isExperimentActive(mockExperiment))
    }

    /**
     * If the [Experiment.status] is [ExperimentStatus.LAUNCHED],
     * then [ExperimentUtils.isExperimentActive] should return true.
     */
    @Test
    fun isExperimentActiveReturnsTrueWhenTheExperimentIsLaunched() {
        val mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.LAUNCHED)

        assertTrue(isExperimentActive(mockExperiment))
    }

    /**
     * If the [Experiment.status] is [ExperimentStatus.PAUSED],
     * then [ExperimentUtils.isExperimentActive] should return false.
     */
    @Test
    fun isExperimentActiveReturnsFalseWhenTheExperimentIsPaused() {
        val mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.PAUSED)

        assertFalse(isExperimentActive(mockExperiment))
    }

    /**
     * If the [Experiment.status] is [ExperimentStatus.ARCHIVED],
     * then [ExperimentUtils.isExperimentActive] should return false.
     */
    @Test
    fun isExperimentActiveReturnsFalseWhenTheExperimentIsArchived() {
        val mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.ARCHIVED)

        assertFalse(isExperimentActive(mockExperiment))
    }

    /**
     * If the [Experiment.status] is [ExperimentStatus.NOT_STARTED],
     * then [ExperimentUtils.isExperimentActive] should return false.
     */
    @Test
    fun isExperimentActiveReturnsFalseWhenTheExperimentIsNotStarted() {
        val mockExperiment = makeMockExperimentWithStatus(ExperimentStatus.NOT_STARTED)

        assertFalse(isExperimentActive(mockExperiment))
    }

    /**
     * If the [Experiment] does not have any [Audience]s,
     * then [ExperimentUtils.isUserInExperiment] should return true;
     */
    @Test
    fun isUserInExperimentReturnsTrueIfExperimentHasNoAudiences() {
        val experiment = noAudienceProjectConfig!!.experiments[0]

        assertTrue(isUserInExperiment(noAudienceProjectConfig!!, experiment, emptyMap<String, String>()))
    }

    /**
     * If the [Experiment] contains at least one [Audience], but attributes is empty,
     * then [ExperimentUtils.isUserInExperiment] should return false.
     */
    @Test
    fun isUserInExperimentReturnsFalseIfExperimentHasAudiencesButUserHasNoAttributes() {
        val experiment = projectConfig!!.experiments[0]

        assertFalse(isUserInExperiment(projectConfig!!, experiment, emptyMap<String, String>()))
    }

    /**
     * If the attributes satisfies at least one [Condition] in an [Audience] of the [Experiment],
     * then [ExperimentUtils.isUserInExperiment] should return true.
     */
    @Test
    fun isUserInExperimentReturnsTrueIfUserSatisfiesAnAudience() {
        val experiment = projectConfig!!.experiments[0]
        val attributes = Collections.singletonMap("browser_type", "chrome")

        assertTrue(isUserInExperiment(projectConfig!!, experiment, attributes))
    }

    /**
     * If the attributes satisfies no [Condition] of any [Audience] of the [Experiment],
     * then [ExperimentUtils.isUserInExperiment] should return false.
     */
    @Test
    fun isUserInExperimentReturnsTrueIfUserDoesNotSatisfyAnyAudiences() {
        val experiment = projectConfig!!.experiments[0]
        val attributes = Collections.singletonMap("browser_type", "firefox")

        assertFalse(isUserInExperiment(projectConfig!!, experiment, attributes))
    }

    /**
     * If there are audiences with attributes on the experiment, but one of the attribute values is null,
     * they must explicitly pass in null in order for us to evaluate this. Otherwise we will say they do not match.
     */
    @Test
    fun isUserInExperimentHandlesNullValue() {
        val experiment = v4ProjectConfig!!.experimentKeyMapping[EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY]
        val satisfiesFirstCondition = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY,
                AUDIENCE_WITH_MISSING_VALUE_VALUE)
        val attributesWithNull = Collections.singletonMap<String, String>(ATTRIBUTE_NATIONALITY_KEY, null)
        val nonMatchingMap = Collections.singletonMap(ATTRIBUTE_NATIONALITY_KEY, "American")

        assertTrue(isUserInExperiment(v4ProjectConfig!!, experiment, satisfiesFirstCondition))
        assertTrue(isUserInExperiment(v4ProjectConfig!!, experiment, attributesWithNull))
        assertFalse(isUserInExperiment(v4ProjectConfig!!, experiment, nonMatchingMap))

        // It should explicitly be set to null otherwise we will return false on empty maps
        assertFalse(isUserInExperiment(v4ProjectConfig!!, experiment, emptyMap<String, String>()))
    }

    /**
     * Helper method to create an [Experiment] object with the provided status.
     *
     * @param status What the desired [Experiment.status] should be.
     * @return The newly created [Experiment].
     */
    private fun makeMockExperimentWithStatus(status: ExperimentStatus): Experiment {
        return Experiment("12345",
                "mockExperimentKey",
                status.toString(),
                "layerId",
                emptyList<String>(),
                emptyList<Variation>(),
                emptyMap<String, String>(),
                emptyList<TrafficAllocation>()
        )
    }

    companion object {

        private var projectConfig: ProjectConfig? = null
        private var noAudienceProjectConfig: ProjectConfig? = null
        private var v4ProjectConfig: ProjectConfig? = null

        @BeforeClass
        @Throws(IOException::class)
        fun setUp() {
            projectConfig = validProjectConfigV2()
            noAudienceProjectConfig = noAudienceProjectConfigV2()
            v4ProjectConfig = validProjectConfigV4()
        }
    }
}
