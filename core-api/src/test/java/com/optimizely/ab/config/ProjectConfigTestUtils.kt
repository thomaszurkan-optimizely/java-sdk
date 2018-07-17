/**
 *
 * Copyright 2016-2017, Optimizely and contributors
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
package com.optimizely.ab.config

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.optimizely.ab.config.audience.AndCondition
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.config.audience.NotCondition
import com.optimizely.ab.config.audience.OrCondition
import com.optimizely.ab.config.audience.UserAttribute
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import java.util.Arrays.asList
import java.util.Collections.singletonList
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat

/**
 * Helper class that provides common functionality and resources for testing [ProjectConfig].
 */
object ProjectConfigTestUtils {

    private val VALID_PROJECT_CONFIG_V2 = generateValidProjectConfigV2()
    private fun generateValidProjectConfigV2(): ProjectConfig {
        val experiments = asList(
                Experiment("223", "etag1", "Running", "1",
                        listOf<String>("100"),
                        asList(Variation("276", "vtag1"),
                                Variation("277", "vtag2")),
                        Collections.singletonMap("testUser1", "vtag1"),
                        asList(TrafficAllocation("276", 3500),
                                TrafficAllocation("277", 9000)),
                        ""),
                Experiment("118", "etag2", "Not started", "2",
                        listOf<String>("100"),
                        asList(Variation("278", "vtag3"),
                                Variation("279", "vtag4")),
                        Collections.singletonMap("testUser3", "vtag3"),
                        asList(TrafficAllocation("278", 4500),
                                TrafficAllocation("279", 9000)),
                        ""),
                Experiment("119", "etag3", "Not started", null,
                        listOf<String>("100"),
                        asList(Variation("280", "vtag5"),
                                Variation("281", "vtag6")),
                        Collections.singletonMap("testUser4", "vtag5"),
                        asList(TrafficAllocation("280", 4500),
                                TrafficAllocation("281", 9000)),
                        "")
        )

        val attributes = listOf<Attribute>(Attribute("134", "browser_type"))

        val singleExperimentId = listOf<String>("223")
        val multipleExperimentIds = asList("118", "223")
        val events = asList(EventType("971", "clicked_cart", singleExperimentId),
                EventType("098", "Total Revenue", singleExperimentId),
                EventType("099", "clicked_purchase", multipleExperimentIds),
                EventType("100", "no_running_experiments", listOf<String>("118")))

        val userAttributes = ArrayList<Condition>()
        userAttributes.add(UserAttribute("browser_type", "custom_dimension", "firefox"))

        val orInner = OrCondition(userAttributes)

        val notCondition = NotCondition(orInner)
        val outerOrList = ArrayList<Condition>()
        outerOrList.add(notCondition)

        val orOuter = OrCondition(outerOrList)
        val andList = ArrayList<Condition>()
        andList.add(orOuter)

        val andCondition = AndCondition(andList)

        val audiences = listOf<Audience>(Audience("100", "not_firefox_users", andCondition))

        val userIdToVariationKeyMap = HashMap<String, String>()
        userIdToVariationKeyMap.put("testUser1", "e1_vtag1")
        userIdToVariationKeyMap.put("testUser2", "e1_vtag2")

        val randomGroupExperiments = asList(
                Experiment("301", "group_etag2", "Running", "3",
                        listOf<String>("100"),
                        asList(Variation("282", "e2_vtag1"),
                                Variation("283", "e2_vtag2")),
                        emptyMap<String, String>(),
                        asList(TrafficAllocation("282", 5000),
                                TrafficAllocation("283", 10000)),
                        "42"),
                Experiment("300", "group_etag1", "Running", "4",
                        listOf<String>("100"),
                        asList(Variation("280", "e1_vtag1"),
                                Variation("281", "e1_vtag2")),
                        userIdToVariationKeyMap,
                        asList(TrafficAllocation("280", 3000),
                                TrafficAllocation("281", 10000)),
                        "42")
        )

        val overlappingGroupExperiments = asList(
                Experiment("302", "overlapping_etag1", "Running", "5",
                        listOf<String>("100"),
                        asList(Variation("284", "e1_vtag1"),
                                Variation("285", "e1_vtag2")),
                        userIdToVariationKeyMap,
                        asList(TrafficAllocation("284", 1500),
                                TrafficAllocation("285", 3000)),
                        "43")
        )

        val randomPolicyGroup = Group("42", "random",
                randomGroupExperiments,
                asList(TrafficAllocation("300", 3000),
                        TrafficAllocation("301", 9000),
                        TrafficAllocation("", 10000)))
        val overlappingPolicyGroup = Group("43", "overlapping",
                overlappingGroupExperiments,
                emptyList<TrafficAllocation>())
        val groups = asList(randomPolicyGroup, overlappingPolicyGroup)

        return ProjectConfig("789", "1234", "2", "42", groups, experiments, attributes, events, audiences)
    }

