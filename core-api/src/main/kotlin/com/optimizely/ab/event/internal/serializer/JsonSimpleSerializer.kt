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

import com.optimizely.ab.event.internal.payload.Attribute
import com.optimizely.ab.event.internal.payload.Decision
import com.optimizely.ab.event.internal.payload.EventBatch

import com.optimizely.ab.event.internal.payload.Event
import com.optimizely.ab.event.internal.payload.Snapshot
import com.optimizely.ab.event.internal.payload.Visitor
import org.json.simple.JSONArray
import org.json.simple.JSONObject

internal class JsonSimpleSerializer : Serializer {

    override fun <T> serialize(payload: T): String {
        val payloadJsonObj = serializeEventBatch(payload as EventBatch)

        return payloadJsonObj.toJSONString()
    }

    private fun serializeEventBatch(eventBatch: EventBatch): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put("account_id", eventBatch.accountId)
        jsonObject.put("visitors", serializeVisitors(eventBatch.visitors))
        if (eventBatch.anonymizeIp != null) jsonObject.put("anonymize_ip", eventBatch.anonymizeIp)
        if (eventBatch.clientName != null) jsonObject.put("client_name", eventBatch.clientName)
        if (eventBatch.clientVersion != null) jsonObject.put("client_version", eventBatch.clientVersion)
        if (eventBatch.projectId != null) jsonObject.put("project_id", eventBatch.projectId)
        if (eventBatch.revision != null) jsonObject.put("revision", eventBatch.revision)

        return jsonObject

    }

    private fun serializeVisitors(visitors: List<Visitor>): JSONArray {
        val jsonArray = JSONArray()

        for (v in visitors) {
            jsonArray.add(serializeVisitor(v))
        }

        return jsonArray
    }

    private fun serializeVisitor(visitor: Visitor): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put("visitor_id", visitor.visitorId)

        if (visitor.sessionId != null) jsonObject.put("session_id", visitor.sessionId)

        if (visitor.attributes != null) jsonObject.put("attributes", serializeFeatures(visitor.attributes))

        jsonObject.put("snapshots", serializeSnapshots(visitor.snapshots))

        return jsonObject
    }

    private fun serializeSnapshots(snapshots: List<Snapshot>): JSONArray {
        val jsonArray = JSONArray()

        for (snapshot in snapshots) {
            jsonArray.add(serializeSnapshot(snapshot))
        }

        return jsonArray
    }

    private fun serializeSnapshot(snapshot: Snapshot): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put("decisions", serializeDecisions(snapshot.decisions))
        jsonObject.put("events", serializeEvents(snapshot.events))

        return jsonObject
    }

    private fun serializeEvents(events: List<Event>): JSONArray {
        val jsonArray = JSONArray()

        for (event in events) {
            jsonArray.add(serializeEvent(event))
        }

        return jsonArray
    }

    private fun serializeEvent(eventV3: Event): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put("timestamp", eventV3.timestamp)
        jsonObject.put("uuid", eventV3.uuid)
        jsonObject.put("key", eventV3.key)

        if (eventV3.entityId != null) jsonObject.put("entity_id", eventV3.entityId)
        if (eventV3.quantity != null) jsonObject.put("quantity", eventV3.quantity)
        if (eventV3.revenue != null) jsonObject.put("revenue", eventV3.revenue)
        if (eventV3.tags != null) jsonObject.put("tags", serializeTags(eventV3.tags))
        if (eventV3.type != null) jsonObject.put("type", eventV3.type)
        if (eventV3.value != null) jsonObject.put("value", eventV3.value)

        return jsonObject
    }

    private fun serializeTags(tags: Map<String, *>?): JSONArray {
        val jsonArray = JSONArray()
        for ((key, value) in tags!!) {
            if (value != null) {
                val jsonObject = JSONObject()
                jsonObject.put(key, value)
            }
        }

        return jsonArray
    }

    private fun serializeDecision(decision: Decision): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("campaign_id", decision.campaignId)
        if (decision.experimentId != null) jsonObject.put("experiment_id", decision.experimentId)
        if (decision.variationId != null) jsonObject.put("variation_id", decision.variationId)
        jsonObject.put("is_campaign_holdback", decision.isCampaignHoldback)

        return jsonObject
    }

    private fun serializeFeatures(features: List<Attribute>?): JSONArray {
        val jsonArray = JSONArray()
        for (feature in features!!) {
            jsonArray.add(serializeFeature(feature))
        }

        return jsonArray
    }

    private fun serializeFeature(feature: Attribute): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", feature.type)
        jsonObject.put("value", feature.value)
        if (feature.entityId != null) jsonObject.put("entity_id", feature.entityId)
        if (feature.key != null) jsonObject.put("key", feature.key)

        return jsonObject
    }

    private fun serializeDecisions(layerStates: List<Decision>): JSONArray {
        val jsonArray = JSONArray()
        for (layerState in layerStates) {
            jsonArray.add(serializeDecision(layerState))
        }

        return jsonArray
    }
}
