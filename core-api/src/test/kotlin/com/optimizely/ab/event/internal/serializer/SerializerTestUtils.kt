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
package com.optimizely.ab.event.internal.serializer

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.optimizely.ab.event.internal.payload.Attribute
import com.optimizely.ab.event.internal.payload.Decision
import com.optimizely.ab.event.internal.payload.EventBatch
import com.optimizely.ab.event.internal.payload.Event
import com.optimizely.ab.event.internal.payload.Snapshot
import com.optimizely.ab.event.internal.payload.Visitor

import java.io.IOException
import java.util.Arrays
import java.util.Collections

object SerializerTestUtils {

    private val visitorId = "testvisitor"
    private val timestamp = 12345L
    private val isGlobalHoldback = false
    private val projectId = "1"
    private val layerId = "2"
    private val accountId = "3"
    private val variationId = "4"
    private val isLayerHoldback = false
    private val experimentId = "5"
    private val sessionId = "sessionid"
    private val revision = "1"
    private val decision = Decision(layerId, experimentId, variationId, isLayerHoldback)

    private val featureId = "6"
    private val featureName = "testfeature"
    private val featureType = "custom"
    private val featureValue = "testfeaturevalue"
    private val shouldIndex = true
    private val userFeatures = listOf<Attribute>(Attribute(featureId, featureName, featureType, featureValue))

    private val actionTriggered = true

    private val eventEntityId = "7"
    private val eventName = "testevent"
    private val eventMetricName = "revenue"
    private val eventMetricValue = 5000L

    private val events = listOf<Event>(Event(timestamp,
            "uuid", eventEntityId, eventName, null, 5000L, null, eventName, null))

    internal fun generateImpression(): EventBatch {
        val snapshot = Snapshot(Arrays.asList(decision), events)

        val vistor = Visitor(visitorId, null, userFeatures, Arrays.asList(snapshot))
        val impression = EventBatch(accountId, Arrays.asList(vistor), false, projectId, revision)
        impression.projectId = projectId
        impression.accountId = accountId
        impression.clientVersion = "0.1.1"
        impression.anonymizeIp = true
        impression.revision = revision

        return impression
    }

    internal fun generateImpressionWithSessionId(): EventBatch {
        val impression = generateImpression()
        impression.visitors[0].sessionId = sessionId

        return impression
    }

    internal fun generateConversion(): EventBatch {
        val conversion = generateImpression()
        conversion.clientVersion = "0.1.1"
        conversion.anonymizeIp = true
        conversion.revision = revision

        return conversion
    }

    internal fun generateConversionWithSessionId(): EventBatch {
        val conversion = generateConversion()
        conversion.visitors[0].sessionId = sessionId

        return conversion
    }

    @Throws(IOException::class)
    internal fun generateImpressionJson(): String {
        val impressionJson = Resources.toString(Resources.getResource("serializer/impression.json"), Charsets.UTF_8)
        return impressionJson.replace("\\s+".toRegex(), "")
    }

    @Throws(IOException::class)
    internal fun generateImpressionWithSessionIdJson(): String {
        val impressionJson = Resources.toString(Resources.getResource("serializer/impression-session-id.json"),
                Charsets.UTF_8)
        return impressionJson.replace("\\s+".toRegex(), "")
    }

    @Throws(IOException::class)
    internal fun generateConversionJson(): String {
        val conversionJson = Resources.toString(Resources.getResource("serializer/conversion.json"), Charsets.UTF_8)
        return conversionJson.replace("\\s+".toRegex(), "")
    }

    @Throws(IOException::class)
    internal fun generateConversionWithSessionIdJson(): String {
        val conversionJson = Resources.toString(Resources.getResource("serializer/conversion-session-id.json"),
                Charsets.UTF_8)
        return conversionJson.replace("\\s+".toRegex(), "")
    }
}
