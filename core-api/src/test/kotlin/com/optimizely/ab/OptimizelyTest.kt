/****************************************************************************
 * Copyright 2016-2018, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab

import ch.qos.logback.classic.Level
import com.google.common.collect.ImmutableMap
import com.optimizely.ab.bucketing.Bucketer
import com.optimizely.ab.bucketing.DecisionService
import com.optimizely.ab.bucketing.FeatureDecision
import com.optimizely.ab.config.Attribute
import com.optimizely.ab.config.EventType
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.LiveVariable
import com.optimizely.ab.config.LiveVariableUsageInstance
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.parser.ConfigParseException
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.error.NoOpErrorHandler
import com.optimizely.ab.error.RaiseExceptionErrorHandler
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.event.internal.EventBuilder
import com.optimizely.ab.internal.LogbackVerifier
import com.optimizely.ab.internal.ControlAttribute
import com.optimizely.ab.notification.ActivateNotificationListener
import com.optimizely.ab.notification.NotificationCenter
import com.optimizely.ab.notification.NotificationListener
import com.optimizely.ab.notification.TrackNotificationListener
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV2
import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV3
import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4
import com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.EVENT_BASIC_EVENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_BASIC_EXPERIMENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_LAUNCHED_EXPERIMENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_PAUSED_EXPERIMENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_MULTI_VARIATE_FEATURE
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_FLAG_SINGLE_VARIABLE_INTEGER
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_MULTI_VARIATE_FEATURE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_SINGLE_VARIABLE_DOUBLE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.FEATURE_SINGLE_VARIABLE_INTEGER_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED
import com.optimizely.ab.config.ValidProjectConfigV4.PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL
import com.optimizely.ab.config.ValidProjectConfigV4.ROLLOUT_2_ID
import com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_BOOLEAN_VARIABLE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_DOUBLE_DEFAULT_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_DOUBLE_VARIABLE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_FIRST_LETTER_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.VARIABLE_INTEGER_VARIABLE_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.VARIATION_MULTIVARIATE_EXPERIMENT_GRED
import com.optimizely.ab.config.ValidProjectConfigV4.VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY
import com.optimizely.ab.event.LogEvent.RequestMethod
import com.optimizely.ab.event.internal.EventBuilderTest.Companion.createExperimentVariationMap
import java.util.Arrays.asList
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.mockito.Matchers.any
import org.mockito.Matchers.anyMapOf
import org.mockito.Matchers.anyString
import org.mockito.Matchers.eq
import org.mockito.Matchers.isNull
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Tests for the top-level [Optimizely] class.
 */
@RunWith(Parameterized::class)
 class OptimizelyTest(private val datafileVersion:Int,
private val validDatafile:String,
private val noAudienceDatafile:String,
private val validProjectConfig:ProjectConfig,
private val noAudienceProjectConfig:ProjectConfig) {

@Rule
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
 var rule = MockitoJUnit.rule()

@Rule
 var thrown = ExpectedException.none()

@Rule
 var logbackVerifier = LogbackVerifier()

@Mock internal var mockEventHandler:EventHandler? = null
@Mock internal var mockBucketer:Bucketer? = null
@Mock internal var mockDecisionService:DecisionService? = null
@Mock internal var mockErrorHandler:ErrorHandler? = null

 //======== activate tests ========//

    /**
 * Verify that the [Optimizely.activate] call correctly builds an endpoint url and
 * request params and passes them through [EventHandler.dispatchEvent].
 */
    @Test
@Throws(Exception::class)
 fun activateEndToEnd() {
val activatedExperiment:Experiment
val testUserAttributes = HashMap<String, String>()
val bucketingKey = testBucketingIdKey
val userId = testUserId
val bucketingId = testBucketingId
if (datafileVersion >= 4)
{
activatedExperiment = validProjectConfig.experimentKeyMapping!![EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]!!
testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
activatedExperiment = validProjectConfig.experiments[0]
testUserAttributes.put("browser_type", "chrome")
}
testUserAttributes.put(bucketingKey, bucketingId)
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()

testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, testUserId,
testUserAttributes))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, bucketingId))
.thenReturn(bucketedVariation)

logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
activatedExperiment.key + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
testParams + " and payload \"\"")

 // activate the experiment
        val actualVariation = optimizely.activate(activatedExperiment.key, userId, testUserAttributes)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, bucketingId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that the [Optimizely.activate] DOES NOT dispatch an impression event
 * when the user isn't bucketed to a variation.
 */
    @Test
@Throws(Exception::class)
 fun activateForNullVariation() {
val activatedExperiment = validProjectConfig.experiments[0]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")
testUserAttributes.put(testBucketingIdKey,
testBucketingId)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testBucketingId))
.thenReturn(null)

logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
activatedExperiment.key + "\".")

 // activate the experiment
        val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, testUserAttributes)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testBucketingId)
assertNull(actualVariation)

 // verify that dispatchEvent was NOT called
        verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify the case were [Optimizely.activate] is called with an [Experiment]
 * that is not present in the current [ProjectConfig]. We should NOT throw an error in that case.
 *
 * This may happen if an experiment is retrieved from the project config, the project config is updated and the
 * referenced experiment removed, then activate is called given the now removed experiment.
 * Could also happen if an experiment was manually created and passed through.
 */
    @Test
@Throws(Exception::class)
 fun activateWhenExperimentIsNotInProject() {
val unknownExperiment = createUnknownExperiment()
val bucketedVariation = unknownExperiment.variations[0]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(validProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

`when`<Variation>(mockBucketer!!.bucket(unknownExperiment, testUserId))
.thenReturn(bucketedVariation)

optimizely.activate(unknownExperiment, testUserId)
}

/**
 * Verify that the [&lt;][Optimizely.activate] call
 * uses forced variation to force the user into the second variation.  The mock bucket returns
 * the first variation. Then remove the forced variation and confirm that the forced variation is null.
 */
    @Test
@Throws(Exception::class)
 fun activateWithExperimentKeyForced() {
val activatedExperiment = validProjectConfig.experiments[0]
val forcedVariation = activatedExperiment.variations[1]
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

optimizely.setForcedVariation(activatedExperiment.key, testUserId, forcedVariation.key)

val testUserAttributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
testUserAttributes.put("browser_type", "chrome")
}

testUserAttributes.put(testBucketingIdKey,
testBucketingId)

val testParams = HashMap<String, String>()
testParams.put("test", "params")

val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
eq(testUserId), eq<Map<String, String>>(testUserAttributes)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testBucketingId))
.thenReturn(bucketedVariation)

 // activate the experiment
        val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, testUserAttributes)

assertThat<Variation>(actualVariation, `is`(forcedVariation))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)

optimizely.setForcedVariation(activatedExperiment.key, testUserId, null)

assertEquals(optimizely.getForcedVariation(activatedExperiment.key, testUserId), null)

}

/**
 * Verify that the [&lt;][Optimizely.getVariation] call
 * uses forced variation to force the user into the second variation.  The mock bucket returns
 * the first variation. Then remove the forced variation and confirm that the forced variation is null.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithExperimentKeyForced() {
val activatedExperiment = validProjectConfig.experiments[0]
val forcedVariation = activatedExperiment.variations[1]
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

optimizely.setForcedVariation(activatedExperiment.key, testUserId, forcedVariation.key)

val testUserAttributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
testUserAttributes.put("browser_type", "chrome")
}

testUserAttributes.put(testBucketingIdKey,
testBucketingId)

val testParams = HashMap<String, String>()
testParams.put("test", "params")

val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(forcedVariation),
eq(testUserId), eq<Map<String, String>>(testUserAttributes)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testBucketingId))
.thenReturn(bucketedVariation)

 // activate the experiment
        var actualVariation = optimizely.getVariation(activatedExperiment.key, testUserId, testUserAttributes)

assertThat<Variation>(actualVariation, `is`(forcedVariation))

optimizely.setForcedVariation(activatedExperiment.key, testUserId, null)

assertEquals(optimizely.getForcedVariation(activatedExperiment.key, testUserId), null)

actualVariation = optimizely.getVariation(activatedExperiment.key, testUserId, testUserAttributes)

assertThat<Variation>(actualVariation, `is`(bucketedVariation))
}

/**
 * Verify that the [&lt;][Optimizely.activate] call
 * uses forced variation to force the user into the second variation.  The mock bucket returns
 * the first variation. Then remove the forced variation and confirm that the forced variation is null.
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledWithExperimentKeyForced() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val activatedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]
val forcedVariation = activatedExperiment!!.variations[1]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

optimizely.setForcedVariation(activatedExperiment.key, testUserId, forcedVariation.key)

val testUserAttributes = HashMap<String, String>()
if (datafileVersion < 4)
{
testUserAttributes.put("browser_type", "chrome")
}

val testParams = HashMap<String, String>()
testParams.put("test", "params")

val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq<Experiment>(activatedExperiment), eq(forcedVariation),
eq(testUserId), eq<Map<String, String>>(testUserAttributes)))
.thenReturn(logEventToDispatch)

 // activate the experiment
        assertTrue(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.key, testUserId))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)

assertTrue(optimizely.setForcedVariation(activatedExperiment.key, testUserId, null))

assertNull(optimizely.getForcedVariation(activatedExperiment.key, testUserId))

assertFalse(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.key, testUserId))

}

/**
 * Verify that the [&lt;][Optimizely.activate] call
 * correctly builds an endpoint url and request params
 * and passes them through [EventHandler.dispatchEvent].
 */
    @Test
@Throws(Exception::class)
 fun activateWithExperimentKey() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]
val userIdBucketVariation = activatedExperiment.variations[1]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
testUserAttributes.put("browser_type", "chrome")
}

testUserAttributes.put(testBucketingIdKey, testBucketingId)

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), eq<Map<String, String>>(testUserAttributes)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId))
.thenReturn(userIdBucketVariation)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testBucketingId))
.thenReturn(bucketedVariation)

 // activate the experiment
        val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, testUserAttributes)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testBucketingId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.activate] handles the case where an unknown experiment
 * (i.e., not in the config) is passed through and a [NoOpErrorHandler] is used by default.
 */
    @Test
@Throws(Exception::class)
 fun activateWithUnknownExperimentKeyAndNoOpErrorHandler() {
val unknownExperiment = createUnknownExperiment()

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile.")
logbackVerifier.expectMessage(Level.INFO,
"Not activating user \"userId\" for experiment \"unknown_experiment\".")

 // since we use a NoOpErrorHandler, we should fail and return null
        val actualVariation = optimizely.activate(unknownExperiment.key, testUserId)

 // verify that null is returned, as no project config was available
        assertNull(actualVariation)
}

/**
 * Verify that [Optimizely.activate] handles the case where an unknown experiment
 * (i.e., not in the config) is passed through and a [RaiseExceptionErrorHandler] is provided.
 */
    @Test
@Throws(Exception::class)
 fun activateWithUnknownExperimentKeyAndRaiseExceptionErrorHandler() {
thrown.expect(UnknownExperimentException::class.java)

val unknownExperiment = createUnknownExperiment()

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

 // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.activate(unknownExperiment.key, testUserId)
}

/**
 * Verify that [Optimizely.activate] passes through attributes.
 */
    @Test
@Throws(Exception::class)
 fun activateWithAttributes() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]
val userIdBucketedVariation = activatedExperiment.variations[1]
val attribute = validProjectConfig.attributes[0]

 // setup a mock event builder to return expected impression params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), anyMapOf(String::class.java, String::class.java)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId))
.thenReturn(userIdBucketedVariation)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testBucketingId))
.thenReturn(bucketedVariation)

