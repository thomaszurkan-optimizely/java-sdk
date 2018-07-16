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

import com.optimizely.ab.annotations.VisibleForTesting
import com.optimizely.ab.config.EventType
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Variation
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.event.internal.payload.Attribute
import com.optimizely.ab.event.internal.payload.Decision
import com.optimizely.ab.event.internal.payload.EventBatch
import com.optimizely.ab.event.internal.payload.Event
import com.optimizely.ab.event.internal.payload.Snapshot
import com.optimizely.ab.event.internal.payload.Visitor
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer
import com.optimizely.ab.event.internal.serializer.Serializer
import com.optimizely.ab.internal.EventTagUtils
import com.optimizely.ab.internal.ControlAttribute
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.UUID

class EventBuilder @JvmOverloads constructor(@VisibleForTesting
                                             val clientEngine: EventBatch.ClientEngine = EventBatch.ClientEngine.JAVA_SDK, @VisibleForTesting
                                             val clientVersion: String = BuildVersionInfo.VERSION) {

    private val serializer: Serializer

    init {
        this.serializer = DefaultJsonSerializer.instance
    }


    fun createImpressionEvent(projectConfig: ProjectConfig,
                              activatedExperiment: Experiment,
                              variation: Variation,
                              userId: String,
                              attributes: Map<String, String>): LogEvent {

        val decision = Decision(activatedExperiment.layerId!!, activatedExperiment.id,
                variation.id, false)
        val impressionEvent = Event(System.currentTimeMillis(), UUID.randomUUID().toString(), activatedExperiment.layerId,
                ACTIVATE_EVENT_KEY, null, null, null, ACTIVATE_EVENT_KEY, null)
        val snapshot = Snapshot(Arrays.asList(decision), Arrays.asList(impressionEvent))

        val visitor = Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot))
        val visitors = Arrays.asList(visitor)
        val eventBatch = EventBatch( projectConfig.accountId, visitors, projectConfig.anonymizeIP, projectConfig.projectId, clientEngine.clientEngineValue, clientVersion, projectConfig.revision)
        val payload = this.serializer.serialize(eventBatch)
        return LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, emptyMap<String, String>(), payload)
    }

    fun createConversionEvent(projectConfig: ProjectConfig,
                              experimentVariationMap: Map<Experiment, Variation>,
                              userId: String,
                              eventId: String,
                              eventName: String,
                              attributes: Map<String, String>,
                              eventTags: Map<String, *>): LogEvent? {

        if (experimentVariationMap.isEmpty()) {
            return null
        }

        val decisions = ArrayList<Decision>()
        for ((key, value) in experimentVariationMap) {
            val decision = Decision(key.layerId!!, key.id, value.id, false)
            decisions.add(decision)
        }

        val eventType = projectConfig.eventNameMapping[eventName]

        val conversionEvent = Event(System.currentTimeMillis(), UUID.randomUUID().toString(), eventType!!.id,
                eventType.key, null, EventTagUtils.getRevenueValue(eventTags), eventTags, eventType!!.key, EventTagUtils.getNumericValue(eventTags))
        val snapshot = Snapshot(decisions, Arrays.asList(conversionEvent))

        val visitor = Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot))
        val visitors = Arrays.asList(visitor)
        val eventBatch = EventBatch(projectConfig.accountId, visitors, projectConfig.anonymizeIP, projectConfig.projectId, clientEngine.clientEngineValue, clientVersion, projectConfig.revision)
        val payload = this.serializer.serialize(eventBatch)
        return LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, emptyMap<String, String>(), payload)
    }

    private fun buildAttributeList(projectConfig: ProjectConfig, attributes: Map<String, String>): List<Attribute> {
        val attributesList = ArrayList<Attribute>()

        for ((key, value) in attributes) {
            val attributeId = projectConfig.getAttributeId(projectConfig, key)
            if (attributeId != null) {
                val attribute = Attribute(attributeId,
                        key,
                        Attribute.CUSTOM_ATTRIBUTE_TYPE,
                        value)
                attributesList.add(attribute)
            }
        }

        //checks if botFiltering value is not set in the project config file.
        if (projectConfig.botFiltering != null) {
            val attribute = Attribute(
                    ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                    ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                    Attribute.CUSTOM_ATTRIBUTE_TYPE,
                    projectConfig.botFiltering
            )
            attributesList.add(attribute)
        }

        return attributesList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventBuilder::class.java!!)
        internal val EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events"
        internal val ACTIVATE_EVENT_KEY = "campaign_activated"
    }
}
