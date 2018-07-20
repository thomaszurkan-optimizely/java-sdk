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
package com.optimizely.ab.config.parser

import com.optimizely.ab.config.Attribute
import com.optimizely.ab.config.EventType
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Experiment.ExperimentStatus
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.LiveVariable
import com.optimizely.ab.config.LiveVariable.VariableStatus
import com.optimizely.ab.config.LiveVariable.VariableType
import com.optimizely.ab.config.LiveVariableUsageInstance
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Rollout
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.audience.AndCondition
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.config.audience.NotCondition
import com.optimizely.ab.config.audience.OrCondition
import com.optimizely.ab.config.audience.UserAttribute
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.util.ArrayList
import java.util.HashMap

/**
 * `json-simple`-based config parser implementation.
 */
internal class JsonSimpleConfigParser : ConfigParser {

    @Throws(ConfigParseException::class)
    override fun parseProjectConfig(json: String): ProjectConfig {
        try {
            val parser = JSONParser()
            val rootObject = parser.parse(json) as JSONObject

            val accountId = rootObject["accountId"] as String
            val projectId = rootObject["projectId"] as String
            val revision = rootObject["revision"] as String
            val version = rootObject["version"] as String
            val datafileVersion = Integer.parseInt(version)

            val experiments = parseExperiments(rootObject["experiments"] as JSONArray)

            val attributes: List<Attribute>
            attributes = parseAttributes(rootObject["attributes"] as JSONArray)

            val events = parseEvents(rootObject["events"] as JSONArray)
            val audiences = parseAudiences(parser.parse(rootObject["audiences"].toString()) as JSONArray)
            val groups = parseGroups(rootObject["groups"] as JSONArray)

            var anonymizeIP = false
            var liveVariables: List<LiveVariable>? = null
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
                liveVariables = parseLiveVariables(rootObject["variables"] as JSONArray)

                anonymizeIP = rootObject["anonymizeIP"] as Boolean
            }

            var featureFlags: List<FeatureFlag>? = null
            var rollouts: List<Rollout>? = null
            var botFiltering: Boolean? = null
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
                featureFlags = parseFeatureFlags(rootObject["featureFlags"] as JSONArray)
                rollouts = parseRollouts(rootObject["rollouts"] as JSONArray)
                if (rootObject.containsKey("botFiltering"))
                    botFiltering = rootObject["botFiltering"] as Boolean
            }