val attr = HashMap<String, String>()
attr.put(attribute.key, "attributeValue")
attr.put(testBucketingIdKey, testBucketingId)

 // activate the experiment
        val actualVariation = optimizely.activate(activatedExperiment.key, testUserId,
attr)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testBucketingId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)
verify(mockEventBuilder).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
eq(bucketedVariation), eq(testUserId), attributeCaptor.capture() as Map<String, String>)

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, hasEntry(attribute.key, "attributeValue"))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [&lt;][Optimizely.activate] handles the case
 * where an unknown attribute (i.e., not in the config) is passed through.
 *
 * In this case, the activate call should remove the unknown attribute from the given map.
 */
    @Test
@Throws(Exception::class)
 fun activateWithUnknownAttribute() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]

 // setup a mock event builder to return mock params and endpoint
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

val testUserAttributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
testUserAttributes.put("browser_type", "chrome")
}
testUserAttributes.put("unknownAttribute", "dimValue")

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), anyMapOf(String::class.java, String::class.java)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId))
.thenReturn(bucketedVariation)

logbackVerifier.expectMessage(Level.INFO, "Activating user \"userId\" in experiment \"" +
activatedExperiment.key + "\".")
logbackVerifier.expectMessage(Level.WARN, "Attribute(s) [unknownAttribute] not in the datafile.")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching impression event to URL test_url with params " +
testParams + " and payload \"\"")

 // Use an immutable map to also check that we're not attempting to change the provided attribute map
        val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, testUserAttributes)

assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
eq(bucketedVariation), eq(testUserId), attributeCaptor.capture() as Map<String, String>)

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, not(hasKey("unknownAttribute")))

 // verify that dispatchEvent was called with the correct LogEvent object.
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.activate] ignores null attributes.
 */
    @Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun activateWithNullAttributes() {
val activatedExperiment = noAudienceProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]

 // setup a mock event builder to return expected impression params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), eq<Map<String, String>>(emptyMap<String, String>())))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!!!.bucket(activatedExperiment, testUserId))
.thenReturn(bucketedVariation)

 // activate the experiment
        val attributes:MutableMap<String, String>? = null
val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, attributes!!)

logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.")

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)
verify(mockEventBuilder).createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment),
eq(bucketedVariation), eq(testUserId), attributeCaptor.capture() as Map<String, String>)

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, `is`<Map<String, String>>(emptyMap<String, String>()))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.activate] gracefully handles null attribute values.
 */
    @Test
@Throws(Exception::class)
 fun activateWithNullAttributeValues() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]
val attribute = validProjectConfig.attributes[0]

 // setup a mock event builder to return expected impression params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), anyMapOf(String::class.java, String::class.java)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId))
.thenReturn(bucketedVariation)

 // activate the experiment
        val attributes = HashMap<String, String>()
//attributes.put(attribute.key, null)
val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, attributes)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)
verify(mockEventBuilder).createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment),
eq(bucketedVariation), eq(testUserId), attributeCaptor.capture() as Map<String, String>)

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, hasEntry<String, Any>(attribute.key, null))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.activate] returns null when the experiment id corresponds to a
 * non-running experiment.
 */
    @Test
@Throws(Exception::class)
 fun activateDraftExperiment() {
val inactiveExperiment:Experiment
if (datafileVersion == 4)
{
inactiveExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_PAUSED_EXPERIMENT_KEY]!!
}
else
{
inactiveExperiment = validProjectConfig.experiments[1]
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + inactiveExperiment.key +
"\" is not running.")
logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
inactiveExperiment.key + "\".")

val variation = optimizely.activate(inactiveExperiment.key, testUserId)

 // verify that null is returned, as the experiment isn't running
        assertNull(variation)
}

/**
 * Verify that a user who falls in an experiment's audience is assigned a variation.
 */
    @Test
@Throws(Exception::class)
 fun activateUserInAudience() {
val experimentToCheck = validProjectConfig.experiments[0]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")

val actualVariation = optimizely.activate(experimentToCheck.key, testUserId, testUserAttributes)
assertNotNull(actualVariation)
}

/**
 * Verify that if user ID sent is null will return null variation.
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(Exception::class)
 fun activateUserIDIsNull() {
val experimentToCheck = validProjectConfig.experiments[0]
val nullUserID:String? = null
val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")

val nullVariation = optimizely.activate(experimentToCheck.key, nullUserID!!, testUserAttributes)
assertNull(nullVariation)

logbackVerifier.expectMessage(
Level.ERROR,
"The user ID parameter must be nonnull."
)
}

/**
 * Verify that if user ID sent is null will return null variation.
 * In activate override function where experiment object is passed
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(Exception::class)
 fun activateWithExperimentUserIDIsNull() {
val experimentToCheck = validProjectConfig.experiments[0]
val nullUserID:String? = null
val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")

val nullVariation = optimizely.activate(experimentToCheck, nullUserID!!, testUserAttributes)
assertNull(nullVariation)

logbackVerifier.expectMessage(
Level.ERROR,
"The user ID parameter must be nonnull."
)
}

/**
 * Verify that if Experiment key sent is null will return null variation.
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(Exception::class)
 fun activateExperimentKeyIsNull() {
val nullExperimentKey:String? = null
val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")

val nullVariation = optimizely.activate(nullExperimentKey!!, testUserId, testUserAttributes)
assertNull(nullVariation)

logbackVerifier.expectMessage(
Level.ERROR,
"The experimentKey parameter must be nonnull."
)
}
/**
 * Verify that a user not in any of an experiment's audiences isn't assigned to a variation.
 */
    @Test
@Throws(Exception::class)
 fun activateUserNotInAudience() {
val experimentToCheck:Experiment
if (datafileVersion == 4)
{
experimentToCheck = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]!!
}
else
{
experimentToCheck = validProjectConfig.experiments[0]
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "firefox")

logbackVerifier.expectMessage(Level.INFO,
"User \"userId\" does not meet conditions to be in experiment \"" +
experimentToCheck.key + "\".")
logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
experimentToCheck.key + "\".")

val actualVariation = optimizely.activate(experimentToCheck.key, testUserId, testUserAttributes)
assertNull(actualVariation)
}

/**
 * Verify that when no audiences are provided, the user is included in the experiment (i.e., no audiences means
 * the experiment is targeted to "everyone").
 */
    @Test
@Throws(Exception::class)
 fun activateUserWithNoAudiences() {
val experimentToCheck = noAudienceProjectConfig.experiments[0]

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withErrorHandler(mockErrorHandler!!)
.build()

assertNotNull(optimizely.activate(experimentToCheck.key, testUserId))
}

/**
 * Verify that when an experiment has audiences, but no attributes are provided, the user is not assigned a
 * variation.
 */
    @Test
@Throws(Exception::class)
 fun activateUserNoAttributesWithAudiences() {
val experiment:Experiment
if (datafileVersion == 4)
{
experiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]!!
}
else
{
experiment = validProjectConfig.experiments[0]
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.build()

logbackVerifier.expectMessage(Level.INFO,
"User \"userId\" does not meet conditions to be in experiment \"" + experiment.key + "\".")
logbackVerifier.expectMessage(Level.INFO, "Not activating user \"userId\" for experiment \"" +
experiment.key + "\".")

assertNull(optimizely.activate(experiment.key, testUserId))
}

/**
 * Verify that [Optimizely.activate] doesn't return a variation when provided an empty string.
 */
    @Test
@Throws(Exception::class)
 fun activateWithEmptyUserId() {
val experiment = noAudienceProjectConfig.experiments[0]
val experimentKey = experiment.key

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

logbackVerifier.expectMessage(Level.ERROR, "Non-empty user ID required")
logbackVerifier.expectMessage(Level.INFO, "Not activating user for experiment \"$experimentKey\".")
assertNull(optimizely.activate(experimentKey, ""))
}

/**
 * Verify that [Optimizely.activate] returns a variation when given matching
 * user attributes.
 */
    @Test
@Throws(Exception::class)
 fun activateForGroupExperimentWithMatchingAttributes() {
val experiment = validProjectConfig.groups[0]
                .experiments[0]
val variation = experiment.variations[0]

val attributes = HashMap<String, String>()
if (datafileVersion == 4)
{
attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
attributes.put("browser_type", "chrome")
}

`when`<Variation>(mockBucketer!!.bucket(experiment, "user")).thenReturn(variation)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withBucketing(mockBucketer!!)
.build()

assertThat<Variation>(optimizely.activate(experiment.key, "user", attributes),
`is`(variation))
}

/**
 * Verify that [Optimizely.activate] doesn't return a variation when given
 * non-matching user attributes.
 */
    @Test
@Throws(Exception::class)
 fun activateForGroupExperimentWithNonMatchingAttributes() {
val experiment = validProjectConfig.groups[0]
                .experiments[0]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

val experimentKey = experiment.key
logbackVerifier.expectMessage(
Level.INFO,
"User \"user\" does not meet conditions to be in experiment \"$experimentKey\".")
logbackVerifier.expectMessage(Level.INFO,
"Not activating user \"user\" for experiment \"$experimentKey\".")
assertNull(optimizely.activate(experiment.key, "user",
Collections.singletonMap("browser_type", "firefox")))
}

/**
 * Verify that [Optimizely.activate] gives precedence to forced variation bucketing
 * over audience evaluation.
 */
    @Test
@Throws(Exception::class)
 fun activateForcedVariationPrecedesAudienceEval() {
val experiment:Experiment
val whitelistedUserId:String
val expectedVariation:Variation
if (datafileVersion == 4)
{
experiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]!!
whitelistedUserId = MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED
expectedVariation = experiment.variationKeyToVariationMap[VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY]!!
}
else
{
experiment = validProjectConfig.experiments[0]
whitelistedUserId = "testUser1"
expectedVariation = experiment.variations[0]
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

logbackVerifier.expectMessage(Level.INFO, "User \"" + whitelistedUserId + "\" is forced in variation \"" +
expectedVariation.key + "\".")
 // no attributes provided for a experiment that has an audience
        assertTrue(experiment.userIdToVariationKeyMap.containsKey(whitelistedUserId))
assertThat<Variation>(optimizely.activate(experiment.key, whitelistedUserId), `is`(expectedVariation))
}

/**
 * Verify that [Optimizely.activate] gives precedence to experiment status over forced
 * variation bucketing.
 */
    @Test
@Throws(Exception::class)
 fun activateExperimentStatusPrecedesForcedVariation() {
val experiment:Experiment
val whitelistedUserId:String
if (datafileVersion == 4)
{
experiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_PAUSED_EXPERIMENT_KEY]!!
whitelistedUserId = PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL
}
else
{
experiment = validProjectConfig.experiments[1]
whitelistedUserId = "testUser3"
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.key + "\" is not running.")
logbackVerifier.expectMessage(Level.INFO, "Not activating user \"" + whitelistedUserId +
"\" for experiment \"" + experiment.key + "\".")
 // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertTrue(experiment.userIdToVariationKeyMap.containsKey(whitelistedUserId))
assertNull(optimizely.activate(experiment.key, whitelistedUserId))
}

/**
 * Verify that [Optimizely.activate] handles exceptions thrown by
 * [EventHandler.dispatchEvent] gracefully.
 */
    @Test
@Throws(Exception::class)
 fun activateDispatchEventThrowsException() {
val experiment = noAudienceProjectConfig.experiments[0]

doThrow(Exception("Test Exception")).`when`<EventHandler>(mockEventHandler).dispatchEvent(any(LogEvent::class.java))

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.build()

logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher")
optimizely.activate(experiment.key, testUserId)
}