    private val NO_AUDIENCE_PROJECT_CONFIG_V2 = generateNoAudienceProjectConfigV2()
    private fun generateNoAudienceProjectConfigV2(): ProjectConfig {
        val userIdToVariationKeyMap = HashMap<String, String>()
        userIdToVariationKeyMap.put("testUser1", "vtag1")
        userIdToVariationKeyMap.put("testUser2", "vtag2")

        val experiments = asList(
                Experiment("223", "etag1", "Running", "1",
                        emptyList<String>(),
                        asList(Variation("276", "vtag1"),
                                Variation("277", "vtag2")),
                        userIdToVariationKeyMap,
                        asList(TrafficAllocation("276", 3500),
                                TrafficAllocation("277", 9000)),
                        ""),
                Experiment("118", "etag2", "Not started", "2",
                        emptyList<String>(),
                        asList(Variation("278", "vtag3"),
                                Variation("279", "vtag4")),
                        emptyMap<String, String>(),
                        asList(TrafficAllocation("278", 4500),
                                TrafficAllocation("279", 9000)),
                        ""),
                Experiment("119", "etag3", "Launched", "3",
                        emptyList<String>(),
                        asList(Variation("280", "vtag5"),
                                Variation("281", "vtag6")),
                        emptyMap<String, String>(),
                        asList(TrafficAllocation("280", 5000),
                                TrafficAllocation("281", 10000)),
                        "")
        )

        val attributes = listOf<Attribute>(Attribute("134", "browser_type"))

        val singleExperimentId = listOf<String>("223")
        val multipleExperimentIds = asList("118", "223")
        val events = asList(
                EventType("971", "clicked_cart", singleExperimentId),
                EventType("098", "Total Revenue", singleExperimentId),
                EventType("099", "clicked_purchase", multipleExperimentIds),
                EventType("100", "launched_exp_event", listOf<String>("119")),
                EventType("101", "event_with_launched_and_running_experiments", Arrays.asList("119", "223"))
        )

        return ProjectConfig("789", "1234", "2", "42", emptyList<Group>(), experiments, attributes,
                events, emptyList<Audience>())
    }

