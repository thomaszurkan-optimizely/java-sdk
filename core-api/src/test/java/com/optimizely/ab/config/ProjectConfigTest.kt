/**
 *
 * Copyright 2016-2018, Optimizely and contributors
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

import ch.qos.logback.classic.Level
import com.optimizely.ab.config.audience.AndCondition
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.config.audience.NotCondition
import com.optimizely.ab.config.audience.OrCondition
import com.optimizely.ab.config.audience.UserAttribute

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

import java.util.Arrays.asList
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals


import com.optimizely.ab.internal.LogbackVerifier
import com.optimizely.ab.internal.ControlAttribute
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * Tests for [ProjectConfig].
 */
class ProjectConfigTest {

    private var projectConfig: ProjectConfig? = null

    @Rule
    var logbackVerifier = LogbackVerifier()

    @Before
    fun initialize() {
        projectConfig = ProjectConfigTestUtils.validProjectConfigV3()
    }

    /**
     * Verify that [ProjectConfig.toString] doesn't throw an exception.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @Throws(Exception::class)
    fun toStringDoesNotFail() {
        projectConfig!!.toString()
    }

    /**
     * Asserts that [ProjectConfig.getExperimentsForEventKey]
     * returns the respective experiment ids for experiments using an event,
     * provided that the event parameter is valid.
     */
    @Test
    @Throws(Exception::class)
    fun verifyGetExperimentsForValidEvent() {
        val experiment223 = projectConfig!!.experimentIdMapping["223"]
        val experiment118 = projectConfig!!.experimentIdMapping["118"]
        val expectedSingleExperiment = asList<Experiment>(experiment223)
        val actualSingleExperiment = projectConfig!!.getExperimentsForEventKey("clicked_cart")
        assertThat(actualSingleExperiment, `is`(expectedSingleExperiment))

        val expectedMultipleExperiments = asList<Experiment>(experiment118, experiment223)
        val actualMultipleExperiments = projectConfig!!.getExperimentsForEventKey("clicked_purchase")
        assertThat(actualMultipleExperiments, `is`(expectedMultipleExperiments))
    }

    /**
     * Asserts that [ProjectConfig.getExperimentsForEventKey] returns an empty List
     * when given an invalid event key.
     */
    @Test
    @Throws(Exception::class)
    fun verifyGetExperimentsForInvalidEvent() {
        val expectedExperiments = emptyList<Experiment>()
        val actualExperiments = projectConfig!!.getExperimentsForEventKey("a_fake_event")
        assertThat(actualExperiments, `is`<List<Experiment>>(expectedExperiments))
    }

    /**
     * Asserts that getAudienceConditionsFromId returns the respective conditions for an audience, provided the
     * audience ID parameter is valid.
     */
    @Test
    @Throws(Exception::class)
    fun verifyGetAudienceConditionsFromValidId() {
        val userAttributes = ArrayList<Condition>()
        userAttributes.add(UserAttribute("browser_type", "custom_dimension", "firefox"))

        val orInner = OrCondition(userAttributes)

        val notCondition = NotCondition(orInner)
        val outerOrList = ArrayList<Condition>()
        outerOrList.add(notCondition)

        val orOuter = OrCondition(outerOrList)
        val andList = ArrayList<Condition>()
        andList.add(orOuter)

        val expectedConditions = AndCondition(andList)
        val actualConditions = projectConfig!!.getAudienceConditionsFromId("100")
        assertThat(actualConditions, `is`<Condition>(expectedConditions))
    }

    /**
     * Asserts that getAudienceConditionsFromId returns null given an invalid audience ID parameter.
     */
    @Test
    @Throws(Exception::class)
    fun verifyGetAudienceConditionsFromInvalidId() {
        assertNull(projectConfig!!.getAudienceConditionsFromId("invalid_id"))
    }

    /**
     * Asserts that getLiveVariableIdToExperimentsMapping returns a correct mapping between live variable IDs and
     * corresponding experiments using these live variables.
     */
    @Test
    @Throws(Exception::class)
    fun verifyGetLiveVariableIdToExperimentsMapping() {
        val ungroupedExpWithVariables = projectConfig!!.experiments[0]
        val groupedExpWithVariables = projectConfig!!.groups[0].experiments[1]

        val expectedLiveVariableIdToExperimentsMapping = HashMap<String, List<Experiment>>()
        expectedLiveVariableIdToExperimentsMapping.put("6", listOf<Experiment>(ungroupedExpWithVariables))
        expectedLiveVariableIdToExperimentsMapping.put("2", listOf<Experiment>(ungroupedExpWithVariables))
        expectedLiveVariableIdToExperimentsMapping.put("3", listOf<Experiment>(ungroupedExpWithVariables))
        expectedLiveVariableIdToExperimentsMapping.put("4", listOf<Experiment>(ungroupedExpWithVariables))

        expectedLiveVariableIdToExperimentsMapping.put("7", listOf<Experiment>(groupedExpWithVariables))

        assertThat(projectConfig!!.liveVariableIdToExperimentsMapping,
                `is`<Map<String, List<Experiment>>>(expectedLiveVariableIdToExperimentsMapping))
    }