            return ProjectConfig(
                    accountId,
                    anonymizeIP,
                    botFiltering,
                    projectId,
                    revision,
                    version,
                    attributes,
                    audiences,
                    events,
                    experiments,
                    featureFlags,
                    groups,
                    liveVariables,
                    rollouts
            )
        } catch (ex: RuntimeException) {
            throw ConfigParseException("Unable to parse datafile: " + json, ex)
        } catch (e: Exception) {
            throw ConfigParseException("Unable to parse datafile: " + json, e)
        }

    }

    private fun parseExperiments(experimentJson: JSONArray, groupId: String = ""): List<Experiment> {
        val experiments = ArrayList<Experiment>(experimentJson.size)

        for (obj in experimentJson) {
            val experimentObject = obj as JSONObject
            val id = experimentObject["id"] as String
            val key = experimentObject["key"] as String
            val statusJson = experimentObject["status"]
            val status = if (statusJson == null)
                ExperimentStatus.NOT_STARTED.toString()
            else
                experimentObject["status"] as String
            val layerIdObject = experimentObject["layerId"]
            val layerId = if (layerIdObject == null) null else layerIdObject as String?

            val audienceIdsJson = experimentObject["audienceIds"] as JSONArray
            val audienceIds = ArrayList<String>(audienceIdsJson.size)

            for (audienceIdObj in audienceIdsJson) {
                audienceIds.add(audienceIdObj as String)
            }

            // parse the child objects
            val variations = parseVariations(experimentObject["variations"] as JSONArray)
            val userIdToVariationKeyMap = parseForcedVariations(experimentObject["forcedVariations"] as JSONObject)
            val trafficAllocations = parseTrafficAllocation(experimentObject["trafficAllocation"] as JSONArray)

            experiments.add(Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                    trafficAllocations, groupId))
        }

        return experiments
    }

    private fun parseExperimentIds(experimentIdsJsonArray: JSONArray): List<String> {
        val experimentIds = ArrayList<String>(experimentIdsJsonArray.size)

        for (experimentIdObj in experimentIdsJsonArray) {
            experimentIds.add(experimentIdObj as String)
        }

        return experimentIds
    }

    private fun parseFeatureFlags(featureFlagJson: JSONArray): List<FeatureFlag> {
        val featureFlags = ArrayList<FeatureFlag>(featureFlagJson.size)

        for (obj in featureFlagJson) {
            val featureFlagObject = obj as JSONObject
            val id = featureFlagObject["id"] as String
            val key = featureFlagObject["key"] as String
            val layerId = featureFlagObject["rolloutId"] as String

            val experimentIdsJsonArray = featureFlagObject["experimentIds"] as JSONArray
            val experimentIds = parseExperimentIds(experimentIdsJsonArray)

            val liveVariables = parseLiveVariables(featureFlagObject["variables"] as JSONArray)

            featureFlags.add(FeatureFlag(
                    id,
                    key,
                    layerId,
                    experimentIds,
                    liveVariables
            ))
        }

        return featureFlags
    }

    private fun parseVariations(variationJson: JSONArray): List<Variation> {
        val variations = ArrayList<Variation>(variationJson.size)

        for (obj in variationJson) {
            val variationObject = obj as JSONObject
            val id = variationObject["id"] as String
            val key = variationObject["key"] as String
            var featureEnabled: Boolean? = false

            if (variationObject.containsKey("featureEnabled"))
                featureEnabled = variationObject["featureEnabled"] as Boolean

            var liveVariableUsageInstances: List<LiveVariableUsageInstance>? = null
            if (variationObject.containsKey("variables")) {
                liveVariableUsageInstances = parseLiveVariableInstances(variationObject["variables"] as JSONArray)
            }

            variations.add(Variation(id, key, featureEnabled, liveVariableUsageInstances))
        }

        return variations
    }

    private fun parseForcedVariations(forcedVariationJson: JSONObject): Map<String, String> {
        val userIdToVariationKeyMap = HashMap<String, String>()
        for (obj in forcedVariationJson.entries) {
            val entry = obj as Map.Entry<String, String>
            userIdToVariationKeyMap.put(entry.key, entry.value)
        }

        return userIdToVariationKeyMap
    }

    private fun parseTrafficAllocation(trafficAllocationJson: JSONArray): List<TrafficAllocation> {
        val trafficAllocation = ArrayList<TrafficAllocation>(trafficAllocationJson.size)

        for (obj in trafficAllocationJson) {
            val allocationObject = obj as JSONObject
            val entityId = allocationObject["entityId"] as String
            val endOfRange = allocationObject["endOfRange"] as Long

            trafficAllocation.add(TrafficAllocation(entityId, endOfRange.toInt()))
        }

        return trafficAllocation
    }

    private fun parseAttributes(attributeJson: JSONArray): List<Attribute> {
        val attributes = ArrayList<Attribute>(attributeJson.size)

        for (obj in attributeJson) {
            val attributeObject = obj as JSONObject
            val id = attributeObject["id"] as String
            val key = attributeObject["key"] as String
            val segmentId = attributeObject["segmentId"] as String?

            attributes.add(Attribute(id, key, segmentId))
        }

        return attributes
    }

    private fun parseEvents(eventJson: JSONArray): List<EventType> {
        val events = ArrayList<EventType>(eventJson.size)

        for (obj in eventJson) {
            val eventObject = obj as JSONObject
            val experimentIdsJson = eventObject["experimentIds"] as JSONArray
            val experimentIds = parseExperimentIds(experimentIdsJson)

            val id = eventObject["id"] as String
            val key = eventObject["key"] as String

            events.add(EventType(id, key, experimentIds))
        }

        return events
    }

    @Throws(ParseException::class)
    private fun parseAudiences(audienceJson: JSONArray): List<Audience> {
        val parser = JSONParser()
        val audiences = ArrayList<Audience>(audienceJson.size)

        for (obj in audienceJson) {
            val audienceObject = obj as JSONObject
            val id = audienceObject["id"] as String
            val key = audienceObject["name"] as String
            val conditionString = audienceObject["conditions"] as String

            val conditionJson = parser.parse(conditionString) as JSONArray
            val conditions = parseConditions(conditionJson)
            audiences.add(Audience(id, key, conditions))
        }

        return audiences
    }

    private fun parseConditions(conditionJson: JSONArray): Condition {
        val conditions = ArrayList<Condition>()
        val operand = conditionJson[0] as String

        for (i in 1..conditionJson.size - 1) {
            val obj = conditionJson[i]
            if (obj is JSONArray) {
                conditions.add(parseConditions(conditionJson[i] as JSONArray))
            } else {
                val conditionMap = obj as JSONObject
                conditions.add(UserAttribute(conditionMap["name"] as String, conditionMap["type"] as String,
                        conditionMap["value"] as String?))
            }
        }

        val condition: Condition
        if (operand == "and") {
            condition = AndCondition(conditions)
        } else if (operand == "or") {
            condition = OrCondition(conditions)
        } else {
            condition = NotCondition(conditions[0])
        }

        return condition
    }

    private fun parseGroups(groupJson: JSONArray): List<Group> {
        val groups = ArrayList<Group>(groupJson.size)

        for (obj in groupJson) {
            val groupObject = obj as JSONObject
            val id = groupObject["id"] as String
            val policy = groupObject["policy"] as String
            val experiments = parseExperiments(groupObject["experiments"] as JSONArray, id)
            val trafficAllocations = parseTrafficAllocation(groupObject["trafficAllocation"] as JSONArray)

            groups.add(Group(id, policy, experiments, trafficAllocations))
        }

        return groups
    }

    private fun parseLiveVariables(liveVariablesJson: JSONArray): List<LiveVariable> {
        val liveVariables = ArrayList<LiveVariable>(liveVariablesJson.size)

        for (obj in liveVariablesJson) {
            val liveVariableObject = obj as JSONObject
            val id = liveVariableObject["id"] as String
            val key = liveVariableObject["key"] as String
            val defaultValue = liveVariableObject["defaultValue"] as String
            val type = VariableType.fromString(liveVariableObject["type"] as String)
            val status = VariableStatus.fromString(liveVariableObject["status"] as String?)

            liveVariables.add(LiveVariable(id, key, defaultValue, status, type!!))
        }

        return liveVariables
    }

    private fun parseLiveVariableInstances(liveVariableInstancesJson: JSONArray): List<LiveVariableUsageInstance> {
        val liveVariableUsageInstances = ArrayList<LiveVariableUsageInstance>(liveVariableInstancesJson.size)

        for (obj in liveVariableInstancesJson) {
            val liveVariableInstanceObject = obj as JSONObject
            val id = liveVariableInstanceObject["id"] as String
            val value = liveVariableInstanceObject["value"] as String

            liveVariableUsageInstances.add(LiveVariableUsageInstance(id, value))
        }

        return liveVariableUsageInstances
    }

    private fun parseRollouts(rolloutsJson: JSONArray): List<Rollout> {
        val rollouts = ArrayList<Rollout>(rolloutsJson.size)

        for (obj in rolloutsJson) {
            val rolloutObject = obj as JSONObject
            val id = rolloutObject["id"] as String
            val experiments = parseExperiments(rolloutObject["experiments"] as JSONArray)

            rollouts.add(Rollout(id, experiments))
        }

        return rollouts
    }
}//======== Helper methods ========//