/**
 * Verify that [Optimizely.activate] doesn't dispatch an event for an experiment with a
 * "Launched" status.
 */
    @Test
@Throws(Exception::class)
 fun activateLaunchedExperimentDoesNotDispatchEvent() {
val launchedExperiment:Experiment
if (datafileVersion == 4)
{
launchedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_LAUNCHED_EXPERIMENT_KEY]!!
}
else
{
launchedExperiment = noAudienceProjectConfig.experiments[2]
}

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(noAudienceProjectConfig)
.build()

val expectedVariation = launchedExperiment.variations[0]

`when`<Variation>(mockBucketer!!.bucket(launchedExperiment, testUserId))
.thenReturn(launchedExperiment.variations[0])

logbackVerifier.expectMessage(Level.INFO,
"Experiment has \"Launched\" status so not dispatching event during activation.")
val variation = optimizely.activate(launchedExperiment.key, testUserId)

assertNotNull(variation)
assertThat(variation!!.key, `is`(expectedVariation.key))

 // verify that we did NOT dispatch an event
        verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

 //======== track tests ========//

    /**
 * Verify that the [Optimizely.track] call correctly builds a V2 event and passes it
 * through [EventHandler.dispatchEvent].
 */
    @Test
@Throws(Exception::class)
 fun trackEventEndToEndForced() {
val eventType:EventType
val datafile:String
val config:ProjectConfig
if (datafileVersion >= 4)
{
config = spy(validProjectConfig)
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
datafile = validDatafile
}
else
{
config = spy(noAudienceProjectConfig)
eventType = noAudienceProjectConfig.eventTypes[0]
datafile = noAudienceDatafile
}
val allExperiments = ArrayList<Experiment>()
allExperiments.add(config.experiments[0])
val eventBuilder = EventBuilder()
val spyDecisionService = spy(DecisionService(mockBucketer!!,
mockErrorHandler!!,
config, null))

val optimizely = Optimizely.builder(datafile, mockEventHandler!!)
.withDecisionService(spyDecisionService)
.withEventBuilder(eventBuilder)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

 // Bucket to null for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (experiment in allExperiments)
{
`when`<Variation>(mockBucketer!!.bucket(experiment, testUserId))
.thenReturn(null)
}
 // Force to the first variation for all experiments. However, only a subset of the experiments will actually
        // call get forced.
        for (experiment in allExperiments)
{
optimizely.projectConfig.setForcedVariation(experiment.key,
testUserId, experiment.variations[0].key)
}

 // call track
        optimizely.track(eventType.key, testUserId)

 // verify that the bucketing algorithm was called only on experiments corresponding to the specified goal.
        val experimentsForEvent = config.getExperimentsForEventKey(eventType.key)
for (experiment in allExperiments)
{
if (experiment.isRunning && experimentsForEvent.contains(experiment))
{
verify(spyDecisionService).getVariation(experiment, testUserId,
emptyMap<String, String>())
verify(config).getForcedVariation(experiment.key, testUserId)
}
else
{
verify(spyDecisionService, never()).getVariation(experiment, testUserId,
emptyMap<String, String>())
}
}

 // verify that dispatchEvent was called
        verify<EventHandler>(mockEventHandler).dispatchEvent(any(LogEvent::class.java))

for (experiment in allExperiments)
{
assertEquals(optimizely.projectConfig.getForcedVariation(experiment.key, testUserId), experiment.variations[0])
optimizely.projectConfig.setForcedVariation(experiment.key, testUserId, null)
assertNull(optimizely.projectConfig.getForcedVariation(experiment.key, testUserId))
}

}

/**
 * Verify that the [Optimizely.track] call correctly builds a V2 event and passes it
 * through [EventHandler.dispatchEvent].
 */
    @Test
@Throws(Exception::class)
 fun trackEventEndToEnd() {
val eventType:EventType
val datafile:String
val config:ProjectConfig
if (datafileVersion >= 4)
{
config = spy(validProjectConfig)
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
datafile = validDatafile
}
else
{
config = spy(noAudienceProjectConfig)
eventType = noAudienceProjectConfig.eventTypes[0]
datafile = noAudienceDatafile
}
val allExperiments = config.experiments

val eventBuilder = EventBuilder()
val spyDecisionService = spy(DecisionService(mockBucketer!!,
mockErrorHandler!!,
config, null))

val optimizely = Optimizely.builder(datafile, mockEventHandler!!)
.withDecisionService(spyDecisionService)
.withEventBuilder(eventBuilder)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

 // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (experiment in allExperiments)
{
`when`<Variation>(mockBucketer!!.bucket(experiment, testUserId))
.thenReturn(experiment.variations[0])
}

 // call track
        optimizely.track(eventType.key, testUserId)

 // verify that the bucketing algorithm was called only on experiments corresponding to the specified goal.
        val experimentsForEvent = config.getExperimentsForEventKey(eventType.key)
for (experiment in allExperiments)
{
if (experiment.isRunning && experimentsForEvent.contains(experiment))
{
verify(spyDecisionService).getVariation(experiment, testUserId,
emptyMap<String, String>())
verify(config).getForcedVariation(experiment.key, testUserId)
}
else
{
verify(spyDecisionService, never()).getVariation(experiment, testUserId,
emptyMap<String, String>())
}
}

 // verify that dispatchEvent was called
        verify<EventHandler>(mockEventHandler).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that [Optimizely.track] handles the case where an unknown event type
 * (i.e., not in the config) is passed through and a [NoOpErrorHandler] is used by default.
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithUnknownEventKeyAndNoOpErrorHandler() {
val unknownEventType = createUnknownEventType()

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(NoOpErrorHandler())
.build()

logbackVerifier.expectMessage(Level.ERROR, "Event \"unknown_event_type\" is not in the datafile.")
logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"unknown_event_type\" for user \"userId\".")
optimizely.track(unknownEventType.key, testUserId)

 // verify that we did NOT dispatch an event
        verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that [Optimizely.track] handles the case where an unknown event type
 * (i.e., not in the config) is passed through and a [RaiseExceptionErrorHandler] is provided.
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithUnknownEventKeyAndRaiseExceptionErrorHandler() {
thrown.expect(UnknownEventTypeException::class.java)

val unknownEventType = createUnknownEventType()

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

 // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.track(unknownEventType.key, testUserId)
}

/**
 * Verify that [Optimizely.track] passes through attributes.
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithAttributes() {
val attribute = validProjectConfig.attributes[0]
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val attributes = ImmutableMap.of(attribute.key, "attributeValue")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
attributes)
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
anyMapOf(String::class.java, String::class.java),
eq<Map<String, Any>>(emptyMap<String, Any>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

 // call track
        optimizely.track(eventType.key, genericUserId, attributes)

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
attributeCaptor.capture() as Map<String, String>,
eq<Map<String, Any>>(emptyMap<String, Any>()))

val actualValue = attributeCaptor.value
//assertThat<Map<String, String>>(actualValue, hasEntry(attribute.key, "attributeValue"))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.track] ignores null attributes.
 */
    @Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun trackEventWithNullAttributes() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, Any>>(emptyMap<String, Any>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

 // call track
        val attributes:MutableMap<String, String>? = null
optimizely.track(eventType.key, genericUserId, attributes!!)

logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.")

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
attributeCaptor.capture() as Map<String, String>,
eq<Map<String, Any>>(emptyMap<String, Any>()))

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, `is`<Map<String, String>>(emptyMap<String, String>()))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.track] gracefully handles null attribute values.
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithNullAttributeValues() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, Any>>(emptyMap<String, Any>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

 // call track
        val attributes = HashMap<String, String>()
//attributes.put("test", null)
optimizely.track(eventType.key, genericUserId, attributes)

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
attributeCaptor.capture() as Map<String, String>,
eq<Map<String, Any>>(emptyMap<String, Any>()))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.track] handles the case where an unknown attribute
 * (i.e., not in the config) is passed through.
 *
 * In this case, the track event call should remove the unknown attribute from the given map.
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithUnknownAttribute() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
anyMapOf(String::class.java, String::class.java),
eq<Map<String, Any>>(emptyMap<String, Any>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.WARN, "Attribute(s) [unknownAttribute] not in the datafile.")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

 // call track
        optimizely.track(eventType.key, genericUserId, ImmutableMap.of("unknownAttribute", "attributeValue"))

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
attributeCaptor.capture() as Map<String, String>,
eq<Map<String, Any>>(emptyMap<String, Any>()))

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, not(hasKey("unknownAttribute")))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.track] passes event features to
 * [EventBuilder.createConversionEvent]
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithEventTags() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")

val eventTags = HashMap<String, Any>()
eventTags.put("int_param", 123)
eventTags.put("string_param", "123")
eventTags.put("boolean_param", false)
eventTags.put("float_param", 12.3f)
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())

val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
anyMapOf(String::class.java, String::class.java),
eq<Map<String, Any>>(eventTags)))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key + "\" for user \""
+ genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

 // call track
        optimizely.track(eventType.key, genericUserId, emptyMap<String, String>(), eventTags)

 // setup the event map captor (so we can verify its content)
        val eventTagCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eventTagCaptor.capture() as Map<String, *>)

val actualValue = eventTagCaptor.value
assertThat<Map<String, *>>(actualValue as Map<String, *>, hasEntry<String, Any>("int_param", eventTags["int_param"]))
assertThat<Map<String, *>>(actualValue, hasEntry<String, Any>("string_param", eventTags["string_param"]))
assertThat<Map<String, *>>(actualValue, hasEntry<String, Any>("boolean_param", eventTags["boolean_param"]))
assertThat<Map<String, *>>(actualValue, hasEntry<String, Any>("float_param", eventTags["float_param"]))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [Optimizely.track] called with null event tags will default to
 * an empty map when calling [EventBuilder.createConversionEvent]
 */
    @Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun trackEventWithNullEventTags() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, String>>(emptyMap<String, String>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

 // call track
        optimizely.track(eventType.key, genericUserId, emptyMap<String, String>())

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, String>>(emptyMap<String, String>()))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}


/**
 * Verify that [Optimizely.track] called with null User ID will return and will not track
 */
    @Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun trackEventWithNullOrEmptyUserID() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}
 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, String>>(emptyMap<String, String>())))
.thenReturn(logEventToDispatch)

val userID:String? = null
 // call track with null event key
        optimizely.track(eventType.key, userID!!, emptyMap<String, String>(), emptyMap<String, Any>())
logbackVerifier.expectMessage(Level.ERROR, "The user ID parameter must be nonnull.")
logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"" + eventType.key + "\".")
}

/**
 * Verify that [Optimizely.track] called with null event name will return and will not track
 */
    @Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun trackEventWithNullOrEmptyEventKey() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}
val nullEventKey:String? = null
 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, String>>(emptyMap<String, String>())))
.thenReturn(logEventToDispatch)

 // call track with null event key
        optimizely.track(nullEventKey!!, genericUserId, emptyMap<String, String>(), emptyMap<String, Any>())
logbackVerifier.expectMessage(Level.ERROR, "Event Key is null or empty when non-null and non-empty String was expected.")
logbackVerifier.expectMessage(Level.INFO, "Not tracking event for user \"$genericUserId\".")

}
/**
 * Verify that [Optimizely.track] doesn't dispatch an event when no valid experiments
 * correspond to an event.
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithNoValidExperiments() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventNameMapping["clicked_purchase"]!!
}

`when`<Variation>(mockDecisionService!!!!.getVariation(any(Experiment::class.java), any(String::class.java), anyMapOf(String::class.java, String::class.java)))
.thenReturn(null)
val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withDecisionService(mockDecisionService!!)
.build()

val attributes = HashMap<String, String>()
attributes.put("browser_type", "firefox")

logbackVerifier.expectMessage(Level.INFO,
"There are no valid experiments for event \"" + eventType.key + "\" to track.")
logbackVerifier.expectMessage(Level.INFO, "Not tracking event \"" + eventType.key +
"\" for user \"userId\".")
optimizely.track(eventType.key, testUserId, attributes)

verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that [Optimizely.track] handles exceptions thrown by
 * [EventHandler.dispatchEvent] gracefully.
 */
    @Test