    /**
     * Asserts that getVariationToLiveVariableUsageInstanceMapping returns a correct mapping between variation IDs and
     * the values of the live variables for the variation.
     */
    @Test
    @Throws(Exception::class)
    fun verifyGetVariationToLiveVariableUsageInstanceMapping() {
        val expectedVariationToLiveVariableUsageInstanceMapping = HashMap<String, Map<String, LiveVariableUsageInstance>>()

        val ungroupedVariation276VariableValues = HashMap<String, LiveVariableUsageInstance>()
        ungroupedVariation276VariableValues.put("6", LiveVariableUsageInstance("6", "True"))
        ungroupedVariation276VariableValues.put("2", LiveVariableUsageInstance("2", "10"))
        ungroupedVariation276VariableValues.put("3", LiveVariableUsageInstance("3", "string_var_vtag1"))
        ungroupedVariation276VariableValues.put("4", LiveVariableUsageInstance("4", "5.3"))


        val ungroupedVariation277VariableValues = HashMap<String, LiveVariableUsageInstance>()
        ungroupedVariation277VariableValues.put("6", LiveVariableUsageInstance("6", "False"))
        ungroupedVariation277VariableValues.put("2", LiveVariableUsageInstance("2", "20"))
        ungroupedVariation277VariableValues.put("3", LiveVariableUsageInstance("3", "string_var_vtag2"))
        ungroupedVariation277VariableValues.put("4", LiveVariableUsageInstance("4", "6.3"))

        expectedVariationToLiveVariableUsageInstanceMapping.put("276", ungroupedVariation276VariableValues)
        expectedVariationToLiveVariableUsageInstanceMapping.put("277", ungroupedVariation277VariableValues)

        val groupedVariation280VariableValues = HashMap<String, LiveVariableUsageInstance>()
        groupedVariation280VariableValues.put("7", LiveVariableUsageInstance("7", "True"))

        val groupedVariation281VariableValues = HashMap<String, LiveVariableUsageInstance>()
        groupedVariation281VariableValues.put("7", LiveVariableUsageInstance("7", "False"))

        expectedVariationToLiveVariableUsageInstanceMapping.put("280", groupedVariation280VariableValues)
        expectedVariationToLiveVariableUsageInstanceMapping.put("281", groupedVariation281VariableValues)

        assertThat(projectConfig!!.variationToLiveVariableUsageInstanceMapping,
                `is`<Map<String, Map<String, LiveVariableUsageInstance>>>(expectedVariationToLiveVariableUsageInstanceMapping))
    }

    /**
     * Asserts that anonymizeIP is set to false if not explicitly passed into the constructor (in the case of V2
     * projects).
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun verifyAnonymizeIPIsFalseByDefault() {
        val v2ProjectConfig = ProjectConfigTestUtils.validProjectConfigV2()
        assertFalse(v2ProjectConfig.anonymizeIP)
    }

    /**
     * Invalid User IDs
     *
     * User ID is null
     * User ID is an empty string
     * Invalid Experiment IDs
     *
     * Experiment key does not exist in the datafile
     * Experiment key is null
     * Experiment key is an empty string
     * Invalid Variation IDs [set only]
     *
     * Variation key does not exist in the datafile
     * Variation key is null
     * Variation key is an empty string
     * Multiple set calls [set only]
     *
     * Call set variation with different variations on one user/experiment to confirm that each set is expected.
     * Set variation on multiple variations for one user.
     * Set variations for multiple users.
     */
    /* UserID test */
    @Test
    @SuppressFBWarnings("NP")
    fun setForcedVariationNullUserId() {
        val b = projectConfig!!.setForcedVariation("etag1", null!!, "vtag1")
        assertFalse(b)
    }

    @Test
    @SuppressFBWarnings("NP")
    fun getForcedVariationNullUserId() {
        assertNull(projectConfig!!.getForcedVariation("etag1", null!!))
    }

    @Test
    fun setForcedVariationEmptyUserId() {
        assertFalse(projectConfig!!.setForcedVariation("etag1", "", "vtag1"))
    }

    @Test
    fun getForcedVariationEmptyUserId() {
        assertNull(projectConfig!!.getForcedVariation("etag1", ""))
    }

    /* Invalid Experiement */
    @Test
    @SuppressFBWarnings("NP")
    fun setForcedVariationNullExperimentKey() {
        assertFalse(projectConfig!!.setForcedVariation(null!!, "testUser1", "vtag1"))
    }

    @Test
    @SuppressFBWarnings("NP")
    fun getForcedVariationNullExperimentKey() {
        assertNull(projectConfig!!.getForcedVariation(null!!, "testUser1"))
    }

    @Test
    fun setForcedVariationWrongExperimentKey() {
        assertFalse(projectConfig!!.setForcedVariation("wrongKey", "testUser1", "vtag1"))

    }

    @Test
    fun getForcedVariationWrongExperimentKey() {
        assertNull(projectConfig!!.getForcedVariation("wrongKey", "testUser1"))
    }