    private val VALID_PROJECT_CONFIG_V3 = generateValidProjectConfigV3()
    private fun generateValidProjectConfigV3(): ProjectConfig {
        val variationVtag1VariableUsageInstances = asList(
                LiveVariableUsageInstance("6", "True"),
                LiveVariableUsageInstance("2", "10"),
                LiveVariableUsageInstance("3", "string_var_vtag1"),
                LiveVariableUsageInstance("4", "5.3")
        )

        val variationVtag2VariableUsageInstances = asList(
                LiveVariableUsageInstance("6", "False"),
                LiveVariableUsageInstance("2", "20"),
                LiveVariableUsageInstance("3", "string_var_vtag2"),
                LiveVariableUsageInstance("4", "6.3")
        )

        val experiments = asList(
                Experiment("223", "etag1", "Running", "1",
                        listOf<String>("100"),
                        asList(Variation("276", "vtag1", variationVtag1VariableUsageInstances),
                                Variation("277", "vtag2", variationVtag2VariableUsageInstances)),
                        Collections.singletonMap("testUser1", "vtag1"),
                        asList(TrafficAllocation("276", 3500),
                                TrafficAllocation("277", 9000)),
                        ""),
                Experiment("118", "etag2", "Not started", "2",
                        listOf<String>("100"),
                        asList(Variation("278", "vtag3", emptyList<LiveVariableUsageInstance>()),
                                Variation("279", "vtag4", emptyList<LiveVariableUsageInstance>())),
                        Collections.singletonMap("testUser3", "vtag3"),
                        asList(TrafficAllocation("278", 4500),
                                TrafficAllocation("279", 9000)),
                        "")
        )

        val attributes = listOf<Attribute>(Attribute("134", "browser_type"))

        val singleExperimentId = listOf<String>("223")
        val multipleExperimentIds = asList("118", "223")
        val events = asList(EventType("971", "clicked_cart", singleExperimentId),
                EventType("098", "Total Revenue", singleExperimentId),
                EventType("099", "clicked_purchase", multipleExperimentIds),
                EventType("100", "no_running_experiments", listOf<String>("118")))

        val userAttributes = ArrayList<Condition>()
        userAttributes.add(UserAttribute("browser_type", "custom_dimension", "firefox"))

        val orInner = OrCondition(userAttributes)

        val notCondition = NotCondition(orInner)
        val outerOrList = ArrayList<Condition>()
        outerOrList.add(notCondition)

        val orOuter = OrCondition(outerOrList)
        val andList = ArrayList<Condition>()
        andList.add(orOuter)

        val andCondition = AndCondition(andList)

        val audiences = listOf<Audience>(Audience("100", "not_firefox_users", andCondition))

        val userIdToVariationKeyMap = HashMap<String, String>()
        userIdToVariationKeyMap.put("testUser1", "e1_vtag1")
        userIdToVariationKeyMap.put("testUser2", "e1_vtag2")

        val randomGroupExperiments = asList(
                Experiment("301", "group_etag2", "Running", "3",
                        listOf<String>("100"),
                        asList(Variation("282", "e2_vtag1", emptyList<LiveVariableUsageInstance>()),
                                Variation("283", "e2_vtag2", emptyList<LiveVariableUsageInstance>())),
                        emptyMap<String, String>(),
                        asList(TrafficAllocation("282", 5000),
                                TrafficAllocation("283", 10000)),
                        "42"),
                Experiment("300", "group_etag1", "Running", "4",
                        listOf<String>("100"),
                        asList(Variation("280", "e1_vtag1",
                                listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance("7", "True"))),
                                Variation("281", "e1_vtag2",
                                        listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance("7", "False")))),
                        userIdToVariationKeyMap,
                        asList(TrafficAllocation("280", 3000),
                                TrafficAllocation("281", 10000)),
                        "42")
        )

        val overlappingGroupExperiments = asList(
                Experiment("302", "overlapping_etag1", "Running", "5",
                        listOf<String>("100"),
                        asList(Variation("284", "e1_vtag1", emptyList<LiveVariableUsageInstance>()),
                                Variation("285", "e1_vtag2", emptyList<LiveVariableUsageInstance>())),
                        userIdToVariationKeyMap,
                        asList(TrafficAllocation("284", 1500),
                                TrafficAllocation("285", 3000)),
                        "43")
        )

        val randomPolicyGroup = Group("42", "random",
                randomGroupExperiments,
                asList(TrafficAllocation("300", 3000),
                        TrafficAllocation("301", 9000),
                        TrafficAllocation("", 10000)))
        val overlappingPolicyGroup = Group("43", "overlapping",
                overlappingGroupExperiments,
                emptyList<TrafficAllocation>())
        val groups = asList(randomPolicyGroup, overlappingPolicyGroup)

        val liveVariables = asList(
                LiveVariable("1", "boolean_variable", "False", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.BOOLEAN),
                LiveVariable("2", "integer_variable", "5", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.INTEGER),
                LiveVariable("3", "string_variable", "string_live_variable", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.STRING),
                LiveVariable("4", "double_variable", "13.37", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.DOUBLE),
                LiveVariable("5", "archived_variable", "True", LiveVariable.VariableStatus.ARCHIVED,
                        LiveVariable.VariableType.BOOLEAN),
                LiveVariable("6", "etag1_variable", "False", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.BOOLEAN),
                LiveVariable("7", "group_etag1_variable", "False", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.BOOLEAN),
                LiveVariable("8", "unused_string_variable", "unused_variable", LiveVariable.VariableStatus.ACTIVE,
                        LiveVariable.VariableType.STRING)
        )

        return ProjectConfig("789", "1234", "3", "42", groups, experiments, attributes, events, audiences,
                true, liveVariables)
    }

    private val NO_AUDIENCE_PROJECT_CONFIG_V3 = generateNoAudienceProjectConfigV3()
    private fun generateNoAudienceProjectConfigV3(): ProjectConfig {
        val userIdToVariationKeyMap = HashMap<String, String>()
        userIdToVariationKeyMap.put("testUser1", "vtag1")
        userIdToVariationKeyMap.put("testUser2", "vtag2")

        val experiments = asList(
                Experiment("223", "etag1", "Running", "1",
                        emptyList<String>(),
                        asList(Variation("276", "vtag1", emptyList<LiveVariableUsageInstance>()),
                                Variation("277", "vtag2", emptyList<LiveVariableUsageInstance>())),
                        userIdToVariationKeyMap,
                        asList(TrafficAllocation("276", 3500),
                                TrafficAllocation("277", 9000)),
                        ""),
                Experiment("118", "etag2", "Not started", "2",
                        emptyList<String>(),
                        asList(Variation("278", "vtag3", emptyList<LiveVariableUsageInstance>()),
                                Variation("279", "vtag4", emptyList<LiveVariableUsageInstance>())),
                        emptyMap<String, String>(),
                        asList(TrafficAllocation("278", 4500),
                                TrafficAllocation("279", 9000)),
                        ""),
                Experiment("119", "etag3", "Launched", "3",
                        emptyList<String>(),
                        asList(Variation("280", "vtag5"),
                                Variation("281", "vtag6")),
                        emptyMap<String, String>(),
                        asList(TrafficAllocation("280", 5000),
                                TrafficAllocation("281", 10000)),
                        "")
        )

        val attributes = listOf<Attribute>(Attribute("134", "browser_type"))

        val singleExperimentId = listOf<String>("223")
        val multipleExperimentIds = asList("118", "223")
        val events = asList(
                EventType("971", "clicked_cart", singleExperimentId),
                EventType("098", "Total Revenue", singleExperimentId),
                EventType("099", "clicked_purchase", multipleExperimentIds),
                EventType("100", "launched_exp_event", listOf<String>("119")),
                EventType("101", "event_with_launched_and_running_experiments", Arrays.asList("119", "223"))
        )

        return ProjectConfig("789", "1234", "3", "42", emptyList<Group>(), experiments, attributes,
                events, emptyList<Audience>(), true, emptyList<LiveVariable>())
    }

    private val VALID_PROJECT_CONFIG_V4 = generateValidProjectConfigV4()
    private fun generateValidProjectConfigV4(): ProjectConfig {
        return ValidProjectConfigV4.generateValidProjectConfigV4()
    }

    @Throws(IOException::class)
    fun validConfigJsonV2(): String {
        return Resources.toString(Resources.getResource("config/valid-project-config-v2.json"), Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun noAudienceProjectConfigJsonV2(): String {
        return Resources.toString(Resources.getResource("config/no-audience-project-config-v2.json"), Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun validConfigJsonV3(): String {
        return Resources.toString(Resources.getResource("config/valid-project-config-v3.json"), Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun noAudienceProjectConfigJsonV3(): String {
        return Resources.toString(Resources.getResource("config/no-audience-project-config-v3.json"), Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun validConfigJsonV4(): String {
        return Resources.toString(Resources.getResource("config/valid-project-config-v4.json"), Charsets.UTF_8)
    }

    /**
     * @return the expected [ProjectConfig] for the json produced by [.validConfigJsonV2] ()}
     */
    fun validProjectConfigV2(): ProjectConfig {
        return VALID_PROJECT_CONFIG_V2
    }

    /**
     * @return the expected [ProjectConfig] for the json produced by [.noAudienceProjectConfigJsonV2]
     */
    fun noAudienceProjectConfigV2(): ProjectConfig {
        return NO_AUDIENCE_PROJECT_CONFIG_V2
    }

    /**
     * @return the expected [ProjectConfig] for the json produced by [.validConfigJsonV3] ()}
     */
    fun validProjectConfigV3(): ProjectConfig {
        return VALID_PROJECT_CONFIG_V3
    }

    /**
     * @return the expected [ProjectConfig] for the json produced by [.noAudienceProjectConfigJsonV3]
     */
    fun noAudienceProjectConfigV3(): ProjectConfig {
        return NO_AUDIENCE_PROJECT_CONFIG_V3
    }

    fun validProjectConfigV4(): ProjectConfig {
        return VALID_PROJECT_CONFIG_V4
    }

    /**
     * Asserts that the provided project configs are equivalent.
     */
    fun verifyProjectConfig(actual: ProjectConfig?, expected: ProjectConfig) {
        assertNotNull(actual)

        // verify the project-level values
        assertThat(actual!!.accountId, `is`(expected.accountId))
        assertThat(actual.projectId, `is`(expected.projectId))
        assertThat(actual.version, `is`(expected.version))
        assertThat(actual.revision, `is`(expected.revision))

        verifyAttributes(actual.attributes, expected.attributes)
        verifyAudiences(actual.audiences, expected.audiences)
        verifyEvents(actual.eventTypes, expected.eventTypes)
        verifyExperiments(actual.experiments, expected.experiments)
        verifyFeatureFlags(actual.featureFlags, expected.featureFlags)
        verifyLiveVariables(actual.liveVariables, expected.liveVariables)
        verifyGroups(actual.groups, expected.groups)
        verifyRollouts(actual.rollouts, expected.rollouts)
    }

    /**
     * Asserts that the provided experiment configs are equivalent.
     */
    private fun verifyExperiments(actual: List<Experiment>, expected: List<Experiment>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualExperiment = actual[i]
            val expectedExperiment = expected[i]

            assertThat(actualExperiment.id, `is`(expectedExperiment.id))
            assertThat(actualExperiment.key, `is`(expectedExperiment.key))
            assertThat(actualExperiment.groupId, `is`(expectedExperiment.groupId))
            assertThat(actualExperiment.status, `is`(expectedExperiment.status))
            assertThat(actualExperiment.audienceIds, `is`(expectedExperiment.audienceIds))
            assertThat(actualExperiment.userIdToVariationKeyMap,
                    `is`(expectedExperiment.userIdToVariationKeyMap))

            verifyVariations(actualExperiment.variations, expectedExperiment.variations)
            verifyTrafficAllocations(actualExperiment.trafficAllocation,
                    expectedExperiment.trafficAllocation)
        }
    }

    private fun verifyFeatureFlags(actual: List<FeatureFlag>, expected: List<FeatureFlag>) {
        assertEquals(expected.size.toLong(), actual.size.toLong())
        for (i in actual.indices) {
            val actualFeatureFlag = actual[i]
            val expectedFeatureFlag = expected[i]

            assertEquals(expectedFeatureFlag, actualFeatureFlag)
        }
    }

    /**
     * Asserts that the provided variation configs are equivalent.
     */
    private fun verifyVariations(actual: List<Variation>, expected: List<Variation>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualVariation = actual[i]
            val expectedVariation = expected[i]

            assertThat(actualVariation.id, `is`(expectedVariation.id))
            assertThat(actualVariation.key, `is`(expectedVariation.key))
            verifyLiveVariableInstances(actualVariation.liveVariableUsageInstances,
                    expectedVariation.liveVariableUsageInstances)
        }
    }

    /**
     * Asserts that the provided traffic allocation configs are equivalent.
     */
    private fun verifyTrafficAllocations(actual: List<TrafficAllocation>,
                                         expected: List<TrafficAllocation>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualDistribution = actual[i]
            val expectedDistribution = expected[i]

            assertThat(actualDistribution.entityId, `is`(expectedDistribution.entityId))
            assertEquals("expectedDistribution: " + expectedDistribution.toString() +
                    "is not equal to the actualDistribution: " + actualDistribution.toString(),
                    expectedDistribution.endOfRange.toLong(), actualDistribution.endOfRange.toLong())
        }
    }

    /**
     * Asserts that the provided attributes configs are equivalent.
     */
    private fun verifyAttributes(actual: List<Attribute>, expected: List<Attribute>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualAttribute = actual[i]
            val expectedAttribute = expected[i]

            assertThat(actualAttribute.id, `is`(expectedAttribute.id))
            assertThat(actualAttribute.key, `is`(expectedAttribute.key))
            assertThat(actualAttribute.segmentId, `is`(expectedAttribute.segmentId))
        }
    }

    /**
     * Asserts that the provided event configs are equivalent.
     */
    private fun verifyEvents(actual: List<EventType>, expected: List<EventType>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualEvent = actual[i]
            val expectedEvent = expected[i]

            assertThat(actualEvent.experimentIds, `is`(expectedEvent.experimentIds))
            assertThat(actualEvent.id, `is`(expectedEvent.id))
            assertThat(actualEvent.key, `is`(expectedEvent.key))
        }
    }

    /**
     * Asserts that the provided audience configs are equivalent.
     */
    private fun verifyAudiences(actual: List<Audience>, expected: List<Audience>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualAudience = actual[i]
            val expectedAudience = expected[i]

            assertThat(actualAudience.id, `is`(expectedAudience.id))
            assertThat(actualAudience.key, `is`(expectedAudience.key))
            assertThat(actualAudience.conditions, `is`(expectedAudience.conditions))
            assertThat(actualAudience.conditions, `is`(expectedAudience.conditions))
        }
    }

    /**
     * Assert that the provided group configs are equivalent.
     */
    private fun verifyGroups(actual: List<Group>, expected: List<Group>) {
        assertThat(actual.size, `is`(expected.size))

        for (i in actual.indices) {
            val actualGroup = actual[i]
            val expectedGroup = expected[i]

            assertThat(actualGroup.id, `is`(expectedGroup.id))
            assertThat(actualGroup.policy, `is`(expectedGroup.policy))
            verifyTrafficAllocations(actualGroup.trafficAllocation, expectedGroup.trafficAllocation)
            verifyExperiments(actualGroup.experiments, expectedGroup.experiments)
        }
    }

    /**
     * Verify that the provided live variable definitions are equivalent.
     */
    private fun verifyLiveVariables(actual: List<LiveVariable>?, expected: List<LiveVariable>?) {
        // if using V2, live variables will be null
        if (expected == null) {
            assertNull(actual)
        } else {
            assertThat(actual!!.size, `is`(expected.size))

            for (i in actual.indices) {
                val actualLiveVariable = actual[i]
                val expectedLiveVariable = expected[i]

                assertThat(actualLiveVariable.id, `is`(expectedLiveVariable.id))
                assertThat(actualLiveVariable.key, `is`(expectedLiveVariable.key))
                assertThat(actualLiveVariable.defaultValue, `is`(expectedLiveVariable.defaultValue))
                assertThat<LiveVariable.VariableType>(actualLiveVariable.type, `is`<LiveVariable.VariableType>(expectedLiveVariable.type))
                assertThat<LiveVariable.VariableStatus>(actualLiveVariable.status, `is`<LiveVariable.VariableStatus>(expectedLiveVariable.status))
            }
        }
    }

    private fun verifyRollouts(actual: List<Rollout>, expected: List<Rollout>?) {
        if (expected == null) {
            assertNull(actual)
        } else {
            assertEquals(expected.size.toLong(), actual.size.toLong())

            for (i in actual.indices) {
                val actualRollout = actual[i]
                val expectedRollout = expected[i]

                assertEquals(expectedRollout.id, actualRollout.id)
                verifyExperiments(actualRollout.experiments, expectedRollout.experiments)
            }
        }
    }

    /**
     * Verify that the provided variation-level live variable usage instances are equivalent.
     */
    private fun verifyLiveVariableInstances(actual: List<LiveVariableUsageInstance>?,
                                            expected: List<LiveVariableUsageInstance>?) {
        // if using V2, live variable instances will be null
        if (expected == null) {
            assertNull(actual)
        } else {
            assertThat(actual!!.size, `is`(expected.size))

            for (i in actual.indices) {
                val actualLiveVariableUsageInstance = actual[i]
                val expectedLiveVariableUsageInstance = expected[i]

                assertThat(actualLiveVariableUsageInstance.id, `is`(expectedLiveVariableUsageInstance.id))
                assertThat(actualLiveVariableUsageInstance.value, `is`(expectedLiveVariableUsageInstance.value))
            }
        }
    }

    fun <T> createListOfObjects(vararg elements: T): List<T> {
        val list = ArrayList<T>(elements.size)
        for (element in elements) {
            list.add(element)
        }
        return list
    }

    fun <K, V> createMapOfObjects(keys: List<K>, values: List<V>): Map<K, V> {
        val map = HashMap<K, V>(keys.size)
        if (keys.size == values.size) {
            val keysIterator = keys.iterator()
            val valuesIterator = values.iterator()
            while (keysIterator.hasNext() && valuesIterator.hasNext()) {
                val key = keysIterator.next()
                val value = valuesIterator.next()
                map.put(key, value)
            }
        }
        return map
    }
}