@Throws(Exception::class)
 fun trackDispatchEventThrowsException() {
val eventType = noAudienceProjectConfig.eventTypes[0]

doThrow(Exception("Test Exception")).`when`<EventHandler>(mockEventHandler).dispatchEvent(any(LogEvent::class.java))

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.build()

logbackVerifier.expectMessage(Level.ERROR, "Unexpected exception in event dispatcher")
optimizely.track(eventType.key, testUserId)
}

/**
 * Verify that [Optimizely.track]
 * doesn't dispatch events when the event links only to launched experiments
 */
    @Test
@Throws(Exception::class)
 fun trackDoesNotSendEventWhenExperimentsAreLaunchedOnly() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY]!!
}
else
{
eventType = noAudienceProjectConfig.eventNameMapping["launched_exp_event"]!!
}
val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)
for (experiment in noAudienceProjectConfig.experiments)
{
val variation = experiment.variations[0]
`when`<Variation>(mockBucketAlgorithm.bucket(
eq(experiment),
eq(genericUserId)))
.thenReturn(variation)
}

val client = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withBucketing(mockBucketAlgorithm)
.build()

val eventExperiments = noAudienceProjectConfig.getExperimentsForEventKey(eventType.key)
for (experiment in eventExperiments)
{
logbackVerifier.expectMessage(
Level.INFO,
"Not tracking event \"" + eventType.key + "\" for experiment \"" + experiment.key +
"\" because experiment has status \"Launched\"."
)
}

logbackVerifier.expectMessage(
Level.INFO,
"There are no valid experiments for event \"" + eventType.key + "\" to track."
)
logbackVerifier.expectMessage(
Level.INFO,
"Not tracking event \"" + eventType.key + "\" for user \"" + genericUserId + "\"."
)

 // only 1 experiment uses the event and it has a "Launched" status so experimentsForEvent map is empty
        // and the returned event will be null
        // this means we will never call the dispatcher
        client.track(eventType.key, genericUserId, emptyMap<String, String>())
 // bucket should never be called since experiments are launched so we never get variation for them
        verify(mockBucketAlgorithm, never()).bucket(
any(Experiment::class.java),
anyString())
verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that [Optimizely.track]
 * dispatches log events when the tracked event links to both launched and running experiments.
 */
    @Test
@Throws(Exception::class)
 fun trackDispatchesWhenEventHasLaunchedAndRunningExperiments() {
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = noAudienceProjectConfig.eventNameMapping["event_with_launched_and_running_experiments"]!!
}
val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)
for (experiment in validProjectConfig.experiments)
{
`when`<Variation>(mockBucketAlgorithm.bucket(experiment, genericUserId))
.thenReturn(experiment.variations[0])
}

val client = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withBucketing(mockBucketAlgorithm)
.withEventBuilder(mockEventBuilder)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
noAudienceProjectConfig,
client.decisionService,
eventType.key,
genericUserId,
emptyMap<String, String>())

 // Create an Argument Captor to ensure we are creating a correct experiment variation map
        val experimentVariationMapCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

val conversionEvent = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(noAudienceProjectConfig),
experimentVariationMapCaptor.capture() as Map<Experiment, Variation>,
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, Any>>(emptyMap<String, Any>())
)).thenReturn(conversionEvent)

val eventExperiments = noAudienceProjectConfig.getExperimentsForEventKey(eventType.key)
for (experiment in eventExperiments)
{
if (experiment.isLaunched)
{
logbackVerifier.expectMessage(
Level.INFO,
"Not tracking event \"" + eventType.key + "\" for experiment \"" + experiment.key +
"\" because experiment has status \"Launched\"."
)
}
}

 // The event has 1 launched experiment and 1 running experiment.
        // It should send a track event with the running experiment
        client.track(eventType.key, genericUserId, emptyMap<String, String>())
verify(client.eventHandler).dispatchEvent(eq(conversionEvent))

 // Check the argument captor got the correct arguments
        val actualExperimentVariationMap = experimentVariationMapCaptor.value
assertEquals(experimentVariationMap, actualExperimentVariationMap)
}

/**
 * Verify that an event is not dispatched if a user doesn't satisfy audience conditions for an experiment.
 */
    @Test
@Throws(Exception::class)
 fun trackDoesNotSendEventWhenUserDoesNotSatisfyAudiences() {
val attribute = validProjectConfig.attributes[0]
val eventType = validProjectConfig.eventTypes[2]

 // the audience for the experiments is "NOT firefox" so this user shouldn't satisfy audience conditions
        val attributeMap = Collections.singletonMap(attribute.key, "firefox")

val client = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

logbackVerifier.expectMessage(Level.INFO, "There are no valid experiments for event \"" + eventType.key
+ "\" to track.")

client.track(eventType.key, genericUserId, attributeMap)
verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

 //======== getVariation tests ========//

    /**
 * Verify that [Optimizely.getVariation] correctly makes the
 * [Bucketer.bucket] call and does NOT dispatch an event.
 */
    @Test
@Throws(Exception::class)
 fun getVariation() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId)).thenReturn(bucketedVariation)

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")

 // activate the experiment
        val actualVariation = optimizely.getVariation(activatedExperiment.key, testUserId,
testUserAttributes)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // verify that we didn't attempt to dispatch an event
        verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that [Optimizely.getVariation] correctly makes the
 * [Bucketer.bucket] call and does NOT dispatch an event.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithExperimentKey() {
val activatedExperiment = noAudienceProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId)).thenReturn(bucketedVariation)

 // activate the experiment
        val actualVariation = optimizely.getVariation(activatedExperiment.key, testUserId)

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // verify that we didn't attempt to dispatch an event
        verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that [Optimizely.getVariation] returns null variation when null or empty
 * experimentKey is sent
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(Exception::class)
 fun getVariationWithNullExperimentKey() {
val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val nullExperimentKey:String? = null
 // activate the experiment
        val nullVariation = optimizely.getVariation(nullExperimentKey!!, testUserId)

assertNull(nullVariation)
logbackVerifier.expectMessage(Level.ERROR, "The experimentKey parameter must be nonnull.")

}

/**
 * Verify that [Optimizely.getVariation] handles the case where an unknown experiment
 * (i.e., not in the config) is passed through and a [NoOpErrorHandler] is used by default.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithUnknownExperimentKeyAndNoOpErrorHandler() {
val unknownExperiment = createUnknownExperiment()

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withErrorHandler(NoOpErrorHandler())
.build()

logbackVerifier.expectMessage(Level.ERROR, "Experiment \"unknown_experiment\" is not in the datafile")

 // since we use a NoOpErrorHandler, we should fail and return null
        val actualVariation = optimizely.getVariation(unknownExperiment.key, testUserId)

 // verify that null is returned, as no project config was available
        assertNull(actualVariation)
}

/**
 * Verify that [Optimizely.getVariation] returns a valid variation for a user who
 * falls into the experiment.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithAudiences() {
val experiment = validProjectConfig.experiments[0]
val bucketedVariation = experiment.variations[0]

`when`<Variation>(mockBucketer!!.bucket(experiment, testUserId)).thenReturn(bucketedVariation)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withBucketing(mockBucketer!!)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")

val actualVariation = optimizely.getVariation(experiment.key, testUserId, testUserAttributes)

verify<Bucketer>(mockBucketer!!).bucket(experiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))
}

/**
 * Verify that [Optimizely.getVariation] doesn't return a variation when
 * given an experiment with audiences but no attributes.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithAudiencesNoAttributes() {
val experiment:Experiment
if (datafileVersion >= 4)
{
experiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]!!
}
else
{
experiment = validProjectConfig.experiments[0]
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withErrorHandler(mockErrorHandler!!)
.build()

logbackVerifier.expectMessage(Level.INFO,
"User \"userId\" does not meet conditions to be in experiment \"" + experiment.key + "\".")

val actualVariation = optimizely.getVariation(experiment.key, testUserId)
assertNull(actualVariation)
}

/**
 * Verify that [Optimizely.getVariation] returns a variation when given an experiment
 * with no audiences and no user attributes.
 */
    @Test
@Throws(Exception::class)
 fun getVariationNoAudiences() {
val experiment = noAudienceProjectConfig.experiments[0]
val bucketedVariation = experiment.variations[0]

`when`<Variation>(mockBucketer!!.bucket(experiment, testUserId)).thenReturn(bucketedVariation)

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withBucketing(mockBucketer!!)
.withErrorHandler(mockErrorHandler!!)
.build()

val actualVariation = optimizely.getVariation(experiment.key, testUserId)

verify<Bucketer>(mockBucketer!!).bucket(experiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))
}

/**
 * Verify that [Optimizely.getVariation] handles the case where an unknown experiment
 * (i.e., not in the config) is passed through and a [RaiseExceptionErrorHandler] is provided.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithUnknownExperimentKeyAndRaiseExceptionErrorHandler() {
thrown.expect(UnknownExperimentException::class.java)

val unknownExperiment = createUnknownExperiment()

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

 // since we use a RaiseExceptionErrorHandler, we should throw an error
        optimizely.getVariation(unknownExperiment.key, testUserId)
}

/**
 * Verify that [Optimizely.getVariation] doesn't return a variation when provided an
 * empty string.
 */
    @Test
@Throws(Exception::class)
 fun getVariationWithEmptyUserId() {
val experiment = noAudienceProjectConfig.experiments[0]

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(RaiseExceptionErrorHandler())
.build()

logbackVerifier.expectMessage(Level.ERROR, "Non-empty user ID required")
assertNull(optimizely.getVariation(experiment.key, ""))
}

/**
 * Verify that [Optimizely.getVariation] returns a variation when given matching
 * user attributes.
 */
    @Test
@Throws(Exception::class)
 fun getVariationForGroupExperimentWithMatchingAttributes() {
val experiment = validProjectConfig.groups[0]
                .experiments[0]
val variation = experiment.variations[0]

val attributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
attributes.put("browser_type", "chrome")
}

`when`<Variation>(mockBucketer!!.bucket(experiment, "user")).thenReturn(variation)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withBucketing(mockBucketer!!)
.build()

assertThat<Variation>(optimizely.getVariation(experiment.key, "user", attributes),
`is`(variation))
}

/**
 * Verify that [Optimizely.getVariation] doesn't return a variation when given
 * non-matching user attributes.
 */
    @Test
@Throws(Exception::class)
 fun getVariationForGroupExperimentWithNonMatchingAttributes() {
val experiment = validProjectConfig.groups[0]
                .experiments[0]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

assertNull(optimizely.getVariation(experiment.key, "user",
Collections.singletonMap("browser_type", "firefox")))
}

/**
 * Verify that [Optimizely.getVariation] gives precedence to experiment status over forced
 * variation bucketing.
 */
    @Test
@Throws(Exception::class)
 fun getVariationExperimentStatusPrecedesForcedVariation() {
val experiment:Experiment
if (datafileVersion >= 4)
{
experiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_PAUSED_EXPERIMENT_KEY]!!
}
else
{
experiment = validProjectConfig.experiments[1]
}

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

