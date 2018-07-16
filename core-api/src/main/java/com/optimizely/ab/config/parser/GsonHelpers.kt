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
package com.optimizely.ab.config.parser

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.optimizely.ab.bucketing.DecisionService
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Experiment.ExperimentStatus
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.LiveVariable
import com.optimizely.ab.config.LiveVariableUsageInstance
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Type
import java.util.ArrayList
import java.util.HashMap

internal object GsonHelpers {

    private val logger = LoggerFactory.getLogger(DecisionService::class.java!!)

    private fun parseVariations(variationJson: JsonArray, context: JsonDeserializationContext): List<Variation> {
        val variations = ArrayList<Variation>(variationJson.size())
        for (obj in variationJson) {
            val variationObject = obj as JsonObject
            val id = variationObject.get("id").asString
            val key = variationObject.get("key").asString
            var featureEnabled: Boolean? = false
            if (variationObject.has("featureEnabled"))
                featureEnabled = variationObject.get("featureEnabled").asBoolean

            var variableUsageInstances: List<LiveVariableUsageInstance>? = null
            // this is an existence check rather than a version check since it's difficult to pass data
            // across deserializers.
            if (variationObject.has("variables")) {
                val liveVariableUsageInstancesType = object : TypeToken<List<LiveVariableUsageInstance>>() {

                }.type
                variableUsageInstances = context.deserialize<List<LiveVariableUsageInstance>>(variationObject.getAsJsonArray("variables"),
                        liveVariableUsageInstancesType)
            }

            variations.add(Variation(id, key, featureEnabled, variableUsageInstances))
        }

        return variations
    }

    private fun parseForcedVariations(forcedVariationJson: JsonObject): Map<String, String> {
        val userIdToVariationKeyMap = HashMap<String, String>()
        val entrySet = forcedVariationJson.entrySet()
        for ((key, value) in entrySet) {
            userIdToVariationKeyMap.put(key, value.asString)
        }

        return userIdToVariationKeyMap
    }

    fun parseTrafficAllocation(trafficAllocationJson: JsonArray): List<TrafficAllocation> {
        val trafficAllocation = ArrayList<TrafficAllocation>(trafficAllocationJson.size())

        for (obj in trafficAllocationJson) {
            val allocationObject = obj as JsonObject
            val entityId = allocationObject.get("entityId").asString
            val endOfRange = allocationObject.get("endOfRange").asInt

            trafficAllocation.add(TrafficAllocation(entityId, endOfRange))
        }

        return trafficAllocation
    }

    fun parseExperiment(experimentJson: JsonObject, groupId: String, context: JsonDeserializationContext): Experiment {
        val id = experimentJson.get("id").asString
        val key = experimentJson.get("key").asString
        val experimentStatusJson = experimentJson.get("status")
        val status = if (experimentStatusJson.isJsonNull)
            ExperimentStatus.NOT_STARTED.toString()
        else
            experimentStatusJson.asString

        val layerIdJson = experimentJson.get("layerId")
        val layerId = layerIdJson?.asString

        val audienceIdsJson = experimentJson.getAsJsonArray("audienceIds")
        val audienceIds = ArrayList<String>(audienceIdsJson.size())
        for (audienceIdObj in audienceIdsJson) {
            audienceIds.add(audienceIdObj.asString)
        }

        // parse the child objects
        val variations = parseVariations(experimentJson.getAsJsonArray("variations"), context)
        val userIdToVariationKeyMap = parseForcedVariations(experimentJson.getAsJsonObject("forcedVariations"))
        val trafficAllocations = parseTrafficAllocation(experimentJson.getAsJsonArray("trafficAllocation"))

        return Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                trafficAllocations, groupId)
    }

    fun parseExperiment(experimentJson: JsonObject, context: JsonDeserializationContext): Experiment {
        return parseExperiment(experimentJson, "", context)
    }

    fun parseFeatureFlag(featureFlagJson: JsonObject, context: JsonDeserializationContext): FeatureFlag {
        val id = featureFlagJson.get("id").asString
        val key = featureFlagJson.get("key").asString
        val layerId = featureFlagJson.get("rolloutId").asString

        val experimentIdsJson = featureFlagJson.getAsJsonArray("experimentIds")
        val experimentIds = ArrayList<String>()
        for (experimentIdObj in experimentIdsJson) {
            experimentIds.add(experimentIdObj.asString)
        }

        var liveVariables: List<LiveVariable> = ArrayList()
        try {
            val liveVariableType = object : TypeToken<List<LiveVariable>>() {

            }.type
            liveVariables = context.deserialize(featureFlagJson.getAsJsonArray("variables"),
                    liveVariableType)
        } catch (exception: JsonParseException) {
            logger.warn("Unable to parse variables for feature \"" + key
                    + "\". JsonParseException: " + exception)
        }

        return FeatureFlag(
                id,
                key,
                layerId,
                experimentIds,
                liveVariables
        )
    }
}