    @Test
    fun setForcedVariationEmptyExperimentKey() {
        assertFalse(projectConfig!!.setForcedVariation("", "testUser1", "vtag1"))

    }

    @Test
    fun getForcedVariationEmptyExperimentKey() {
        assertNull(projectConfig!!.getForcedVariation("", "testUser1"))
    }

    /* Invalid Variation Id (set only */
    @Test
    fun setForcedVariationWrongVariationKey() {
        assertFalse(projectConfig!!.setForcedVariation("etag1", "testUser1", "vtag3"))
    }

    @Test
    fun setForcedVariationNullVariationKey() {
        assertFalse(projectConfig!!.setForcedVariation("etag1", "testUser1", null))
        assertNull(projectConfig!!.getForcedVariation("etag1", "testUser1"))
    }

    @Test
    fun setForcedVariationEmptyVariationKey() {
        assertFalse(projectConfig!!.setForcedVariation("etag1", "testUser1", ""))
    }

    /* Multiple set calls (set only */
    @Test
    fun setForcedVariationDifferentVariations() {
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", "vtag1"))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", "vtag2"))
        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser1")!!.key, "vtag2")
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", null))
    }

    @Test
    fun setForcedVariationMultipleVariationsExperiments() {
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", "vtag1"))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser2", "vtag2"))
        assertTrue(projectConfig!!.setForcedVariation("etag2", "testUser1", "vtag3"))
        assertTrue(projectConfig!!.setForcedVariation("etag2", "testUser2", "vtag4"))
        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser1")!!.key, "vtag1")
        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser2")!!.key, "vtag2")
        assertEquals(projectConfig!!.getForcedVariation("etag2", "testUser1")!!.key, "vtag3")
        assertEquals(projectConfig!!.getForcedVariation("etag2", "testUser2")!!.key, "vtag4")
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", null))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser2", null))
        assertTrue(projectConfig!!.setForcedVariation("etag2", "testUser1", null))
        assertTrue(projectConfig!!.setForcedVariation("etag2", "testUser2", null))
        assertNull(projectConfig!!.getForcedVariation("etag1", "testUser1"))
        assertNull(projectConfig!!.getForcedVariation("etag1", "testUser2"))
        assertNull(projectConfig!!.getForcedVariation("etag2", "testUser1"))
        assertNull(projectConfig!!.getForcedVariation("etag2", "testUser2"))


    }

    @Test
    fun setForcedVariationMultipleUsers() {
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", "vtag1"))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser2", "vtag1"))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser3", "vtag1"))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser4", "vtag1"))

        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser1")!!.key, "vtag1")
        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser2")!!.key, "vtag1")
        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser3")!!.key, "vtag1")
        assertEquals(projectConfig!!.getForcedVariation("etag1", "testUser4")!!.key, "vtag1")

        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser1", null))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser2", null))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser3", null))
        assertTrue(projectConfig!!.setForcedVariation("etag1", "testUser4", null))

        assertNull(projectConfig!!.getForcedVariation("etag1", "testUser1"))
        assertNull(projectConfig!!.getForcedVariation("etag1", "testUser2"))
        assertNull(projectConfig!!.getForcedVariation("etag2", "testUser1"))
        assertNull(projectConfig!!.getForcedVariation("etag2", "testUser2"))

    }

    @Test
    fun getAttributeIDWhenAttributeKeyIsFromAttributeKeyMapping() {
        val projectConfig = ProjectConfigTestUtils.validProjectConfigV4()
        val attributeID = projectConfig.getAttributeId(projectConfig, "house")
        assertEquals(attributeID, "553339214")
    }

    @Test
    fun getAttributeIDWhenAttributeKeyIsUsingReservedKey() {
        val projectConfig = ProjectConfigTestUtils.validProjectConfigV4()
        val attributeID = projectConfig.getAttributeId(projectConfig, "\$opt_user_agent")
        assertEquals(attributeID, ControlAttribute.USER_AGENT_ATTRIBUTE.toString())
    }

    @Test
    fun getAttributeIDWhenAttributeKeyUnrecognizedAttribute() {
        val projectConfig = ProjectConfigTestUtils.validProjectConfigV4()
        val invalidAttribute = "empty"
        val attributeID = projectConfig.getAttributeId(projectConfig, invalidAttribute)
        assertNull(attributeID)
        logbackVerifier.expectMessage(Level.DEBUG, "Unrecognized Attribute \"" + invalidAttribute + "\"")
    }

    @Test
    fun getAttributeIDWhenAttributeKeyPrefixIsMatched() {
        val projectConfig = ProjectConfigTestUtils.validProjectConfigV4()
        val attributeWithReservedPrefix = "\$opt_test"
        val attributeID = projectConfig.getAttributeId(projectConfig, attributeWithReservedPrefix)
        assertEquals(attributeID, "583394100")
        logbackVerifier.expectMessage(Level.WARN, "Attribute " + attributeWithReservedPrefix + " unexpectedly" +
                " has reserved prefix \$opt_; using attribute ID instead of reserved attribute name.")
    }

}