logbackVerifier.expectMessage(Level.INFO, "Experiment \"" + experiment.key + "\" is not running.")
 // testUser3 has a corresponding forced variation, but experiment status should be checked first
        assertNull(optimizely.getVariation(experiment.key, "testUser3"))
}

 //======== Notification listeners ========//

    /**
 * Verify that the [&lt;][Optimizely.activate] call
 * correctly builds an endpoint url and request params
 * and passes them through [EventHandler.dispatchEvent].
 */
    @Test
@Throws(Exception::class)
 fun activateWithListener() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testUserAttributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
testUserAttributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
testUserAttributes.put("browser_type", "chrome")
}

testUserAttributes.put(testBucketingIdKey, testBucketingId)

val activateNotification = object:ActivateNotificationListener() {
override fun onActivate(experiment:Experiment, userId:String, attributes:Map<String, String>, variation:Variation, event:LogEvent) {
assertEquals(experiment.key, activatedExperiment.key)
assertEquals(bucketedVariation.key, variation.key)
assertEquals(userId, testUserId)
for ((key, value) in attributes)
{
assertEquals(testUserAttributes[key], value)
}

assertEquals(event.requestMethod, RequestMethod.GET)
}

}

val notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification)

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(validProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), eq<Map<String, String>>(testUserAttributes)))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testBucketingId))
.thenReturn(bucketedVariation)


 // activate the experiment
        val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, testUserAttributes)

assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId))
 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testBucketingId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

@Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun activateWithListenerNullAttributes() {
val activatedExperiment = noAudienceProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]

 // setup a mock event builder to return expected impression params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(noAudienceProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment), eq(bucketedVariation),
eq(testUserId), eq<Map<String, String>>(emptyMap<String, String>())))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, testUserId))
.thenReturn(bucketedVariation)

val activateNotification = object:ActivateNotificationListener() {
override fun onActivate(experiment:Experiment, userId:String, attributes:Map<String, String>, variation:Variation, event:LogEvent) {
assertEquals(experiment.key, activatedExperiment.key)
assertEquals(bucketedVariation.key, variation.key)
assertEquals(userId, testUserId)
assertTrue(attributes.isEmpty())

assertEquals(event.requestMethod, RequestMethod.GET)
}

}

val notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification)

 // activate the experiment
        val attributes:MutableMap<String, String>? = null
val actualVariation = optimizely.activate(activatedExperiment.key, testUserId, attributes!!)

optimizely.notificationCenter.removeNotificationListener(notificationId)

logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.")

 // verify that the bucketing algorithm was called correctly
        verify<Bucketer>(mockBucketer!!).bucket(activatedExperiment, testUserId)
assertThat<Variation>(actualVariation, `is`(bucketedVariation))

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)
verify(mockEventBuilder).createImpressionEvent(eq(noAudienceProjectConfig), eq(activatedExperiment),
eq(bucketedVariation), eq(testUserId), attributeCaptor.capture() as Map<String, String>)

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, `is`<Map<String, String>>(emptyMap<String, String>()))

 // verify that dispatchEvent was called with the correct LogEvent object
        verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Verify that [com.optimizely.ab.notification.NotificationCenter.addNotificationListener] properly used
 * and the listener is
 * added and notified when an experiment is activated.
 */
    @Test
@Throws(Exception::class)
 fun addNotificationListenerFromNotificationCenter() {
val activatedExperiment:Experiment
val eventType:EventType
if (datafileVersion >= 4)
{
activatedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_BASIC_EXPERIMENT_KEY]!!
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
activatedExperiment = validProjectConfig.experiments[0]
eventType = validProjectConfig.eventTypes[0]
}
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withDecisionService(mockDecisionService!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val attributes = emptyMap<String, String>()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
bucketedVariation, genericUserId, attributes))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockDecisionService!!.getVariation(
eq(activatedExperiment),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())))
.thenReturn(bucketedVariation)

 // Add listener
        val listener = mock<ActivateNotificationListener>(ActivateNotificationListener::class.java)
optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, listener)

 // Check if listener is notified when experiment is activated
        val actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes)
verify(listener, times(1))
.onActivate(activatedExperiment, genericUserId, attributes, bucketedVariation, logEventToDispatch)

assertEquals(actualVariation!!.key, bucketedVariation.key)
 // Check if listener is notified after an event is tracked
        val eventKey = eventType.key

val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
attributes)
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventKey),
eq<Map<String, String>>(attributes),
anyMapOf<String, Any>(String::class.java, Any::class.java)))
.thenReturn(logEventToDispatch)

val trackNotification = mock<TrackNotificationListener>(TrackNotificationListener::class.java)

optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification)

optimizely.track(eventKey, genericUserId, attributes)
verify(trackNotification, times(1))
.onTrack(eventKey, genericUserId, attributes, HashMap<String, Any>(), logEventToDispatch)
}

/**
 * Verify that [com.optimizely.ab.notification.NotificationCenter] properly
 * calls and the listener is removed and no longer notified when an experiment is activated.
 */
    @Test
@Throws(Exception::class)
 fun removeNotificationListenerNotificationCenter() {
val activatedExperiment = validProjectConfig.experiments[0]
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val attributes = HashMap<String, String>()
attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
bucketedVariation, genericUserId, attributes))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, genericUserId))
.thenReturn(bucketedVariation)

`when`(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation, genericUserId,
attributes))
.thenReturn(logEventToDispatch)

 // Add and remove listener
        val activateNotification = mock<ActivateNotificationListener>(ActivateNotificationListener::class.java)
var notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification)
assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId))

val trackNotification = mock<TrackNotificationListener>(TrackNotificationListener::class.java)
notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification)
assertTrue(optimizely.notificationCenter.removeNotificationListener(notificationId))

 // Check if listener is notified after an experiment is activated
        val actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes)
verify(activateNotification, never())
.onActivate(activatedExperiment, genericUserId, attributes, actualVariation!!, logEventToDispatch)

 // Check if listener is notified after a live variable is accessed
        val activateExperiment = true
verify(activateNotification, never())
.onActivate(activatedExperiment, genericUserId, attributes, actualVariation, logEventToDispatch)

 // Check if listener is notified after an event is tracked
        val eventType = validProjectConfig.eventTypes[0]
val eventKey = eventType.key

val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
attributes)
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventKey),
eq<Map<String, String>>(attributes),
anyMapOf<String, Any>(String::class.java, Any::class.java)))
.thenReturn(logEventToDispatch)

optimizely.track(eventKey, genericUserId, attributes)
verify(trackNotification, never())
.onTrack(eventKey, genericUserId, attributes, Collections.EMPTY_MAP as Map<String, *>, logEventToDispatch)
}

/**
 * Verify that [com.optimizely.ab.notification.NotificationCenter]
 * clearAllListerners removes all listeners
 * and no longer notified when an experiment is activated.
 */
    @Test
@Throws(Exception::class)
 fun clearNotificationListenersNotificationCenter() {
val activatedExperiment:Experiment
val attributes = HashMap<String, String>()
if (datafileVersion >= 4)
{
activatedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]!!
attributes.put(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
}
else
{
activatedExperiment = validProjectConfig.experiments[0]
attributes.put("browser_type", "chrome")
}
val bucketedVariation = activatedExperiment.variations[0]
val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createImpressionEvent(validProjectConfig, activatedExperiment,
bucketedVariation, genericUserId, attributes))
.thenReturn(logEventToDispatch)

`when`<Variation>(mockBucketer!!.bucket(activatedExperiment, genericUserId))
.thenReturn(bucketedVariation)

 // set up argument captor for the attributes map to compare map equality
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

`when`(mockEventBuilder.createImpressionEvent(
eq(validProjectConfig),
eq(activatedExperiment),
eq(bucketedVariation),
eq(genericUserId),
attributeCaptor.capture() as Map<String, String>
)).thenReturn(logEventToDispatch)

val activateNotification = mock<ActivateNotificationListener>(ActivateNotificationListener::class.java)
val trackNotification = mock<TrackNotificationListener>(TrackNotificationListener::class.java)

optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, activateNotification)
optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification)

optimizely.notificationCenter.clearAllNotificationListeners()

 // Check if listener is notified after an experiment is activated
        val actualVariation = optimizely.activate(activatedExperiment, genericUserId, attributes)

 // check that the argument that was captured by the mockEventBuilder attribute captor,
        // was equal to the attributes passed in to activate
        assertEquals(attributes, attributeCaptor.value)
verify(activateNotification, never())
.onActivate(activatedExperiment, genericUserId, attributes, actualVariation!!, logEventToDispatch)

 // Check if listener is notified after a live variable is accessed
        val activateExperiment = true
verify(activateNotification, never())
.onActivate(activatedExperiment, genericUserId, attributes, actualVariation, logEventToDispatch)

 // Check if listener is notified after a event is tracked
        val eventType = validProjectConfig.eventTypes[0]
val eventKey = eventType.key

val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
OptimizelyTest.genericUserId,
attributes)
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(OptimizelyTest.genericUserId),
eq(eventType.id),
eq(eventKey),
eq<Map<String, String>>(attributes),
anyMapOf<String, Any>(String::class.java, Any::class.java)))
.thenReturn(logEventToDispatch)

optimizely.track(eventKey, genericUserId, attributes)
verify(trackNotification, never())
.onTrack(eventKey, genericUserId, attributes, Collections.EMPTY_MAP as Map<String, *>, logEventToDispatch)
}

/**
 * Add notificaiton listener for track [com.optimizely.ab.notification.NotificationCenter].  Verify called and
 * remove.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun trackEventWithListenerAttributes() {
val attribute = validProjectConfig.attributes[0]
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val attributes = ImmutableMap.of(attribute.key, "attributeValue")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
attributes)
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
anyMapOf(String::class.java, String::class.java),
eq<Map<String, Any>>(emptyMap<String, Any>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

val trackNotification = object:TrackNotificationListener() {
override fun onTrack(eventKey:String, userId:String, _attributes:Map<String, String>, eventTags:Map<String, *>, event:LogEvent) {
assertEquals(eventType.key, eventKey)
assertEquals(genericUserId, userId)
assertEquals(attributes, _attributes)
assertTrue(eventTags.isEmpty())
}
}

val notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification)

 // call track
        optimizely.track(eventType.key, genericUserId, attributes)

optimizely.notificationCenter.removeNotificationListener(notificationId)

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
attributeCaptor.capture() as Map<String, String>,
eq<Map<String, Any>>(emptyMap<String, Any>()))

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, hasEntry(attribute.key, "attributeValue"))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

/**
 * Track with listener and verify that [Optimizely.track] ignores null attributes.
 */
    @Test
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "testing nullness contract violation")
@Throws(Exception::class)
 fun trackEventWithListenerNullAttributes() {
val eventType:EventType
if (datafileVersion >= 4)
{
eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
}
else
{
eventType = validProjectConfig.eventTypes[0]
}

 // setup a mock event builder to return expected conversion params
        val mockEventBuilder = mock<EventBuilder>(EventBuilder::class.java)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withEventBuilder(mockEventBuilder)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

val testParams = HashMap<String, String>()
testParams.put("test", "params")
val experimentVariationMap = createExperimentVariationMap(
validProjectConfig,
mockDecisionService!!,
eventType.key,
genericUserId,
emptyMap<String, String>())
val logEventToDispatch = LogEvent(RequestMethod.GET, "test_url", testParams, "")
`when`(mockEventBuilder.createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<Map<String, Any>>(emptyMap<String, Any>())))
.thenReturn(logEventToDispatch)

