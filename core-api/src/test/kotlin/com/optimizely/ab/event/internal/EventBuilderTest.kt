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
package com.optimizely.ab.event.internal

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.optimizely.ab.bucketing.Bucketer
import com.optimizely.ab.bucketing.DecisionService
import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.config.Attribute
import com.optimizely.ab.config.EventType
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Variation
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.error.NoOpErrorHandler
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.event.internal.payload.Decision
import com.optimizely.ab.event.internal.payload.EventBatch
import com.optimizely.ab.internal.ControlAttribute
import com.optimizely.ab.internal.ReservedEventKey
import com.optimizely.ab.event.ClientEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4
import com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE
import com.optimizely.ab.config.ValidProjectConfigV4.EVENT_BASIC_EVENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.EVENT_PAUSED_EXPERIMENT_KEY
import com.optimizely.ab.config.ValidProjectConfigV4.MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED
import com.optimizely.ab.config.ValidProjectConfigV4.PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL
import junit.framework.Assert.assertNotNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(Parameterized::class)
class EventBuilderTest(private val datafileVersion: Int,
                       private val validProjectConfig: ProjectConfig) {

    private val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    private val builder = EventBuilder()

    /**
     * Verify [com.optimizely.ab.event.internal.payload.EventBatch] event creation
     */
    @Test
    @Throws(Exception::class)
    fun createImpressionEventPassingUserAgentAttribute() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val activatedExperiment = validProjectConfig.experiments[0]
        val bucketedVariation = activatedExperiment.variations[0]
        val attribute = validProjectConfig.attributes[0]
        val userId = "userId"
        val attributeMap = HashMap<String, String>()
        attributeMap.put(attribute.key, "value")
        attributeMap.put(ControlAttribute.USER_AGENT_ATTRIBUTE.toString(), "Chrome")
        val expectedDecision = Decision(activatedExperiment.layerId!!, activatedExperiment.id, bucketedVariation.id, false)
        val feature = com.optimizely.ab.event.internal.payload.Attribute(attribute.id,
                attribute.key, com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "value")
        val userAgentFeature = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.USER_AGENT_ATTRIBUTE.toString(),
                ControlAttribute.USER_AGENT_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "Chrome")

        val botFilteringFeature = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                validProjectConfig.botFiltering!!)
        val expectedUserFeatures: List<com.optimizely.ab.event.internal.payload.Attribute>

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(userAgentFeature, feature, botFilteringFeature)
        else
            expectedUserFeatures = Arrays.asList(userAgentFeature, feature)

        val impressionEvent = builder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap)

        // verify that request endpoint is correct
        assertThat(impressionEvent.endpointUrl, `is`(EventBuilder.EVENT_ENDPOINT))

        val eventBatch = gson.fromJson<EventBatch>(impressionEvent.body, EventBatch::class.java!!)

        // verify payload information
        assertThat(eventBatch.visitors[0].visitorId, `is`(userId))
        assertThat(eventBatch.visitors[0].snapshots[0].events[0].timestamp.toDouble(), closeTo(System.currentTimeMillis().toDouble(), 1000.0))
        assertFalse(eventBatch.visitors[0].snapshots[0].decisions[0].isCampaignHoldback)
        assertThat(eventBatch.anonymizeIp, `is`(validProjectConfig.anonymizeIP))
        assertThat(eventBatch.projectId, `is`(validProjectConfig.projectId))
        assertThat(eventBatch.visitors[0].snapshots[0].decisions[0], `is`(expectedDecision))
        assertThat(eventBatch.visitors[0].snapshots[0].decisions[0].campaignId,
                `is`(activatedExperiment.layerId))
        assertThat(eventBatch.accountId, `is`(validProjectConfig.accountId))
        //assertThat<List<Attribute>>(eventBatch.visitors[0].attributes!, `is`<List<Attribute>>(expectedUserFeatures))
        assertThat(eventBatch.clientName, `is`(ClientEngine.JAVA_SDK.clientEngineValue))
        assertThat(eventBatch.clientVersion, `is`(BuildVersionInfo.VERSION))
        assertNull(eventBatch.visitors[0].sessionId)
    }

    /**
     * Verify [com.optimizely.ab.event.internal.payload.EventBatch] event creation
     */
    @Test
    @Throws(Exception::class)
    fun createImpressionEvent() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val activatedExperiment = validProjectConfig.experiments[0]
        val bucketedVariation = activatedExperiment.variations[0]
        val attribute = validProjectConfig.attributes[0]
        val userId = "userId"
        val attributeMap = Collections.singletonMap(attribute.key, "value")
        val expectedDecision = Decision(activatedExperiment.layerId!!, activatedExperiment.id, bucketedVariation.id, false)
        val feature = com.optimizely.ab.event.internal.payload.Attribute(attribute.id,
                attribute.key, com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "value")
        val botFilteringFeature = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                validProjectConfig.botFiltering!!)
        val expectedUserFeatures: List<com.optimizely.ab.event.internal.payload.Attribute>

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(feature, botFilteringFeature)
        else
            expectedUserFeatures = Arrays.asList(feature)

        val impressionEvent = builder.createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap)

        // verify that request endpoint is correct
        assertThat(impressionEvent.endpointUrl, `is`(EventBuilder.EVENT_ENDPOINT))

        val eventBatch = gson.fromJson<EventBatch>(impressionEvent.body, EventBatch::class.java!!)

        // verify payload information
        assertThat(eventBatch.visitors[0].visitorId, `is`(userId))
        assertThat(eventBatch.visitors[0].snapshots[0].events[0].timestamp.toDouble(), closeTo(System.currentTimeMillis().toDouble(), 1000.0))
        assertFalse(eventBatch.visitors[0].snapshots[0].decisions[0].isCampaignHoldback)
        assertThat(eventBatch.anonymizeIp, `is`(validProjectConfig.anonymizeIP))
        assertThat(eventBatch.projectId, `is`(validProjectConfig.projectId))
        assertThat(eventBatch.visitors[0].snapshots[0].decisions[0], `is`(expectedDecision))
        assertThat(eventBatch.visitors[0].snapshots[0].decisions[0].campaignId,
                `is`(activatedExperiment.layerId))
        assertThat(eventBatch.accountId, `is`(validProjectConfig.accountId))
        //assertThat<List<Attribute>>(eventBatch.visitors[0].attributes!!, `is`<List<Attribute>>(expectedUserFeatures))
        assertThat(eventBatch.clientName, `is`(ClientEngine.JAVA_SDK.clientEngineValue))
        assertThat(eventBatch.clientVersion, `is`(BuildVersionInfo.VERSION))
        assertNull(eventBatch.visitors[0].sessionId)
    }

    /**
     * Verify that passing through an unknown attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    @Throws(Exception::class)
    fun createImpressionEventIgnoresUnknownAttributes() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val projectConfig = validProjectConfig
        val activatedExperiment = projectConfig.experiments[0]
        val bucketedVariation = activatedExperiment.variations[0]

        val impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                Collections.singletonMap("unknownAttribute", "blahValue"))

        val impression = gson.fromJson<EventBatch>(impressionEvent.body, EventBatch::class.java!!)

        // verify that no Feature is created for "unknownAtrribute" -> "blahValue"
        for (feature in impression.visitors[0].attributes!!) {
            assertFalse(feature.key === "unknownAttribute")
            assertFalse(feature.value === "blahValue")
        }
    }

    /**
     * Verify that supplying [EventBuilder] with a custom client engine and client version results in impression
     * events being sent with the overriden values.
     */
    @Test
    @Throws(Exception::class)
    fun createImpressionEventAndroidClientEngineClientVersion() {
        val builder = EventBuilder(ClientEngine.ANDROID_SDK, "0.0.0")
        val projectConfig = validProjectConfigV2()
        val activatedExperiment = projectConfig.experiments[0]
        val bucketedVariation = activatedExperiment.variations[0]
        val attribute = projectConfig.attributes[0]
        val userId = "userId"
        val attributeMap = Collections.singletonMap(attribute.key, "value")

        val impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap)
        val impression = gson.fromJson<EventBatch>(impressionEvent.body, EventBatch::class.java!!)

        assertThat(impression.clientName, `is`(ClientEngine.ANDROID_SDK.clientEngineValue))
        assertThat(impression.clientVersion, `is`("0.0.0"))
    }

    /**
     * Verify that supplying [EventBuilder] with a custom Android TV client engine and client version
     * results in impression events being sent with the overriden values.
     */
    @Test
    @Throws(Exception::class)
    fun createImpressionEventAndroidTVClientEngineClientVersion() {
        val clientVersion = "0.0.0"
        val builder = EventBuilder(ClientEngine.ANDROID_TV_SDK, clientVersion)
        val projectConfig = validProjectConfigV2()
        val activatedExperiment = projectConfig.experiments[0]
        val bucketedVariation = activatedExperiment.variations[0]
        val attribute = projectConfig.attributes[0]
        val userId = "userId"
        val attributeMap = Collections.singletonMap(attribute.key, "value")

        val impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap)
        val impression = gson.fromJson<EventBatch>(impressionEvent.body, EventBatch::class.java!!)

        assertThat(impression.clientName, `is`(ClientEngine.ANDROID_TV_SDK.clientEngineValue))
        assertThat(impression.clientVersion, `is`(clientVersion))
    }

    /**
     * Verify [com.optimizely.ab.event.internal.payload.EventBatch] event creation
     */
    @Test
    @Throws(Exception::class)
    fun createConversionEvent() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val attribute = validProjectConfig.attributes[0]
        val eventType = validProjectConfig.eventTypes[0]
        val userId = "userId"

        val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)

        val allExperiments = validProjectConfig.experiments
        val experimentsForEventKey = validProjectConfig.getExperimentsForEventKey(eventType.key)

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (experiment in allExperiments) {
            `when`<Variation>(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.variations[0])
        }
        val decisionService = DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler::class.java),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        val attributeMap = Collections.singletonMap(attribute.key, AUDIENCE_GRYFFINDOR_VALUE)
        val eventTagMap = HashMap<String, Any>()
        eventTagMap.put("boolean_param", false)
        eventTagMap.put("string_param", "123")
        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                userId,
                attributeMap)
        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.id,
                eventType.key,
                attributeMap,
                eventTagMap)

        val expectedDecisions = ArrayList<Decision>()

        for (experiment in experimentsForEventKey) {
            if (experiment.isRunning) {
                val layerState = Decision(experiment.layerId!!, experiment.id,
                        experiment.variations[0].id, false)
                expectedDecisions.add(layerState)
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent!!.endpointUrl, `is`(EventBuilder.EVENT_ENDPOINT))

        val conversion = gson.fromJson<EventBatch>(conversionEvent.body, EventBatch::class.java!!)

        // verify payload information
        assertThat(conversion.visitors[0].visitorId, `is`(userId))
        assertThat(conversion.visitors[0].snapshots[0].events[0].timestamp.toDouble(),
                closeTo(System.currentTimeMillis().toDouble(), 120.0))
        assertThat(conversion.projectId, `is`(validProjectConfig.projectId))
        assertThat(conversion.accountId, `is`(validProjectConfig.accountId))

        val feature = com.optimizely.ab.event.internal.payload.Attribute(attribute.id, attribute.key,
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE)
        val feature2 = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                validProjectConfig.botFiltering!!)
        val expectedUserFeatures: List<com.optimizely.ab.event.internal.payload.Attribute>

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(feature, feature2)
        else
            expectedUserFeatures = Arrays.asList(feature)

        assertEquals(conversion.visitors[0].attributes, expectedUserFeatures)
        assertThat(conversion.visitors[0].snapshots[0].decisions, containsInAnyOrder<Any>(*expectedDecisions.toTypedArray()))
        assertEquals(conversion.visitors[0].snapshots[0].events[0].entityId, eventType.id)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].key, eventType.key)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].revenue, null)
        assertTrue(conversion.visitors[0].attributes!!.containsAll(expectedUserFeatures))
        assertTrue(conversion.visitors[0].snapshots[0].events[0].tags == eventTagMap)
        assertFalse(conversion.visitors[0].snapshots[0].decisions[0].isCampaignHoldback)
        assertEquals(conversion.anonymizeIp, validProjectConfig.anonymizeIP)
        assertEquals(conversion.clientName, ClientEngine.JAVA_SDK.clientEngineValue)
        assertEquals(conversion.clientVersion, BuildVersionInfo.VERSION)
    }

    /**
     * Verify [com.optimizely.ab.event.internal.payload.EventBatch] event creation
     * passing User Agent reserved attribute in attribute map and to check it exist in visitors.attributes
     */
    @Test
    @Throws(Exception::class)
    fun createConversionEventPassingUserAgentAttribute() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val attribute = validProjectConfig.attributes[0]
        val eventType = validProjectConfig.eventTypes[0]
        val userId = "userId"

        val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)

        val allExperiments = validProjectConfig.experiments
        val experimentsForEventKey = validProjectConfig.getExperimentsForEventKey(eventType.key)

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (experiment in allExperiments) {
            `when`<Variation>(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.variations[0])
        }
        val decisionService = DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler::class.java),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        val attributeMap = HashMap<String, String>()
        attributeMap.put(attribute.key, AUDIENCE_GRYFFINDOR_VALUE)
        attributeMap.put(ControlAttribute.USER_AGENT_ATTRIBUTE.toString(), "Chrome")
        val eventTagMap = HashMap<String, Any>()
        eventTagMap.put("boolean_param", false)
        eventTagMap.put("string_param", "123")
        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                userId,
                attributeMap)
        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.id,
                eventType.key,
                attributeMap,
                eventTagMap)

        val expectedDecisions = ArrayList<Decision>()

        for (experiment in experimentsForEventKey) {
            if (experiment.isRunning) {
                val layerState = Decision(experiment.layerId!!, experiment.id,
                        experiment.variations[0].id, false)
                expectedDecisions.add(layerState)
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent!!.endpointUrl, `is`(EventBuilder.EVENT_ENDPOINT))

        val conversion = gson.fromJson<EventBatch>(conversionEvent.body, EventBatch::class.java!!)

        // verify payload information
        assertThat(conversion.visitors[0].visitorId, `is`(userId))
        assertThat(conversion.projectId, `is`(validProjectConfig.projectId))
        assertThat(conversion.accountId, `is`(validProjectConfig.accountId))

        val feature = com.optimizely.ab.event.internal.payload.Attribute(
                attribute.id, attribute.key,
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE)
        val userAgentFeature = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.USER_AGENT_ATTRIBUTE.toString(),
                ControlAttribute.USER_AGENT_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "Chrome")
        val botFilteringFeature = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                validProjectConfig.botFiltering!!)
        val expectedUserFeatures: List<com.optimizely.ab.event.internal.payload.Attribute>

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(userAgentFeature, feature, botFilteringFeature)
        else
            expectedUserFeatures = Arrays.asList(userAgentFeature, feature)

        assertEquals(conversion.visitors[0].attributes, expectedUserFeatures)
        assertThat(conversion.visitors[0].snapshots[0].decisions, containsInAnyOrder<Any>(*expectedDecisions.toTypedArray()))
        assertEquals(conversion.visitors[0].snapshots[0].events[0].entityId, eventType.id)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].key, eventType.key)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].revenue, null)
        assertTrue(conversion.visitors[0].attributes!!.containsAll(expectedUserFeatures))
        assertTrue(conversion.visitors[0].snapshots[0].events[0].tags == eventTagMap)
        assertFalse(conversion.visitors[0].snapshots[0].decisions[0].isCampaignHoldback)
        assertEquals(conversion.anonymizeIp, validProjectConfig.anonymizeIP)
        assertEquals(conversion.clientName, ClientEngine.JAVA_SDK.clientEngineValue)
        assertEquals(conversion.clientVersion, BuildVersionInfo.VERSION)
    }

    /**
     * Verify that "revenue" and "value" are properly recorded in a conversion request as [com.optimizely.ab.event.internal.payload.Event] objects.
     * "revenue" is fixed-point and "value" is floating-point.
     */
    @Test
    @Throws(Exception::class)
    fun createConversionParamsWithEventMetrics() {
        val revenue = 1234L
        val value = 13.37

        // use the "valid" project config and its associated experiment, variation, and attributes
        val attribute = validProjectConfig.attributes[0]
        val eventType = validProjectConfig.eventTypes[0]

        val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)

        // Bucket to the first variation for all experiments.
        for (experiment in validProjectConfig.experiments) {
            `when`<Variation>(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.variations[0])
        }
        val decisionService = DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler::class.java),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        val attributeMap = Collections.singletonMap(attribute.key, "value")
        val eventTagMap = HashMap<String, Any>()
        eventTagMap.put(ReservedEventKey.REVENUE.toString(), revenue)
        eventTagMap.put(ReservedEventKey.VALUE.toString(), value)
        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                userId,
                attributeMap)
        val conversionEvent = builder.createConversionEvent(validProjectConfig, experimentVariationMap, userId,
                eventType.id, eventType.key, attributeMap,
                eventTagMap)

        val conversion = gson.fromJson<EventBatch>(conversionEvent!!.body, EventBatch::class.java!!)
        // we're not going to verify everything, only the event metrics
        assertThat(conversion.visitors[0].snapshots[0].events[0].revenue!!.toLong(), `is`(revenue))
        assertThat(conversion.visitors[0].snapshots[0].events[0].value!!.toDouble(), `is`(value))
    }

    /**
     * Verify that precedence is given to forced variation bucketing over audience evaluation when constructing a
     * conversion event.
     */
    @Test
    fun createConversionEventForcedVariationBucketingPrecedesAudienceEval() {
        val eventType: EventType
        val whitelistedUserId: String
        if (datafileVersion == 4) {
            eventType = validProjectConfig.eventNameMapping[EVENT_BASIC_EVENT_KEY]!!
            whitelistedUserId = MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED
        } else {
            eventType = validProjectConfig.eventTypes[0]
            whitelistedUserId = "testUser1"
        }

        val decisionService = DecisionService(
                Bucketer(validProjectConfig),
                NoOpErrorHandler(),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        // attributes are empty so user won't be in the audience for experiment using the event, but bucketing
        // will still take place
        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                whitelistedUserId,
                emptyMap<String, String>())
        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                whitelistedUserId,
                eventType.id,
                eventType.key,
                emptyMap<String, String>(),
                emptyMap<String, Any>())
        assertNotNull(conversionEvent)

        val conversion = gson.fromJson<EventBatch>(conversionEvent!!.body, EventBatch::class.java!!)
        if (datafileVersion == 4) {
            // 2 experiments use the event
            // basic experiment has no audience
            // user is whitelisted in to one audience
            assertEquals(2, conversion.visitors[0].snapshots[0].decisions.size.toLong())
        } else {
            assertEquals(1, conversion.visitors[0].snapshots[0].decisions.size.toLong())
        }
    }

    /**
     * Verify that precedence is given to experiment status over forced variation bucketing when constructing a
     * conversion event.
     */
    @Test
    fun createConversionEventExperimentStatusPrecedesForcedVariation() {
        val eventType: EventType
        if (datafileVersion == 4) {
            eventType = validProjectConfig.eventNameMapping[EVENT_PAUSED_EXPERIMENT_KEY]!!
        } else {
            eventType = validProjectConfig.eventTypes[3]
        }
        val whitelistedUserId = PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL

        val bucketer = spy(Bucketer(validProjectConfig))
        val decisionService = DecisionService(
                bucketer,
                mock(ErrorHandler::class.java),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                whitelistedUserId,
                emptyMap<String, String>())
        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                whitelistedUserId,
                eventType.id,
                eventType.key,
                emptyMap<String, String>(),
                emptyMap<String, Any>())

        for (experiment in validProjectConfig.experiments) {
            verify(bucketer, never()).bucket(experiment, whitelistedUserId)
        }

        assertNull(conversionEvent)
    }

    /**
     * Verify that supplying [EventBuilder] with a custom client engine and client version results in conversion
     * events being sent with the overriden values.
     */
    @Test
    @Throws(Exception::class)
    fun createConversionEventAndroidClientEngineClientVersion() {
        val builder = EventBuilder(ClientEngine.ANDROID_SDK, "0.0.0")
        val attribute = validProjectConfig.attributes[0]
        val eventType = validProjectConfig.eventTypes[0]

        val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)
        for (experiment in validProjectConfig.experiments) {
            `when`<Variation>(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.variations[0])
        }
        val decisionService = DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler::class.java),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        val attributeMap = Collections.singletonMap(attribute.key, "value")
        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                userId,
                attributeMap)
        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.id,
                eventType.key,
                attributeMap,
                emptyMap<String, Any>())

        val conversion = gson.fromJson<EventBatch>(conversionEvent!!.body, EventBatch::class.java!!)

        assertThat(conversion.clientName, `is`(ClientEngine.ANDROID_SDK.clientEngineValue))
        assertThat(conversion.clientVersion, `is`("0.0.0"))
    }

    /**
     * Verify that supplying [EventBuilder] with a Android TV client engine and client version results in
     * conversion events being sent with the overriden values.
     */
    @Test
    @Throws(Exception::class)
    fun createConversionEventAndroidTVClientEngineClientVersion() {
        val clientVersion = "0.0.0"
        val builder = EventBuilder(ClientEngine.ANDROID_TV_SDK, clientVersion)
        val projectConfig = validProjectConfigV2()
        val attribute = projectConfig.attributes[0]
        val eventType = projectConfig.eventTypes[0]
        val userId = "userId"

        val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)
        for (experiment in projectConfig.experiments) {
            `when`<Variation>(mockBucketAlgorithm.bucket(experiment, userId))
                    .thenReturn(experiment.variations[0])
        }

        val attributeMap = Collections.singletonMap(attribute.key, "value")
        val experimentList = projectConfig.getExperimentsForEventKey(eventType.key)
        val experimentVariationMap = HashMap<Experiment, Variation>(experimentList.size)
        for (experiment in experimentList) {
            experimentVariationMap.put(experiment, experiment.variations[0])
        }

        val conversionEvent = builder.createConversionEvent(
                projectConfig,
                experimentVariationMap,
                userId,
                eventType.id,
                eventType.key,
                attributeMap,
                emptyMap<String, Any>())
        val conversion = gson.fromJson<EventBatch>(conversionEvent!!.body, EventBatch::class.java!!)

        assertThat(conversion.clientName, `is`(ClientEngine.ANDROID_TV_SDK.clientEngineValue))
        assertThat(conversion.clientVersion, `is`(clientVersion))
    }

    /**
     * Verify that supplying an empty Experiment Variation map to
     * [EventBuilder.createConversionEvent]
     * returns a null [LogEvent].
     */
    @Test
    fun createConversionEventReturnsNullWhenExperimentVariationMapIsEmpty() {
        val eventType = validProjectConfig.eventTypes[0]
        val builder = EventBuilder()

        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                emptyMap<Experiment, Variation>(),
                userId,
                eventType.id,
                eventType.key,
                emptyMap<String, String>(),
                emptyMap<String, String>()
        )

        assertNull(conversionEvent)
    }

    /**
     * Verify [com.optimizely.ab.event.internal.payload.EventBatch] event creation
     */
    @Test
    @Throws(Exception::class)
    fun createImpressionEventWithBucketingId() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val projectConfig = validProjectConfig
        val activatedExperiment = projectConfig.experiments[0]
        val bucketedVariation = activatedExperiment.variations[0]
        val attribute = projectConfig.attributes[0]
        val userId = "userId"
        val attributeMap = HashMap<String, String>()
        attributeMap.put(attribute.key, "value")

        attributeMap.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), "variation")

        val expectedDecision = Decision(activatedExperiment.layerId!!, activatedExperiment.id, bucketedVariation.id, false)

        val feature = com.optimizely.ab.event.internal.payload.Attribute(attribute.id, attribute.key,
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "value")
        val feature1 = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BUCKETING_ATTRIBUTE.toString(),
                ControlAttribute.BUCKETING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                "variation")
        val feature2 = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                validProjectConfig.botFiltering!!)

        val expectedUserFeatures: List<com.optimizely.ab.event.internal.payload.Attribute>

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(feature, feature1, feature2)
        else
            expectedUserFeatures = Arrays.asList(feature, feature1)

        val impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap)

        // verify that request endpoint is correct
        assertThat(impressionEvent.endpointUrl, `is`(EventBuilder.EVENT_ENDPOINT))

        val impression = gson.fromJson<EventBatch>(impressionEvent.body, EventBatch::class.java!!)

        // verify payload information
        assertThat(impression.visitors[0].visitorId, `is`(userId))
        assertThat(impression.visitors[0].snapshots[0].events[0].timestamp.toDouble(), closeTo(System.currentTimeMillis().toDouble(), 1000.0))
        assertFalse(impression.visitors[0].snapshots[0].decisions[0].isCampaignHoldback)
        assertThat(impression.anonymizeIp, `is`(projectConfig.anonymizeIP))
        assertThat(impression.projectId, `is`(projectConfig.projectId))
        assertThat(impression.visitors[0].snapshots[0].decisions[0], `is`(expectedDecision))
        assertThat(impression.visitors[0].snapshots[0].decisions[0].campaignId, `is`(activatedExperiment.layerId))
        assertThat(impression.accountId, `is`(projectConfig.accountId))

        //assertThat<List<Attribute>>(impression.visitors[0].attributes!!, `is`<List<Attribute>>(expectedUserFeatures))
        assertThat(impression.clientName, `is`(ClientEngine.JAVA_SDK.clientEngineValue))
        assertThat(impression.clientVersion, `is`(BuildVersionInfo.VERSION))
        assertNull(impression.visitors[0].sessionId)
    }

    /**
     * Verify [EventBatch] event creation
     */
    @Test
    @Throws(Exception::class)
    fun createConversionEventWithBucketingId() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        val attribute = validProjectConfig.attributes[0]
        val eventType = validProjectConfig.eventTypes[0]
        val userId = "userId"
        val bucketingId = "bucketingId"

        val mockBucketAlgorithm = mock<Bucketer>(Bucketer::class.java)

        val allExperiments = validProjectConfig.experiments
        val experimentsForEventKey = validProjectConfig.getExperimentsForEventKey(eventType.key)

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (experiment in allExperiments) {
            `when`<Variation>(mockBucketAlgorithm.bucket(experiment, bucketingId))
                    .thenReturn(experiment.variations[0])
        }
        val decisionService = DecisionService(
                mockBucketAlgorithm,
                mock(ErrorHandler::class.java),
                validProjectConfig,
                mock<UserProfileService>(UserProfileService::class.java)
        )

        val attributeMap = java.util.HashMap<String, String>()
        attributeMap.put(attribute.key, AUDIENCE_GRYFFINDOR_VALUE)
        attributeMap.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), bucketingId)

        val eventTagMap = HashMap<String, Any>()
        eventTagMap.put("boolean_param", false)
        eventTagMap.put("string_param", "123")
        val experimentVariationMap = createExperimentVariationMap(
                validProjectConfig,
                decisionService,
                eventType.key,
                userId,
                attributeMap)
        val conversionEvent = builder.createConversionEvent(
                validProjectConfig,
                experimentVariationMap,
                userId,
                eventType.id,
                eventType.key,
                attributeMap,
                eventTagMap)

        val expectedDecisions = ArrayList<Decision>()

        for (experiment in experimentsForEventKey) {
            if (experiment.isRunning) {
                val decision = Decision(experiment.layerId!!, experiment.id,
                        experiment.variations[0].id, false)
                expectedDecisions.add(decision)
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent!!.endpointUrl, `is`(EventBuilder.EVENT_ENDPOINT))

        val conversion = gson.fromJson<EventBatch>(conversionEvent.body, EventBatch::class.java!!)

        // verify payload information
        assertThat(conversion.visitors[0].visitorId, `is`(userId))
        assertThat(conversion.visitors[0].snapshots[0].events[0].timestamp.toDouble(), closeTo(System.currentTimeMillis().toDouble(), 1000.0))
        assertThat(conversion.projectId, `is`(validProjectConfig.projectId))
        assertThat(conversion.accountId, `is`(validProjectConfig.accountId))

        val attribute1 = com.optimizely.ab.event.internal.payload.Attribute(attribute.id, attribute.key,
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                AUDIENCE_GRYFFINDOR_VALUE)
        val attribute2 = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BUCKETING_ATTRIBUTE.toString(),
                ControlAttribute.BUCKETING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                bucketingId)
        val attribute3 = com.optimizely.ab.event.internal.payload.Attribute(
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE,
                validProjectConfig.botFiltering!!)
        val expectedUserFeatures: List<com.optimizely.ab.event.internal.payload.Attribute>

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(attribute1, attribute2, attribute3)
        else
            expectedUserFeatures = Arrays.asList(attribute1, attribute2)

        assertEquals(conversion.visitors[0].attributes, expectedUserFeatures)
        assertThat(conversion.visitors[0].snapshots[0].decisions, containsInAnyOrder<Any>(*expectedDecisions.toTypedArray()))
        assertEquals(conversion.visitors[0].snapshots[0].events[0].entityId, eventType.id)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].type, eventType.key)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].key, eventType.key)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].revenue, null)
        assertEquals(conversion.visitors[0].snapshots[0].events[0].quantity, null)
        assertTrue(conversion.visitors[0].snapshots[0].events[0].tags == eventTagMap)
        assertFalse(conversion.visitors[0].snapshots[0].decisions[0].isCampaignHoldback)
        assertEquals(conversion.anonymizeIp, validProjectConfig.anonymizeIP)
        assertEquals(conversion.clientName, ClientEngine.JAVA_SDK.clientEngineValue)
        assertEquals(conversion.clientVersion, BuildVersionInfo.VERSION)
    }

    companion object {

        @Parameterized.Parameters
        @Throws(IOException::class)
        fun data(): Collection<Array<Any>> {
            return Arrays.asList(*arrayOf(arrayOf(2, validProjectConfigV2()), arrayOf(4, validProjectConfigV4())))
        }

        private val userId = "userId"


        //========== helper methods =========//
        fun createExperimentVariationMap(projectConfig: ProjectConfig,
                                         decisionService: DecisionService,
                                         eventName: String,
                                         userId: String,
                                         attributes: Map<String, String>?): Map<Experiment, Variation> {

            val eventExperiments = projectConfig.getExperimentsForEventKey(eventName)
            val experimentVariationMap = HashMap<Experiment, Variation>(eventExperiments.size)
            for (experiment in eventExperiments) {
                if (experiment.isRunning) {
                    val variation = decisionService.getVariation(experiment, userId, attributes!!)
                    if (variation != null) {
                        experimentVariationMap.put(experiment, variation)
                    }
                }
            }

            return experimentVariationMap
        }
    }
}

