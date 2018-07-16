/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.bucketing

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.ProjectConfigTestUtils
import com.optimizely.ab.config.Rollout
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.ValidProjectConfigV4
import com.optimizely.ab.config.Variation
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.internal.LogbackVerifier

import com.optimizely.ab.internal.ControlAttribute
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

import java.util.Collections
import java.util.HashMap

import ch.qos.logback.classic.Level
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4
import com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_NATIONALITY_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_ENGLISH_CITIZENS_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_MULTI_VARIATE_FEATURE
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_SINGLE_VARIABLE_INTEGER
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_MULTI_VARIATE_FEATURE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.ROLLOUT_2
import com.optimizely.ab.config.ValidProjectConfigV4.ROLLOUT_3_EVERYONE_ELSE_RULE
import com.optimizely.ab.config.ValidProjectConfigV4.ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.mockito.Matchers.any
import org.mockito.Matchers.anyMapOf
import org.mockito.Matchers.anyString
import org.mockito.Matchers.eq
import org.mockito.Mockito.atMost
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class DecisionServiceTest {

    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    var rule = MockitoJUnit.rule()

    @Mock
    private val mockErrorHandler: ErrorHandler? = null

    @Rule
    var logbackVerifier = LogbackVerifier()

    //========= getVariation tests =========/

    /**
     * Verify that [DecisionService.getVariation]
     * gives precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    @Throws(Exception::class)
    fun getVariationWhitelistedPrecedesAudienceEval() {
        val bucketer = spy(Bucketer(validProjectConfig))
        val decisionService = spy(DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null))
        val experiment = validProjectConfig!!.experiments[0]
        val expectedVariation = experiment.variations[0]

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, emptyMap<String, String>()))

        logbackVerifier.expectMessage(Level.INFO, "User \"$whitelistedUserId\" is forced in variation \"vtag1\".")

        // no attributes provided for a experiment that has an audience
        assertThat<Variation>(decisionService.getVariation(experiment, whitelistedUserId, emptyMap<String, String>()), `is`(expectedVariation))

        verify(decisionService).getWhitelistedVariation(experiment, whitelistedUserId)
        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile::class.java))
    }

    /**
     * Verify that [DecisionService.getVariation]
     * gives precedence to forced variation bucketing over whitelisting.
     */
    @Test
    @Throws(Exception::class)
    fun getForcedVariationBeforeWhitelisting() {
        val bucketer = Bucketer(validProjectConfig)
        val decisionService = spy(DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null))
        val experiment = validProjectConfig!!.experiments[0]
        val whitelistVariation = experiment.variations[0]
        val expectedVariation = experiment.variations[1]

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, emptyMap<String, String>()))

        logbackVerifier.expectMessage(Level.INFO, "User \"$genericUserId\" does not meet conditions to be in experiment \"etag1\".")

        // set the runtimeForcedVariation
        validProjectConfig!!.setForcedVariation(experiment.key, whitelistedUserId, expectedVariation.key)
        // no attributes provided for a experiment that has an audience
        assertThat<Variation>(decisionService.getVariation(experiment, whitelistedUserId, emptyMap<String, String>()), `is`(expectedVariation))

        //verify(decisionService).getForcedVariation(experiment.getKey(), whitelistedUserId);
        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile::class.java))
        assertEquals(decisionService.getWhitelistedVariation(experiment, whitelistedUserId), whitelistVariation)
        assertTrue(validProjectConfig!!.setForcedVariation(experiment.key, whitelistedUserId, null))
        assertNull(validProjectConfig!!.getForcedVariation(experiment.key, whitelistedUserId))
        assertThat<Variation>(decisionService.getVariation(experiment, whitelistedUserId, emptyMap<String, String>()), `is`(whitelistVariation))
    }

    /**
     * Verify that [DecisionService.getVariation]
     * gives precedence to forced variation bucketing over audience evaluation.
     */
    @Test
    @Throws(Exception::class)
    fun getVariationForcedPrecedesAudienceEval() {
        val bucketer = spy(Bucketer(validProjectConfig))
        val decisionService = spy(DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null))
        val experiment = validProjectConfig!!.experiments[0]
        val expectedVariation = experiment.variations[1]

        // user excluded without audiences and whitelisting
        assertNull(decisionService.getVariation(experiment, genericUserId, emptyMap<String, String>()))

        logbackVerifier.expectMessage(Level.INFO, "User \"$genericUserId\" does not meet conditions to be in experiment \"etag1\".")

        // set the runtimeForcedVariation
        validProjectConfig!!.setForcedVariation(experiment.key, genericUserId, expectedVariation.key)
        // no attributes provided for a experiment that has an audience
        assertThat<Variation>(decisionService.getVariation(experiment, genericUserId, emptyMap<String, String>()), `is`(expectedVariation))

        verify(decisionService, never()).getStoredVariation(eq(experiment), any(UserProfile::class.java))
        assertEquals(validProjectConfig!!.setForcedVariation(experiment.key, genericUserId, null), true)
        assertNull(validProjectConfig!!.getForcedVariation(experiment.key, genericUserId))
    }

    /**
     * Verify that [DecisionService.getVariation]
     * gives precedence to forced variation bucketing over user profile.
     */
    @Test
    @Throws(Exception::class)
    fun getVariationForcedBeforeUserProfile() {
        val experiment = validProjectConfig!!.experiments[0]
        val variation = experiment.variations[0]
        val bucketer = spy(Bucketer(validProjectConfig))
        val decision = Decision(variation.id)
        val userProfile = UserProfile(userProfileId,
                Collections.singletonMap(experiment.id, decision))
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        `when`(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap())

        val decisionService = spy(DecisionService(bucketer,
                mockErrorHandler!!, validProjectConfig!!, userProfileService))

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertNull(decisionService.getVariation(experiment, genericUserId, emptyMap<String, String>()))

        logbackVerifier.expectMessage(Level.INFO,
                "User \"" + genericUserId + "\" does not meet conditions to be in experiment \""
                        + experiment.key + "\".")

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertEquals(variation,
                decisionService.getVariation(experiment, userProfileId, emptyMap<String, String>()))

        val forcedVariation = experiment.variations[1]
        validProjectConfig!!.setForcedVariation(experiment.key, userProfileId, forcedVariation.key)
        assertEquals(forcedVariation,
                decisionService.getVariation(experiment, userProfileId, emptyMap<String, String>()))
        assertTrue(validProjectConfig!!.setForcedVariation(experiment.key, userProfileId, null))
        assertNull(validProjectConfig!!.getForcedVariation(experiment.key, userProfileId))


    }

    /**
     * Verify that [DecisionService.getVariation]
     * gives precedence to user profile over audience evaluation.
     */
    @Test
    @Throws(Exception::class)
    fun getVariationEvaluatesUserProfileBeforeAudienceTargeting() {
        val experiment = validProjectConfig!!.experiments[0]
        val variation = experiment.variations[0]
        val bucketer = spy(Bucketer(validProjectConfig))
        val decision = Decision(variation.id)
        val userProfile = UserProfile(userProfileId,
                Collections.singletonMap(experiment.id, decision))
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        `when`(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap())

        val decisionService = spy(DecisionService(bucketer,
                mockErrorHandler!!, validProjectConfig!!, userProfileService))

        // ensure that normal users still get excluded from the experiment when they fail audience evaluation
        assertNull(decisionService.getVariation(experiment, genericUserId, emptyMap<String, String>()))

        logbackVerifier.expectMessage(Level.INFO,
                "User \"" + genericUserId + "\" does not meet conditions to be in experiment \""
                        + experiment.key + "\".")

        // ensure that a user with a saved user profile, sees the same variation regardless of audience evaluation
        assertEquals(variation,
                decisionService.getVariation(experiment, userProfileId, emptyMap<String, String>()))

    }

    /**
     * Verify that [DecisionService.getVariation]
     * gives a null variation on a Experiment that is not running. Set the forced variation.
     * And, test to make sure that after setting forced variation, the getVariation still returns
     * null.
     */
    @Test
    fun getVariationOnNonRunningExperimentWithForcedVariation() {
        val experiment = validProjectConfig!!.experiments[1]
        assertFalse(experiment.isRunning)
        val variation = experiment.variations[0]
        val bucketer = Bucketer(validProjectConfig)

        val decisionService = spy(DecisionService(bucketer,
                mockErrorHandler!!, validProjectConfig!!, null))

        // ensure that the not running variation returns null with no forced variation set.
        assertNull(decisionService.getVariation(experiment, "userId", emptyMap<String, String>()))

        // we call getVariation 3 times on an experiment that is not running.
        logbackVerifier.expectMessage(Level.INFO,
                "Experiment \"etag2\" is not running.", times(3))

        // set a forced variation on the user that got back null
        assertTrue(validProjectConfig!!.setForcedVariation(experiment.key, "userId", variation.key))

        // ensure that a user with a forced variation set
        // still gets back a null variation if the variation is not running.
        assertNull(decisionService.getVariation(experiment, "userId", emptyMap<String, String>()))

        // set the forced variation back to null
        assertTrue(validProjectConfig!!.setForcedVariation(experiment.key, "userId", null))
        // test one more time that the getVariation returns null for the experiment that is not running.
        assertNull(decisionService.getVariation(experiment, "userId", emptyMap<String, String>()))


    }

    //========== get Variation for Feature tests ==========//

    /**
     * Verify that [DecisionService.getVariationForFeature]
     * returns null when the [FeatureFlag] is not used in any experiments or rollouts.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    fun getVariationForFeatureReturnsNullWhenFeatureFlagExperimentIdsIsEmpty() {
        val emptyFeatureFlag = mock<FeatureFlag>(FeatureFlag::class.java)
        `when`(emptyFeatureFlag.experimentIds).thenReturn(emptyList<String>())
        val featureKey = "testFeatureFlagKey"
        `when`(emptyFeatureFlag.key).thenReturn(featureKey)
        `when`(emptyFeatureFlag.rolloutId).thenReturn("")

        val decisionService = DecisionService(
                mock(Bucketer::class.java),
                mockErrorHandler!!,
                validProjectConfig!!, null)

        logbackVerifier.expectMessage(Level.INFO,
                "The feature flag \"$featureKey\" is not used in any experiments.")
        logbackVerifier.expectMessage(Level.INFO,
                "The feature flag \"$featureKey\" is not used in a rollout.")
        logbackVerifier.expectMessage(Level.INFO,
                "The user \"" + genericUserId + "\" was not bucketed into a rollout for feature flag \"" +
                        featureKey + "\".")

        val featureDecision = decisionService.getVariationForFeature(
                emptyFeatureFlag,
                genericUserId,
                emptyMap<String, String>())
        assertNull(featureDecision.variation)
        assertNull(featureDecision.decisionSource)

        verify(emptyFeatureFlag, times(1)).experimentIds
        verify(emptyFeatureFlag, times(1)).rolloutId
        verify(emptyFeatureFlag, times(3)).key
    }

    /**
     * Verify that [DecisionService.getVariationForFeature]
     * returns null when the user is not bucketed into any experiments or rollouts for the [FeatureFlag].
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    fun getVariationForFeatureReturnsNullWhenItGetsNoVariationsForExperimentsAndRollouts() {
        val spyFeatureFlag = spy(FEATURE_FLAG_MULTI_VARIATE_FEATURE)

        val spyDecisionService = spy(DecisionService(
                mock(Bucketer::class.java),
                mockErrorHandler!!,
                validProjectConfig!!, null)
        )

        // do not bucket to any experiments
        doReturn(null).`when`(spyDecisionService).getVariation(
                any(Experiment::class.java),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )
        // do not bucket to any rollouts
        doReturn(FeatureDecision(null, null, null)).`when`(spyDecisionService).getVariationForFeatureInRollout(
                any(FeatureFlag::class.java),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // try to get a variation back from the decision service for the feature flag
        val featureDecision = spyDecisionService.getVariationForFeature(
                spyFeatureFlag,
                genericUserId,
                emptyMap<String, String>()
        )
        assertNull(featureDecision.variation)
        assertNull(featureDecision.decisionSource)

        logbackVerifier.expectMessage(Level.INFO,
                "The user \"" + genericUserId + "\" was not bucketed into a rollout for feature flag \"" +
                        FEATURE_MULTI_VARIATE_FEATURE_KEY + "\".")

        verify(spyFeatureFlag, times(2)).experimentIds
        verify(spyFeatureFlag, times(1)).key
    }

    /**
     * Verify that [DecisionService.getVariationForFeature]
     * returns the variation of the experiment a user gets bucketed into for an experiment.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    fun getVariationForFeatureReturnsVariationReturnedFromGetVariation() {
        val spyFeatureFlag = spy(ValidProjectConfigV4.FEATURE_FLAG_MUTEX_GROUP_FEATURE)

        val spyDecisionService = spy(DecisionService(
                mock(Bucketer::class.java),
                mockErrorHandler!!,
                validProjectConfigV4(), null)
        )

        doReturn(null).`when`(spyDecisionService).getVariation(
                eq(ValidProjectConfigV4.EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        doReturn(ValidProjectConfigV4.VARIATION_MUTEX_GROUP_EXP_2_VAR_1).`when`(spyDecisionService).getVariation(
                eq(ValidProjectConfigV4.EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        val featureDecision = spyDecisionService.getVariationForFeature(
                spyFeatureFlag,
                genericUserId,
                emptyMap<String, String>()
        )
        assertEquals(ValidProjectConfigV4.VARIATION_MUTEX_GROUP_EXP_2_VAR_1, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.EXPERIMENT, featureDecision.decisionSource)

        verify(spyFeatureFlag, times(2)).experimentIds
        verify(spyFeatureFlag, never()).key
    }

    /**
     * Verify that when getting a [Variation] for a [FeatureFlag] in
     * [DecisionService.getVariationForFeature],
     * check first if the user is bucketed to an [Experiment]
     * then check if the user is not bucketed to an experiment,
     * check for a [Rollout].
     */
    @Test
    fun getVariationForFeatureReturnsVariationFromExperimentBeforeRollout() {
        val featureFlag = FEATURE_FLAG_MULTI_VARIATE_FEATURE
        val featureExperiment = v4ProjectConfig!!.experimentIdMapping[featureFlag.experimentIds[0]]
        assertNotNull(featureExperiment)
        val featureRollout = v4ProjectConfig!!.rolloutIdMapping[featureFlag.rolloutId]
        val experimentVariation = featureExperiment.variations[0]
        val rolloutExperiment = featureRollout.experiments[0]
        val rolloutVariation = rolloutExperiment.variations[0]

        val decisionService = spy(DecisionService(
                mock(Bucketer::class.java),
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )
        )

        // return variation for experiment
        doReturn(experimentVariation)
                .`when`(decisionService).getVariation(
                eq<Experiment>(featureExperiment),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // return variation for rollout
        doReturn(FeatureDecision(rolloutExperiment, rolloutVariation, FeatureDecision.DecisionSource.ROLLOUT))
                .`when`(decisionService).getVariationForFeatureInRollout(
                eq(featureFlag),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // make sure we get the right variation back
        val featureDecision = decisionService.getVariationForFeature(
                featureFlag,
                genericUserId,
                emptyMap<String, String>()
        )
        assertEquals(experimentVariation, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.EXPERIMENT, featureDecision.decisionSource)

        // make sure we do not even check for rollout bucketing
        verify(decisionService, never()).getVariationForFeatureInRollout(
                any(FeatureFlag::class.java),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // make sure we ask for experiment bucketing once
        verify(decisionService, times(1)).getVariation(
                any(Experiment::class.java),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )
    }

    /**
     * Verify that when getting a [Variation] for a [FeatureFlag] in
     * [DecisionService.getVariationForFeature],
     * check first if the user is bucketed to an [Rollout]
     * if the user is not bucketed to an experiment.
     */
    @Test
    fun getVariationForFeatureReturnsVariationFromRolloutWhenExperimentFails() {
        val featureFlag = FEATURE_FLAG_MULTI_VARIATE_FEATURE
        val featureExperiment = v4ProjectConfig!!.experimentIdMapping[featureFlag.experimentIds[0]]
        assertNotNull(featureExperiment)
        val featureRollout = v4ProjectConfig!!.rolloutIdMapping[featureFlag.rolloutId]
        val rolloutExperiment = featureRollout.experiments[0]
        val rolloutVariation = rolloutExperiment.variations[0]

        val decisionService = spy(DecisionService(
                mock(Bucketer::class.java),
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )
        )

        // return variation for experiment
        doReturn(null)
                .`when`(decisionService).getVariation(
                eq<Experiment>(featureExperiment),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // return variation for rollout
        doReturn(FeatureDecision(rolloutExperiment, rolloutVariation, FeatureDecision.DecisionSource.ROLLOUT))
                .`when`(decisionService).getVariationForFeatureInRollout(
                eq(featureFlag),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // make sure we get the right variation back
        val featureDecision = decisionService.getVariationForFeature(
                featureFlag,
                genericUserId,
                emptyMap<String, String>()
        )
        assertEquals(rolloutVariation, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource)

        // make sure we do not even check for rollout bucketing
        verify(decisionService, times(1)).getVariationForFeatureInRollout(
                any(FeatureFlag::class.java),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        // make sure we ask for experiment bucketing once
        verify(decisionService, times(1)).getVariation(
                any(Experiment::class.java),
                anyString(),
                anyMapOf(String::class.java, String::class.java)
        )

        logbackVerifier.expectMessage(
                Level.INFO,
                "The user \"" + genericUserId + "\" was bucketed into a rollout for feature flag \"" +
                        featureFlag.key + "\"."
        )
    }

    //========== getVariationForFeatureInRollout tests ==========//

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * returns null when trying to bucket a user into a [FeatureFlag]
     * that does not have a [Rollout] attached.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsNullWhenFeatureIsNotAttachedToRollout() {
        val mockFeatureFlag = mock<FeatureFlag>(FeatureFlag::class.java)
        `when`(mockFeatureFlag.rolloutId).thenReturn("")
        val featureKey = "featureKey"
        `when`(mockFeatureFlag.key).thenReturn(featureKey)

        val decisionService = DecisionService(
                mock(Bucketer::class.java),
                mockErrorHandler!!,
                validProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                mockFeatureFlag,
                genericUserId,
                emptyMap<String, String>()
        )
        assertNull(featureDecision.variation)
        assertNull(featureDecision.decisionSource)

        logbackVerifier.expectMessage(
                Level.INFO,
                "The feature flag \"$featureKey\" is not used in a rollout."
        )
    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * return null when a user is excluded from every rule of a rollout due to traffic allocation.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsNullWhenUserIsExcludedFromAllTraffic() {
        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        `when`<Variation>(mockBucketer.bucket(any(Experiment::class.java), anyString())).thenReturn(null)

        val decisionService = DecisionService(
                mockBucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                Collections.singletonMap(
                        ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE
                )
        )
        assertNull(featureDecision.variation)
        assertNull(featureDecision.decisionSource)

        // with fall back bucketing, the user has at most 2 chances to get bucketed with traffic allocation
        // one chance with the audience rollout rule
        // one chance with the everyone else rule
        verify(mockBucketer, atMost(2)).bucket(any(Experiment::class.java), anyString())
    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * returns null when a user is excluded from every rule of a rollout due to targeting
     * and also fails traffic allocation in the everyone else rollout.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsNullWhenUserFailsAllAudiencesAndTraffic() {
        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        `when`<Variation>(mockBucketer.bucket(any(Experiment::class.java), anyString())).thenReturn(null)

        val decisionService = DecisionService(
                mockBucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                emptyMap<String, String>()
        )
        assertNull(featureDecision.variation)
        assertNull(featureDecision.decisionSource)

        // user is only bucketed once for the everyone else rule
        verify(mockBucketer, times(1)).bucket(any(Experiment::class.java), anyString())
    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * returns the variation of "Everyone Else" rule
     * when the user fails targeting for all rules, but is bucketed into the "Everyone Else" rule.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsVariationWhenUserFailsAllAudienceButSatisfiesTraffic() {
        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        val rollout = ROLLOUT_2
        val everyoneElseRule = rollout.experiments[rollout.experiments.size - 1]
        val expectedVariation = everyoneElseRule.variations[0]
        `when`<Variation>(mockBucketer.bucket(eq(everyoneElseRule), anyString())).thenReturn(expectedVariation)

        val decisionService = DecisionService(
                mockBucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                emptyMap<String, String>()
        )
        assertEquals(expectedVariation, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource)

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(1)).bucket(any(Experiment::class.java), anyString())
    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * returns the variation of "Everyone Else" rule
     * when the user passes targeting for a rule, but was failed the traffic allocation for that rule,
     * and is bucketed successfully into the "Everyone Else" rule.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsVariationWhenUserFailsTrafficInRuleAndPassesInEveryoneElse() {
        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        val rollout = ROLLOUT_2
        val everyoneElseRule = rollout.experiments[rollout.experiments.size - 1]
        val expectedVariation = everyoneElseRule.variations[0]
        `when`<Variation>(mockBucketer.bucket(any(Experiment::class.java), anyString())).thenReturn(null)
        `when`<Variation>(mockBucketer.bucket(eq(everyoneElseRule), anyString())).thenReturn(expectedVariation)

        val decisionService = DecisionService(
                mockBucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                Collections.singletonMap(
                        ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE
                )
        )
        assertEquals(expectedVariation, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource)

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(2)).bucket(any(Experiment::class.java), anyString())
    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * returns the variation of "Everyone Else" rule
     * when the user passes targeting for a rule, but was failed the traffic allocation for that rule,
     * and is bucketed successfully into the "Everyone Else" rule.
     * Fallback bucketing should not evaluate any other audiences.
     * Even though the user would satisfy a later rollout rule, they are never evaluated for it or bucketed into it.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsVariationWhenUserFailsTrafficInRuleButWouldPassForAnotherRuleAndPassesInEveryoneElse() {
        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        val rollout = ROLLOUT_2
        val englishCitizensRule = rollout.experiments[2]
        val englishCitizenVariation = englishCitizensRule.variations[0]
        val everyoneElseRule = rollout.experiments[rollout.experiments.size - 1]
        val expectedVariation = everyoneElseRule.variations[0]
        `when`<Variation>(mockBucketer.bucket(any(Experiment::class.java), anyString())).thenReturn(null)
        `when`<Variation>(mockBucketer.bucket(eq(everyoneElseRule), anyString())).thenReturn(expectedVariation)
        `when`<Variation>(mockBucketer.bucket(eq(englishCitizensRule), anyString())).thenReturn(englishCitizenVariation)

        val decisionService = DecisionService(
                mockBucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                ProjectConfigTestUtils.createMapOfObjects(
                        ProjectConfigTestUtils.createListOfObjects(
                                ATTRIBUTE_HOUSE_KEY, ATTRIBUTE_NATIONALITY_KEY
                        ),
                        ProjectConfigTestUtils.createListOfObjects(
                                AUDIENCE_GRYFFINDOR_VALUE, AUDIENCE_ENGLISH_CITIZENS_VALUE
                        )
                )
        )
        assertEquals(expectedVariation, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource)

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(2)).bucket(any(Experiment::class.java), anyString())
    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * returns the variation of "English Citizens" rule
     * when the user fails targeting for previous rules, but passes targeting and traffic for Rule 3.
     */
    @Test
    fun getVariationForFeatureInRolloutReturnsVariationWhenUserFailsTargetingInPreviousRulesButPassesRule3() {
        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        val rollout = ROLLOUT_2
        val englishCitizensRule = rollout.experiments[2]
        val englishCitizenVariation = englishCitizensRule.variations[0]
        val everyoneElseRule = rollout.experiments[rollout.experiments.size - 1]
        val everyoneElseVariation = everyoneElseRule.variations[0]
        `when`<Variation>(mockBucketer.bucket(any(Experiment::class.java), anyString())).thenReturn(null)
        `when`<Variation>(mockBucketer.bucket(eq(everyoneElseRule), anyString())).thenReturn(everyoneElseVariation)
        `when`<Variation>(mockBucketer.bucket(eq(englishCitizensRule), anyString())).thenReturn(englishCitizenVariation)

        val decisionService = DecisionService(
                mockBucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        )

        val featureDecision = decisionService.getVariationForFeatureInRollout(
                FEATURE_FLAG_MULTI_VARIATE_FEATURE,
                genericUserId,
                Collections.singletonMap(
                        ATTRIBUTE_NATIONALITY_KEY, AUDIENCE_ENGLISH_CITIZENS_VALUE
                )
        )
        assertEquals(englishCitizenVariation, featureDecision.variation)
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT, featureDecision.decisionSource)

        // verify user is only bucketed once for everyone else rule
        verify(mockBucketer, times(1)).bucket(any(Experiment::class.java), anyString())
    }

    //========= white list tests ==========/

    /**
     * Test [DecisionService.getWhitelistedVariation] correctly returns a whitelisted variation.
     */
    @Test
    fun getWhitelistedReturnsForcedVariation() {
        val bucketer = Bucketer(validProjectConfig)
        val decisionService = DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null)

        logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \""
                + whitelistedVariation!!.key + "\".")
        assertEquals(whitelistedVariation, decisionService.getWhitelistedVariation(whitelistedExperiment!!, whitelistedUserId))
    }

    /**
     * Verify that [DecisionService.getWhitelistedVariation] returns null
     * when an invalid variation key is found in the forced variations mapping.
     */
    @Test
    @Throws(Exception::class)
    fun getWhitelistedWithInvalidVariation() {
        val userId = "testUser1"
        val invalidVariationKey = "invalidVarKey"

        val bucketer = Bucketer(validProjectConfig)
        val decisionService = DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null)

        val variations = listOf<Variation>(Variation("1", "var1"))

        val trafficAllocations = listOf<TrafficAllocation>(TrafficAllocation("1", 1000))

        val userIdToVariationKeyMap = Collections.singletonMap(userId, invalidVariationKey)

        val experiment = Experiment("1234", "exp_key", "Running", "1", emptyList<String>(),
                variations, userIdToVariationKeyMap, trafficAllocations)

        logbackVerifier.expectMessage(
                Level.ERROR,
                "Variation \"$invalidVariationKey\" is not in the datafile. Not activating user \"$userId\".")

        assertNull(decisionService.getWhitelistedVariation(experiment, userId))
    }

    /**
     * Verify that [DecisionService.getWhitelistedVariation] returns null when user is not whitelisted.
     */
    @Test
    @Throws(Exception::class)
    fun getWhitelistedReturnsNullWhenUserIsNotWhitelisted() {
        val bucketer = Bucketer(validProjectConfig)
        val decisionService = DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null)

        assertNull(decisionService.getWhitelistedVariation(whitelistedExperiment!!, genericUserId))
    }

    //======== User Profile tests =========//

    /**
     * Verify that [DecisionService.getStoredVariation] returns a variation that is
     * stored in the provided [UserProfile].
     */
    @SuppressFBWarnings
    @Test
    @Throws(Exception::class)
    fun bucketReturnsVariationStoredInUserProfile() {
        val experiment = noAudienceProjectConfig!!.experiments[0]
        val variation = experiment.variations[0]
        val decision = Decision(variation.id)

        val userProfile = UserProfile(userProfileId,
                Collections.singletonMap(experiment.id, decision))
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        `when`(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap())

        val bucketer = Bucketer(noAudienceProjectConfig)
        val decisionService = DecisionService(bucketer,
                mockErrorHandler!!,
                noAudienceProjectConfig!!,
                userProfileService)

        logbackVerifier.expectMessage(Level.INFO,
                "Returning previously activated variation \"" + variation.key + "\" of experiment \"" + experiment.key + "\""
                        + " for user \"" + userProfileId + "\" from user profile.")

        // ensure user with an entry in the user profile is bucketed into the corresponding stored variation
        assertEquals(variation,
                decisionService.getVariation(experiment, userProfileId, emptyMap<String, String>()))

        verify(userProfileService).lookup(userProfileId)
    }

    /**
     * Verify that [DecisionService.getStoredVariation] returns null and logs properly
     * when there is no stored variation for that user in that [Experiment] in the [UserProfileService].
     */
    @Test
    @Throws(Exception::class)
    fun getStoredVariationLogsWhenLookupReturnsNull() {
        val experiment = noAudienceProjectConfig!!.experiments[0]

        val bucketer = Bucketer(noAudienceProjectConfig)
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        val userProfile = UserProfile(userProfileId,
                emptyMap<String, Decision>())
        `when`(userProfileService.lookup(userProfileId)).thenReturn(userProfile.toMap())

        val decisionService = DecisionService(bucketer,
                mockErrorHandler!!, noAudienceProjectConfig!!, userProfileService)

        logbackVerifier.expectMessage(Level.INFO, "No previously activated variation of experiment " +
                "\"" + experiment.key + "\" for user \"" + userProfileId + "\" found in user profile.")

        assertNull(decisionService.getStoredVariation(experiment, userProfile))
    }

    /**
     * Verify that [DecisionService.getStoredVariation] returns null
     * when a [UserProfile] is present, contains a decision for the experiment in question,
     * but the variation ID for that decision does not exist in the datafile.
     */
    @Test
    @Throws(Exception::class)
    fun getStoredVariationReturnsNullWhenVariationIsNoLongerInConfig() {
        val experiment = noAudienceProjectConfig!!.experiments[0]
        val storedVariationId = "missingVariation"
        val storedDecision = Decision(storedVariationId)
        val storedDecisions = HashMap<String, Decision>()
        storedDecisions.put(experiment.id, storedDecision)
        val storedUserProfile = UserProfile(userProfileId,
                storedDecisions)

        val bucketer = mock<Bucketer>(Bucketer::class.java)
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        `when`(userProfileService.lookup(userProfileId)).thenReturn(storedUserProfile.toMap())

        val decisionService = DecisionService(bucketer, mockErrorHandler!!, noAudienceProjectConfig!!,
                userProfileService)

        logbackVerifier.expectMessage(Level.INFO,
                "User \"" + userProfileId + "\" was previously bucketed into variation with ID \"" + storedVariationId + "\" for " +
                        "experiment \"" + experiment.key + "\", but no matching variation " +
                        "was found for that user. We will re-bucket the user.")

        assertNull(decisionService.getStoredVariation(experiment, storedUserProfile))
    }

    /**
     * Verify that [DecisionService.getVariation]
     * saves a [Variation]of an [Experiment] for a user when a [UserProfileService] is present.
     */
    @SuppressFBWarnings
    @Test
    @Throws(Exception::class)
    fun getVariationSavesBucketedVariationIntoUserProfile() {
        val experiment = noAudienceProjectConfig!!.experiments[0]
        val variation = experiment.variations[0]
        val decision = Decision(variation.id)

        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        val originalUserProfile = UserProfile(userProfileId,
                HashMap())
        `when`(userProfileService.lookup(userProfileId)).thenReturn(originalUserProfile.toMap())
        val expectedUserProfile = UserProfile(userProfileId,
                Collections.singletonMap(experiment.id, decision))

        val mockBucketer = mock<Bucketer>(Bucketer::class.java)
        `when`<Variation>(mockBucketer.bucket(experiment, userProfileId)).thenReturn(variation)

        val decisionService = DecisionService(mockBucketer,
                mockErrorHandler!!, noAudienceProjectConfig!!, userProfileService)

        assertEquals(variation, decisionService.getVariation(experiment, userProfileId, emptyMap<String, String>()))
        logbackVerifier.expectMessage(Level.INFO,
                String.format("Saved variation \"%s\" of experiment \"%s\" for user \"$userProfileId\".", variation.id,
                        experiment.id))

        verify(userProfileService).save(eq(expectedUserProfile.toMap()))
    }

    /**
     * Verify that [DecisionService.getVariation] logs correctly
     * when a [UserProfileService] is present but fails to save an activation.
     */
    @Test
    @Throws(Exception::class)
    fun bucketLogsCorrectlyWhenUserProfileFailsToSave() {
        val experiment = noAudienceProjectConfig!!.experiments[0]
        val variation = experiment.variations[0]
        val decision = Decision(variation.id)
        val bucketer = Bucketer(noAudienceProjectConfig)
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        doThrow(Exception()).`when`(userProfileService).save(anyMapOf(String::class.java, Any::class.java))

        val experimentBucketMap = HashMap<String, Decision>()
        experimentBucketMap.put(experiment.id, decision)
        val expectedUserProfile = UserProfile(userProfileId,
                experimentBucketMap)
        val saveUserProfile = UserProfile(userProfileId,
                HashMap())

        val decisionService = DecisionService(bucketer,
                mockErrorHandler!!, noAudienceProjectConfig!!, userProfileService)


        decisionService.saveVariation(experiment, variation, saveUserProfile)

        logbackVerifier.expectMessage(Level.WARN,
                String.format("Failed to save variation \"%s\" of experiment \"%s\" for user \"$userProfileId\".", variation.id,
                        experiment.id))

        verify(userProfileService).save(eq(expectedUserProfile.toMap()))
    }

    /**
     * Verify that a [UserProfile] is saved when the user is brand new and did not have anything returned from
     * [UserProfileService.lookup].
     */
    @Test
    @Throws(Exception::class)
    fun getVariationSavesANewUserProfile() {
        val experiment = noAudienceProjectConfig!!.experiments[0]
        val variation = experiment.variations[0]
        val decision = Decision(variation.id)
        val expectedUserProfile = UserProfile(userProfileId,
                Collections.singletonMap(experiment.id, decision))

        val bucketer = mock<Bucketer>(Bucketer::class.java)
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        val decisionService = DecisionService(bucketer, mockErrorHandler!!, noAudienceProjectConfig!!,
                userProfileService)

        `when`<Variation>(bucketer.bucket(experiment, userProfileId)).thenReturn(variation)
        `when`(userProfileService.lookup(userProfileId)).thenReturn(null)

        assertEquals(variation, decisionService.getVariation(experiment, userProfileId, emptyMap<String, String>()))
        verify(userProfileService).save(expectedUserProfile.toMap())
    }

    @Test
    @Throws(Exception::class)
    fun getVariationBucketingId() {
        val bucketer = mock<Bucketer>(Bucketer::class.java)
        val decisionService = spy(DecisionService(bucketer, mockErrorHandler!!, validProjectConfig!!, null))
        val experiment = validProjectConfig!!.experiments[0]
        val expectedVariation = experiment.variations[0]

        `when`<Variation>(bucketer.bucket(experiment, "bucketId")).thenReturn(expectedVariation)

        val attr = HashMap<String, String>()
        attr.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), "bucketId")
        // user excluded without audiences and whitelisting
        assertThat<Variation>(decisionService.getVariation(experiment, genericUserId, attr), `is`(expectedVariation))

    }

    /**
     * Verify that [DecisionService.getVariationForFeatureInRollout]
     * uses bucketing ID to bucket the user into rollouts.
     */
    @Test
    fun getVariationForRolloutWithBucketingId() {
        val rolloutRuleExperiment = ROLLOUT_3_EVERYONE_ELSE_RULE
        val rolloutVariation = ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION
        val featureFlag = FEATURE_FLAG_SINGLE_VARIABLE_INTEGER
        val bucketingId = "user_bucketing_id"
        val userId = "user_id"
        val attributes = HashMap<String, String>()
        attributes.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), bucketingId)

        val bucketer = mock<Bucketer>(Bucketer::class.java)
        `when`<Variation>(bucketer.bucket(rolloutRuleExperiment, userId)).thenReturn(null)
        `when`<Variation>(bucketer.bucket(rolloutRuleExperiment, bucketingId)).thenReturn(rolloutVariation)

        val decisionService = spy(DecisionService(
                bucketer,
                mockErrorHandler!!,
                v4ProjectConfig!!, null
        ))

        val expectedFeatureDecision = FeatureDecision(
                rolloutRuleExperiment,
                rolloutVariation,
                FeatureDecision.DecisionSource.ROLLOUT)

        val featureDecision = decisionService.getVariationForFeature(featureFlag, userId, attributes)

        assertEquals(expectedFeatureDecision, featureDecision)
    }

    companion object {

        private val genericUserId = "genericUserId"
        private val whitelistedUserId = "testUser1"
        private val userProfileId = "userProfileId"

        private var noAudienceProjectConfig: ProjectConfig? = null
        private var v4ProjectConfig: ProjectConfig? = null
        private var validProjectConfig: ProjectConfig? = null
        private var whitelistedExperiment: Experiment? = null
        private var whitelistedVariation: Variation? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setUp() {
            validProjectConfig = validProjectConfigV3()
            v4ProjectConfig = validProjectConfigV4()
            noAudienceProjectConfig = noAudienceProjectConfigV3()
            whitelistedExperiment = validProjectConfig!!.experimentIdMapping["223"]
            whitelistedVariation = whitelistedExperiment!!.variationKeyToVariationMap["vtag1"]
        }
    }

}