logbackVerifier.expectMessage(Level.INFO, "Tracking event \"" + eventType.key +
"\" for user \"" + genericUserId + "\".")
logbackVerifier.expectMessage(Level.DEBUG, "Dispatching conversion event to URL test_url with params " +
testParams + " and payload \"\"")

val trackNotification = object:TrackNotificationListener() {
override fun onTrack(eventKey:String, userId:String, attributes:Map<String, String>, eventTags:Map<String, *>, event:LogEvent) {
assertEquals(eventType.key, eventKey)
assertEquals(genericUserId, userId)
assertTrue(attributes.isEmpty())
assertTrue(eventTags.isEmpty())
}
}

val notificationId = optimizely.notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, trackNotification)

 // call track
        val attributes:MutableMap<String, String>? = null
optimizely.track(eventType.key, genericUserId, attributes!!)

optimizely.notificationCenter.removeNotificationListener(notificationId)

logbackVerifier.expectMessage(Level.WARN, "Attributes is null when non-null was expected. Defaulting to an empty attributes map.")

 // setup the attribute map captor (so we can verify its content)
        val attributeCaptor = ArgumentCaptor.forClass<Map<*, *>>(Map::class.java)

 // verify that the event builder was called with the expected attributes
        verify(mockEventBuilder).createConversionEvent(
eq(validProjectConfig),
eq(experimentVariationMap),
eq(genericUserId),
eq(eventType.id),
eq(eventType.key),
attributeCaptor.capture() as Map<String, String>,
eq<Map<String, Any>>(emptyMap<String, Any>()))

val actualValue = attributeCaptor.value
assertThat<Map<String, String>>(actualValue as Map<String, String>, `is`<Map<String, String>>(emptyMap<String, String>()))

verify<EventHandler>(mockEventHandler).dispatchEvent(logEventToDispatch)
}

 //======== Feature Accessor Tests ========//

    /**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns null and logs a message
 * when it is called with a feature key that has no corresponding feature in the datafile.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueForTypeReturnsNullWhenFeatureNotFound() {

val invalidFeatureKey = "nonexistent feature key"
val invalidVariableKey = "nonexistent variable key"
val attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build()

var value = optimizely.getFeatureVariableValueForType(
invalidFeatureKey,
invalidVariableKey,
genericUserId,
emptyMap<String, String>(),
LiveVariable.VariableType.STRING)
assertNull(value)

value = optimizely.getFeatureVariableString(invalidFeatureKey, invalidVariableKey, genericUserId, attributes)
assertNull(value)

logbackVerifier.expectMessage(Level.INFO,
"No feature flag was found for key \"$invalidFeatureKey\".",
times(2))

verify<DecisionService>(mockDecisionService!!, never()).getVariation(
any(Experiment::class.java),
anyString(),
anyMapOf(String::class.java, String::class.java))
}

/**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns null and logs a message
 * when the feature key is valid, but no variable could be found for the variable key in the feature.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueForTypeReturnsNullWhenVariableNotFoundInValidFeature() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY
val invalidVariableKey = "nonexistent variable key"

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build()

val value = optimizely.getFeatureVariableValueForType(
validFeatureKey,
invalidVariableKey,
genericUserId,
emptyMap<String, String>(),
LiveVariable.VariableType.STRING)
assertNull(value)

logbackVerifier.expectMessage(Level.INFO,
"No feature variable was found for key \"" + invalidVariableKey + "\" in feature flag \"" +
validFeatureKey + "\".")

verify<DecisionService>(mockDecisionService!!, never()).getVariation(
any(Experiment::class.java),
anyString(),
anyMapOf(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns null when the variable's type does not match the type with which it was attempted to be accessed.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueReturnsNullWhenVariableTypeDoesNotMatch() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY
val validVariableKey = VARIABLE_FIRST_LETTER_KEY

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build()

val value = optimizely.getFeatureVariableValueForType(
validFeatureKey,
validVariableKey,
genericUserId,
emptyMap<String, String>(),
LiveVariable.VariableType.INTEGER
)
assertNull(value)

logbackVerifier.expectMessage(
Level.INFO,
"The feature variable \"" + validVariableKey +
"\" is actually of type \"" + LiveVariable.VariableType.STRING.toString() +
"\" type. You tried to access it as type \"" + LiveVariable.VariableType.INTEGER.toString() +
"\". Please use the appropriate feature variable accessor."
)
}

/**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns the String default value of a live variable
 * when the feature is not attached to an experiment or a rollout.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueForTypeReturnsDefaultValueWhenFeatureIsNotAttachedToExperimentOrRollout() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY
val validVariableKey = VARIABLE_BOOLEAN_VARIABLE_KEY
val defaultValue = VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE
val attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

val value = optimizely.getFeatureVariableValueForType(
validFeatureKey,
validVariableKey,
genericUserId,
attributes,
LiveVariable.VariableType.BOOLEAN)
assertEquals(defaultValue, value)

logbackVerifier.expectMessage(
Level.INFO,
"The feature flag \"$validFeatureKey\" is not used in any experiments."
)
logbackVerifier.expectMessage(
Level.INFO,
"The feature flag \"$validFeatureKey\" is not used in a rollout."
)
logbackVerifier.expectMessage(
Level.INFO,
"User \"" + genericUserId + "\" was not bucketed into any variation for feature flag \"" +
validFeatureKey + "\". The default value \"" +
defaultValue + "\" for \"" +
validVariableKey + "\" is being returned."
)
}

/**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns the String default value for a live variable
 * when the feature is attached to an experiment and no rollout, but the user is excluded from the experiment.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueReturnsDefaultValueWhenFeatureIsAttachedToOneExperimentButFailsTargeting() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_SINGLE_VARIABLE_DOUBLE_KEY
val validVariableKey = VARIABLE_DOUBLE_VARIABLE_KEY
val expectedValue = VARIABLE_DOUBLE_DEFAULT_VALUE
val featureFlag = FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE
val experiment = validProjectConfig.experimentIdMapping[featureFlag.experimentIds[0]]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

val valueWithImproperAttributes = optimizely.getFeatureVariableValueForType(
validFeatureKey,
validVariableKey,
genericUserId,
Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, "Ravenclaw"),
LiveVariable.VariableType.DOUBLE
)
assertEquals(expectedValue, valueWithImproperAttributes)

logbackVerifier.expectMessage(
Level.INFO,
"User \"" + genericUserId + "\" does not meet conditions to be in experiment \"" +
experiment!!.key + "\"."
)
logbackVerifier.expectMessage(
Level.INFO,
"The feature flag \"$validFeatureKey\" is not used in a rollout."
)
logbackVerifier.expectMessage(
Level.INFO,
"User \"" + genericUserId +
"\" was not bucketed into any variation for feature flag \"" + validFeatureKey +
"\". The default value \"" + expectedValue +
"\" for \"" + validVariableKey + "\" is being returned."
)
}

/**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns the variable value of the variation the user is bucketed into
 * if the variation is not null and the variable has a usage within the variation.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueReturnsVariationValueWhenUserGetsBucketedToVariation() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY
val validVariableKey = VARIABLE_FIRST_LETTER_KEY
val variable = FEATURE_FLAG_MULTI_VARIATE_FEATURE.variableKeyToLiveVariableMap[validVariableKey]
val expectedValue = VARIATION_MULTIVARIATE_EXPERIMENT_GRED.variableIdToLiveVariableUsageInstanceMap[variable!!.id]!!.value
val multivariateExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build()

val featureDecision = FeatureDecision(multivariateExperiment, VARIATION_MULTIVARIATE_EXPERIMENT_GRED, FeatureDecision.DecisionSource.EXPERIMENT)
doReturn(featureDecision).`when`<DecisionService>(mockDecisionService!!).getVariationForFeature(
FEATURE_FLAG_MULTI_VARIATE_FEATURE,
genericUserId,
Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
)

val value = optimizely.getFeatureVariableValueForType(
validFeatureKey,
validVariableKey,
genericUserId,
Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE),
LiveVariable.VariableType.STRING
)

assertEquals(expectedValue, value)
}

/**
 * Verify [Optimizely.getFeatureVariableValueForType]
 * returns the default value for the feature variable
 * when there is no variable usage present for the variation the user is bucketed into.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableValueReturnsDefaultValueWhenNoVariationUsageIsPresent() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_SINGLE_VARIABLE_INTEGER_KEY
val validVariableKey = VARIABLE_INTEGER_VARIABLE_KEY
val variable = FEATURE_FLAG_SINGLE_VARIABLE_INTEGER.variableKeyToLiveVariableMap[validVariableKey]
val expectedValue = variable!!.defaultValue

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

val value = optimizely.getFeatureVariableValueForType(
validFeatureKey,
validVariableKey,
genericUserId,
emptyMap<String, String>(),
LiveVariable.VariableType.INTEGER
)

assertEquals(expectedValue, value)
}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] and they both
 * return False
 * when the APIs are called with a null value for the feature key parameter.
 * @throws Exception
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsFalseWhenFeatureKeyIsNull() {
val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())

//assertFalse(spyOptimizely.isFeatureEnabled(null, genericUserId))

logbackVerifier.expectMessage(
Level.WARN,
"The featureKey parameter must be nonnull."
)

verify(spyOptimizely, times(1)).isFeatureEnabled(
isNull<String>(String::class.java),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<DecisionService>(mockDecisionService!!, never()).getVariationForFeature(
any<FeatureFlag>(FeatureFlag::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] and they both
 * return False
 * when the APIs are called with a null value for the user ID parameter.
 * @throws Exception
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsFalseWhenUserIdIsNull() {
val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())

val featureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

//assertFalse(spyOptimizely.isFeatureEnabled(featureKey, null))

logbackVerifier.expectMessage(
Level.WARN,
"The userId parameter must be nonnull."
)

verify(spyOptimizely, times(1)).isFeatureEnabled(
eq(featureKey),
isNull<String>(String::class.java),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<DecisionService>(mockDecisionService!!, never()).getVariationForFeature(
any<FeatureFlag>(FeatureFlag::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] and they both
 * return False
 * when the APIs are called with a feature key that is not in the datafile.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsFalseWhenFeatureFlagKeyIsInvalid() {

val invalidFeatureKey = "nonexistent feature key"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())

assertFalse(spyOptimizely.isFeatureEnabled(invalidFeatureKey, genericUserId))

logbackVerifier.expectMessage(
Level.INFO,
"No feature flag was found for key \"$invalidFeatureKey\"."
)
verify(spyOptimizely, times(1)).isFeatureEnabled(
eq(invalidFeatureKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<DecisionService>(mockDecisionService!!, never()).getVariation(
any(Experiment::class.java),
anyString(),
anyMapOf(String::class.java, String::class.java))
verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] and they both
 * return False
 * when the user is not bucketed into any variation for the feature.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsFalseWhenUserIsNotBucketedIntoAnyVariation() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())

val featureDecision = FeatureDecision(null, null, null)
doReturn(featureDecision).`when`<DecisionService>(mockDecisionService!!).getVariationForFeature(
any(FeatureFlag::class.java),
anyString(),
anyMapOf(String::class.java, String::class.java)
)

assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId))

logbackVerifier.expectMessage(
Level.INFO,
"Feature \"" + validFeatureKey +
"\" is not enabled for user \"" + genericUserId + "\"."
)
verify(spyOptimizely).isFeatureEnabled(
eq(validFeatureKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<DecisionService>(mockDecisionService!!).getVariationForFeature(
eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] and they both
 * return True when the user is bucketed into a variation for the feature.
 * An impression event should not be dispatched since the user was not bucketed into an Experiment.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsTrueButDoesNotSendWhenUserIsBucketedIntoVariationWithoutExperiment() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())

 // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        val experiment = validProjectConfig.rolloutIdMapping[ROLLOUT_2_ID]!!.experiments[0]
val variation = Variation("variationId", "variationKey", true, null)
val featureDecision = FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT)
doReturn(featureDecision).`when`<DecisionService>(mockDecisionService!!).getVariationForFeature(
eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertTrue(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId))

logbackVerifier.expectMessage(
Level.INFO,
"The user \"" + genericUserId +
"\" is not included in an experiment for feature \"" + validFeatureKey + "\"."
)
logbackVerifier.expectMessage(
Level.INFO,
"Feature \"" + validFeatureKey +
"\" is enabled for user \"" + genericUserId + "\"."
)
verify(spyOptimizely).isFeatureEnabled(
eq(validFeatureKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<DecisionService>(mockDecisionService!!).getVariationForFeature(
eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
verify<EventHandler>(mockEventHandler, never()).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify that the [&lt;][Optimizely.activate] call
 * uses forced variation to force the user into the third variation in which FeatureEnabled is set to
 * false so feature enabled will return false
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledWithExperimentKeyForcedOffFeatureEnabledFalse() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
val activatedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]
val forcedVariation = activatedExperiment!!.variations[2]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

optimizely.setForcedVariation(activatedExperiment.key, testUserId, forcedVariation.key)
assertFalse(optimizely.isFeatureEnabled(FEATURE_FLAG_MULTI_VARIATE_FEATURE.key, testUserId))
}

/**
 * Verify that the [&lt;][Optimizely.activate] call
 * uses forced variation to force the user into the second variation in which FeatureEnabled is not set
 * feature enabled will return false by default
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledWithExperimentKeyForcedWithNoFeatureEnabledSet() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
val activatedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY]
val forcedVariation = activatedExperiment!!.variations[1]

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withBucketing(mockBucketer!!)
.withConfig(validProjectConfig)
.withErrorHandler(mockErrorHandler!!)
.build()

optimizely.setForcedVariation(activatedExperiment.key, testUserId, forcedVariation.key)
assertFalse(optimizely.isFeatureEnabled(FEATURE_SINGLE_VARIABLE_DOUBLE_KEY, testUserId))
}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] sending FeatureEnabled true and they both
 * return True when the user is bucketed into a variation for the feature.
 * An impression event should not be dispatched since the user was not bucketed into an Experiment.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledTrueWhenFeatureEnabledOfVariationIsTrue() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())
 // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        val experiment = validProjectConfig.rolloutIdMapping[ROLLOUT_2_ID]!!.experiments[0]
val variation = Variation("variationId", "variationKey", true, null)
val featureDecision = FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT)
doReturn(featureDecision).`when`<DecisionService>(mockDecisionService!!).getVariationForFeature(
eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertTrue(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId))

}


/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] sending FeatureEnabled false because of which and they both
 * return false even when the user is bucketed into a variation for the feature.
 * An impression event should not be dispatched since the user was not bucketed into an Experiment.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledFalseWhenFeatureEnabledOfVariationIsFalse() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())
 // Should be an experiment from the rollout associated with the feature, but for this test
        // it doesn't matter. Just use any valid experiment.
        val experiment = validProjectConfig.rolloutIdMapping[ROLLOUT_2_ID]!!.experiments[0]
val variation = Variation("variationId", "variationKey", false, null)
val featureDecision = FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.ROLLOUT)
doReturn(featureDecision).`when`<DecisionService>(mockDecisionService!!).getVariationForFeature(
eq(FEATURE_FLAG_MULTI_VARIATE_FEATURE),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId))

}

/**
 * Verify [Optimizely.isFeatureEnabled] calls into
 * [Optimizely.isFeatureEnabled] and they both
 * return False
 * when the user is bucketed an feature test variation that is turned off.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsFalseAndDispatchesWhenUserIsBucketedIntoAnExperimentVariationToggleOff() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.withDecisionService(mockDecisionService!!)
.build())

val activatedExperiment = validProjectConfig.experimentKeyMapping[EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY]
val variation = Variation("2", "variation_toggled_off", false, null)

val featureDecision = FeatureDecision(activatedExperiment, variation, FeatureDecision.DecisionSource.EXPERIMENT)
doReturn(featureDecision).`when`<DecisionService>(mockDecisionService!!).getVariationForFeature(
any(FeatureFlag::class.java),
anyString(),
anyMapOf(String::class.java, String::class.java)
)

assertFalse(spyOptimizely.isFeatureEnabled(validFeatureKey, genericUserId))

logbackVerifier.expectMessage(
Level.INFO,
"Feature \"" + validFeatureKey +
"\" is not enabled for user \"" + genericUserId + "\"."
)
verify<EventHandler>(mockEventHandler, times(1)).dispatchEvent(any(LogEvent::class.java))
}

/** Integration Test
 * Verify [Optimizely.isFeatureEnabled]
 * returns True
 * when the user is bucketed into a variation for the feature.
 * The user is also bucketed into an experiment, so we verify that an event is dispatched.
 * @throws Exception
 */
    @Test
@Throws(Exception::class)
 fun isFeatureEnabledReturnsTrueAndDispatchesEventWhenUserIsBucketedIntoAnExperiment() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val validFeatureKey = FEATURE_MULTI_VARIATE_FEATURE_KEY

val optimizely = Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build()

assertTrue(optimizely.isFeatureEnabled(
validFeatureKey,
genericUserId,
Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE)
))

logbackVerifier.expectMessage(
Level.INFO,
"Feature \"" + validFeatureKey +
"\" is enabled for user \"" + genericUserId + "\"."
)
verify<EventHandler>(mockEventHandler, times(1)).dispatchEvent(any(LogEvent::class.java))
}

/**
 * Verify [Optimizely.getEnabledFeatures] calls into
 * [Optimizely.isFeatureEnabled] for each featureFlag
 * return List of FeatureFlags that are enabled
 */
    @Test
@Throws(ConfigParseException::class)
 fun getEnabledFeatureWithValidUserId() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())
val featureFlags = spyOptimizely.getEnabledFeatures(genericUserId,
HashMap()) as ArrayList<String>
assertFalse(featureFlags.isEmpty())

}

/**
 * Verify [Optimizely.getEnabledFeatures] calls into
 * [Optimizely.isFeatureEnabled] for each featureFlag sending
 * userId as empty string
 * return empty List of FeatureFlags without checking further.
 */
    @Test
@Throws(ConfigParseException::class)
 fun getEnabledFeatureWithEmptyUserId() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())
val featureFlags = spyOptimizely.getEnabledFeatures("",
HashMap()) as ArrayList<String>
logbackVerifier.expectMessage(Level.ERROR, "Non-empty user ID required")
assertTrue(featureFlags.isEmpty())

}

/**
 * Verify [Optimizely.getEnabledFeatures] calls into
 * [Optimizely.isFeatureEnabled] for each featureFlag sending
 * userId as null
 * Exception of IllegalArgumentException will be thrown
 * return
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getEnabledFeatureWithNullUserID() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
val userID:String? = null
val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())
val featureFlags = spyOptimizely.getEnabledFeatures(userID!!,
HashMap()) as ArrayList<String>
assertTrue(featureFlags.isEmpty())

logbackVerifier.expectMessage(
Level.ERROR,
"The user ID parameter must be nonnull."
)
}

/**
 * Verify [Optimizely.getEnabledFeatures] calls into
 * [Optimizely.isFeatureEnabled] for each featureFlag sending
 * userId and emptyMap and Mocked [Optimizely.isFeatureEnabled]
 * to return false so [Optimizely.getEnabledFeatures] will
 * return empty List of FeatureFlags.
 */
    @Test
@Throws(ConfigParseException::class)
 fun getEnabledFeatureWithMockIsFeatureEnabledToReturnFalse() {
assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())
doReturn(false).`when`(spyOptimizely).isFeatureEnabled(
any(String::class.java),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
val featureFlags = spyOptimizely.getEnabledFeatures(genericUserId,
HashMap<String, String>()) as ArrayList<String>
assertTrue(featureFlags.isEmpty())
}

/**
 * Verify [Optimizely.getFeatureVariableString]
 * calls through to [Optimizely.getFeatureVariableString]
 * and returns null
 * when called with a null value for the feature Key parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableStringReturnsNullWhenFeatureKeyIsNull() {
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

//assertNull(spyOptimizely.getFeatureVariableString(
//null,
//variableKey,
//genericUserId
//))

logbackVerifier.expectMessage(
Level.WARN,
"The featureKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableString(
isNull<String>(String::class.java),
any<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableString]
 * calls through to [Optimizely.getFeatureVariableString]
 * and returns null
 * when called with a null value for the variableKey parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableStringReturnsNullWhenVariableKeyIsNull() {
val featureKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableString(
featureKey,
"",
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The variableKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableString(
any<String>(String::class.java),
isNull<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableString]
 * calls through to [Optimizely.getFeatureVariableString]
 * and returns null
 * when called with a null value for the userID parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableStringReturnsNullWhenUserIdIsNull() {
val featureKey = ""
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableString(
featureKey,
variableKey,
""
))

logbackVerifier.expectMessage(
Level.WARN,
"The userId parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableString(
any<String>(String::class.java),
any<String>(String::class.java),
isNull<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}
/**
 * Verify [Optimizely.getFeatureVariableString]
 * calls through to [&lt;][Optimizely.getFeatureVariableString]
 * and returns null
 * when [Optimizely.getFeatureVariableValueForType]
 * returns null
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableStringReturnsNullFromInternal() {
val featureKey = "featureKey"
val variableKey = "variableKey"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

doReturn(null).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.STRING)
)

assertNull(spyOptimizely.getFeatureVariableString(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableString(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
}

/**
 * Verify [Optimizely.getFeatureVariableString]
 * calls through to [Optimizely.getFeatureVariableString]
 * and both return the value returned from
 * [Optimizely.getFeatureVariableValueForType].
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableStringReturnsWhatInternalReturns() {
val featureKey = "featureKey"
val variableKey = "variableKey"
val valueNoAttributes = "valueNoAttributes"
val valueWithAttributes = "valueWithAttributes"
val attributes = Collections.singletonMap("key", "value")

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())


doReturn(valueNoAttributes).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.STRING)
)

doReturn(valueWithAttributes).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq(attributes),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.STRING)
)

assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableString(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableString(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableString(
featureKey,
variableKey,
genericUserId,
attributes
))
}

/**
 * Verify [Optimizely.getFeatureVariableBoolean]
 * calls through to [Optimizely.getFeatureVariableBoolean]
 * and returns null
 * when called with a null value for the feature Key parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableBooleanReturnsNullWhenFeatureKeyIsNull() {
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableBoolean(
"",
variableKey,
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The featureKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableBoolean(
isNull<String>(String::class.java),
any<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableBoolean]
 * calls through to [Optimizely.getFeatureVariableBoolean]
 * and returns null
 * when called with a null value for the variableKey parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableBooleanReturnsNullWhenVariableKeyIsNull() {
val featureKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableBoolean(
featureKey,
"",
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The variableKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableBoolean(
any<String>(String::class.java),
isNull<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableBoolean]
 * calls through to [Optimizely.getFeatureVariableBoolean]
 * and returns null
 * when called with a null value for the userID parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableBooleanReturnsNullWhenUserIdIsNull() {
val featureKey = ""
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableBoolean(
featureKey,
variableKey,
null!!
))

logbackVerifier.expectMessage(
Level.WARN,
"The userId parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableBoolean(
any<String>(String::class.java),
any<String>(String::class.java),
isNull<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}


/**
 * Verify [Optimizely.getFeatureVariableBoolean]
 * calls through to [&lt;][Optimizely.getFeatureVariableBoolean]
 * and returns null
 * when [Optimizely.getFeatureVariableValueForType]
 * returns null
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableBooleanReturnsNullFromInternal() {
val featureKey = "featureKey"
val variableKey = "variableKey"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

doReturn(null).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.BOOLEAN)
)

assertNull(spyOptimizely.getFeatureVariableBoolean(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableBoolean(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
}

/**
 * Verify [Optimizely.getFeatureVariableBoolean]
 * calls through to [Optimizely.getFeatureVariableBoolean]
 * and both return a Boolean representation of the value returned from
 * [Optimizely.getFeatureVariableValueForType].
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableBooleanReturnsWhatInternalReturns() {
val featureKey = "featureKey"
val variableKey = "variableKey"
val valueNoAttributes = false
val valueWithAttributes = true
val attributes = Collections.singletonMap("key", "value")

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())


doReturn(valueNoAttributes.toString()).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.BOOLEAN)
)

doReturn(valueWithAttributes.toString()).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq(attributes),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.BOOLEAN)
)

assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableBoolean(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableBoolean(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableBoolean(
featureKey,
variableKey,
genericUserId,
attributes
))
}

/**
 * Verify [Optimizely.getFeatureVariableDouble]
 * calls through to [&lt;][Optimizely.getFeatureVariableDouble]
 * and returns null
 * when [Optimizely.getFeatureVariableValueForType]
 * returns null
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableDoubleReturnsNullFromInternal() {
val featureKey = "featureKey"
val variableKey = "variableKey"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

doReturn(null).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.DOUBLE)
)

assertNull(spyOptimizely.getFeatureVariableDouble(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableDouble(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
}

/**
 * Verify [Optimizely.getFeatureVariableDouble]
 * calls through to [Optimizely.getFeatureVariableDouble]
 * and both return the parsed Double from the value returned from
 * [Optimizely.getFeatureVariableValueForType].
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableDoubleReturnsWhatInternalReturns() {
val featureKey = "featureKey"
val variableKey = "variableKey"
val valueNoAttributes = 0.1
val valueWithAttributes = 0.2
val attributes = Collections.singletonMap("key", "value")

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())


doReturn(valueNoAttributes.toString()).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.DOUBLE)
)

doReturn(valueWithAttributes.toString()).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq(attributes),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.DOUBLE)
)

assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableDouble(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableDouble(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableDouble(
featureKey,
variableKey,
genericUserId,
attributes
))
}

/**
 * Verify [Optimizely.getFeatureVariableInteger]
 * calls through to [&lt;][Optimizely.getFeatureVariableInteger]
 * and returns null
 * when [Optimizely.getFeatureVariableValueForType]
 * returns null
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableIntegerReturnsNullFromInternal() {
val featureKey = "featureKey"
val variableKey = "variableKey"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

doReturn(null).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.INTEGER)
)

assertNull(spyOptimizely.getFeatureVariableInteger(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableInteger(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)
}

/**
 * Verify [Optimizely.getFeatureVariableDouble]
 * calls through to [Optimizely.getFeatureVariableDouble]
 * and returns null
 * when called with a null value for the feature Key parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableDoubleReturnsNullWhenFeatureKeyIsNull() {
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableDouble(
"",
variableKey,
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The featureKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableDouble(
isNull<String>(String::class.java),
any<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableDouble]
 * calls through to [Optimizely.getFeatureVariableDouble]
 * and returns null
 * when called with a null value for the variableKey parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableDoubleReturnsNullWhenVariableKeyIsNull() {
val featureKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableDouble(
featureKey,
"",
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The variableKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableDouble(
any<String>(String::class.java),
isNull<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableDouble]
 * calls through to [Optimizely.getFeatureVariableDouble]
 * and returns null
 * when called with a null value for the userID parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableDoubleReturnsNullWhenUserIdIsNull() {
val featureKey = ""
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableDouble(
featureKey,
variableKey,
""
))

logbackVerifier.expectMessage(
Level.WARN,
"The userId parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableDouble(
any<String>(String::class.java),
any<String>(String::class.java),
isNull<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify that [Optimizely.getFeatureVariableDouble]
 * and [Optimizely.getFeatureVariableDouble]
 * do not throw errors when they are unable to parse the value into an Double.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableDoubleCatchesExceptionFromParsing() {
val featureKey = "featureKey"
val variableKey = "variableKey"
val unParsableValue = "not_a_double"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

doReturn(unParsableValue).`when`(spyOptimizely).getFeatureVariableValueForType(
anyString(),
anyString(),
anyString(),
anyMapOf(String::class.java, String::class.java),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.DOUBLE)
)

assertNull(spyOptimizely.getFeatureVariableDouble(
featureKey,
variableKey,
genericUserId
))

logbackVerifier.expectMessage(
Level.ERROR,
"NumberFormatException while trying to parse \"" + unParsableValue +
"\" as Double. "
)
}
/**
 * Verify [Optimizely.getFeatureVariableInteger]
 * calls through to [Optimizely.getFeatureVariableInteger]
 * and returns null
 * when called with a null value for the feature Key parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableIntegerReturnsNullWhenFeatureKeyIsNull() {
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableInteger(
"",
variableKey,
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The featureKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableInteger(
isNull<String>(String::class.java),
any<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableInteger]
 * calls through to [Optimizely.getFeatureVariableInteger]
 * and returns null
 * when called with a null value for the variableKey parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableIntegerReturnsNullWhenVariableKeyIsNull() {
val featureKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableInteger(
featureKey,
"",
genericUserId
))

logbackVerifier.expectMessage(
Level.WARN,
"The variableKey parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableInteger(
any<String>(String::class.java),
isNull<String>(String::class.java),
any<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableInteger]
 * calls through to [Optimizely.getFeatureVariableInteger]
 * and returns null
 * when called with a null value for the userID parameter.
 * @throws ConfigParseException
 */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
@Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableIntegerReturnsNullWhenUserIdIsNull() {
val featureKey = ""
val variableKey = ""

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

assertNull(spyOptimizely.getFeatureVariableInteger(
featureKey,
variableKey, ""
))

logbackVerifier.expectMessage(
Level.WARN,
"The userId parameter must be nonnull."
)
verify(spyOptimizely, times(1)).getFeatureVariableInteger(
any<String>(String::class.java),
any<String>(String::class.java),
isNull<String>(String::class.java),
anyMapOf<String, String>(String::class.java, String::class.java)
)
}

/**
 * Verify [Optimizely.getFeatureVariableInteger]
 * calls through to [Optimizely.getFeatureVariableInteger]
 * and both return the parsed Integer value from the value returned from
 * [Optimizely.getFeatureVariableValueForType].
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableIntegerReturnsWhatInternalReturns() {
val featureKey = "featureKey"
val variableKey = "variableKey"
val valueNoAttributes = 1
val valueWithAttributes = 2
val attributes = Collections.singletonMap("key", "value")

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())


doReturn(valueNoAttributes.toString()).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>()),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.INTEGER)
)

doReturn(valueWithAttributes.toString()).`when`(spyOptimizely).getFeatureVariableValueForType(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq(attributes),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.INTEGER)
)

assertEquals(valueNoAttributes, spyOptimizely.getFeatureVariableInteger(
featureKey,
variableKey,
genericUserId
))

verify(spyOptimizely).getFeatureVariableInteger(
eq(featureKey),
eq(variableKey),
eq(genericUserId),
eq<Map<String, String>>(emptyMap<String, String>())
)

assertEquals(valueWithAttributes, spyOptimizely.getFeatureVariableInteger(
featureKey,
variableKey,
genericUserId,
attributes
))
}

/**
 * Verify that [Optimizely.getFeatureVariableInteger]
 * and [Optimizely.getFeatureVariableInteger]
 * do not throw errors when they are unable to parse the value into an Integer.
 * @throws ConfigParseException
 */
    @Test
@Throws(ConfigParseException::class)
 fun getFeatureVariableIntegerCatchesExceptionFromParsing() {
val featureKey = "featureKey"
val variableKey = "variableKey"
val unParsableValue = "not_an_integer"

val spyOptimizely = spy(Optimizely.builder(validDatafile, mockEventHandler!!)
.withConfig(validProjectConfig)
.build())

doReturn(unParsableValue).`when`(spyOptimizely).getFeatureVariableValueForType(
anyString(),
anyString(),
anyString(),
anyMapOf(String::class.java, String::class.java),
eq<LiveVariable.VariableType>(LiveVariable.VariableType.INTEGER)
)

assertNull(spyOptimizely.getFeatureVariableInteger(
featureKey,
variableKey,
genericUserId
))

logbackVerifier.expectMessage(
Level.ERROR,
"NumberFormatException while trying to parse \"" + unParsableValue +
"\" as Integer. "
)
}

/**
 * Verify that [Optimizely.getVariation] returns a variation when given an experiment
 * with no audiences and no user attributes.
 */
    @Test
@Throws(Exception::class)
 fun getVariationBucketingIdAttribute() {
val experiment = noAudienceProjectConfig.experiments[0]
val bucketedVariation = experiment.variations[0]
val bucketingKey = testBucketingIdKey
val bucketingId = "blah"
val userId = testUserId
val testUserAttributes = HashMap<String, String>()
testUserAttributes.put("browser_type", "chrome")
testUserAttributes.put(bucketingKey, bucketingId)


`when`<Variation>(mockBucketer!!.bucket(experiment, bucketingId)).thenReturn(bucketedVariation)

val optimizely = Optimizely.builder(noAudienceDatafile, mockEventHandler!!)
.withConfig(noAudienceProjectConfig)
.withBucketing(mockBucketer!!)
.withErrorHandler(mockErrorHandler!!)
.build()

val actualVariation = optimizely.getVariation(experiment.key, userId, testUserAttributes)

verify<Bucketer>(mockBucketer!!).bucket(experiment, bucketingId)

assertThat<Variation>(actualVariation, `is`(bucketedVariation))
}

 //======== Helper methods ========//

    private fun createUnknownExperiment():Experiment {
return Experiment("0987", "unknown_experiment", "Running", "1",
emptyList<String>(),
listOf<Variation>(Variation("8765", "unknown_variation", emptyList<LiveVariableUsageInstance>())),
emptyMap<String, String>(),
listOf<TrafficAllocation>(TrafficAllocation("8765", 4999)))
}

private fun createUnknownEventType():EventType {
val experimentIds = asList(
"223"
)
return EventType("8765", "unknown_event_type", experimentIds)
}

companion object {

@Parameters
@Throws(IOException::class)
 fun data():Collection<Array<Any>> {
return Arrays.asList(*arrayOf(arrayOf(2, validConfigJsonV2(), noAudienceProjectConfigJsonV2(), validProjectConfigV2(), noAudienceProjectConfigV2()), arrayOf(3, validConfigJsonV3(), noAudienceProjectConfigJsonV3(), validProjectConfigV3(), noAudienceProjectConfigV3()), arrayOf(4, validConfigJsonV4(), validConfigJsonV4(), validProjectConfigV4(), validProjectConfigV4())))
}

private val genericUserId = "genericUserId"
private val testUserId = "userId"
private val testBucketingId = "bucketingId"
private val testBucketingIdKey = ControlAttribute.BUCKETING_ATTRIBUTE.toString()
}
}
