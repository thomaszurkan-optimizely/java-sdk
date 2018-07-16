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
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.HashMap

/**
 * `org.json`-based config parser implementation.
 */
internal class JsonConfigParser : ConfigParser {

    @Throws(ConfigParseException::class)
    override fun parseProjectConfig(json: String): ProjectConfig {
        try {
            val rootObject = JSONObject(json)

            val accountId = rootObject.getString("accountId")
            val projectId = rootObject.getString("projectId")
            val revision = rootObject.getString("revision")
            val version = rootObject.getString("version")
            val datafileVersion = Integer.parseInt(version)

            val experiments = parseExperiments(rootObject.getJSONArray("experiments"))

            val attributes: List<Attribute>
            attributes = parseAttributes(rootObject.getJSONArray("attributes"))

            val events = parseEvents(rootObject.getJSONArray("events"))
            val audiences = parseAudiences(rootObject.getJSONArray("audiences"))
            val groups = parseGroups(rootObject.getJSONArray("groups"))

            var anonymizeIP = false
            var liveVariables: List<LiveVariable>? = null
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
                liveVariables = parseLiveVariables(rootObject.getJSONArray("variables"))

                anonymizeIP = rootObject.getBoolean("anonymizeIP")
            }

            var featureFlags: List<FeatureFlag>? = null
            var rollouts: List<Rollout>? = null
            var botFiltering: Boolean? = null
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
                featureFlags = parseFeatureFlags(rootObject.getJSONArray("featureFlags"))
                rollouts = parseRollouts(rootObject.getJSONArray("rollouts"))
                if (rootObject.has("botFiltering"))
                    botFiltering = rootObject.getBoolean("botFiltering")
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
        } catch (e: Exception) {
            throw ConfigParseException("Unable to parse datafile: " + json, e)
        }

    }

    private fun parseExperiments(experimentJson: JSONArray, groupId: String = ""): List<Experiment> {
        val experiments = ArrayList<Experiment>(experimentJson.length())

        for (obj in experimentJson) {
            val experimentObject = obj as JSONObject
            val id = experimentObject.getString("id")
            val key = experimentObject.getString("key")
            val status = if (experimentObject.isNull("status"))
                ExperimentStatus.NOT_STARTED.toString()
            else
                experimentObject.getString("status")
            val layerId = if (experimentObject.has("layerId")) experimentObject.getString("layerId") else null

            val audienceIdsJson = experimentObject.getJSONArray("audienceIds")
            val audienceIds = ArrayList<String>(audienceIdsJson.length())

            for (audienceIdObj in audienceIdsJson) {
                audienceIds.add(audienceIdObj as String)
            }

            // parse the child objects
            val variations = parseVariations(experimentObject.getJSONArray("variations"))
            val userIdToVariationKeyMap = parseForcedVariations(experimentObject.getJSONObject("forcedVariations"))
            val trafficAllocations = parseTrafficAllocation(experimentObject.getJSONArray("trafficAllocation"))

            experiments.add(Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                    trafficAllocations, groupId))
        }

        return experiments
    }

    private fun parseExperimentIds(experimentIdsJson: JSONArray): List<String> {
        val experimentIds = ArrayList<String>(experimentIdsJson.length())

        for (experimentIdObj in experimentIdsJson) {
            experimentIds.add(experimentIdObj as String)
        }

        return experimentIds
    }

    private fun parseFeatureFlags(featureFlagJson: JSONArray): List<FeatureFlag> {
        val featureFlags = ArrayList<FeatureFlag>(featureFlagJson.length())

        for (obj in featureFlagJson) {
            val featureFlagObject = obj as JSONObject
            val id = featureFlagObject.getString("id")
            val key = featureFlagObject.getString("key")
            val layerId = featureFlagObject.getString("rolloutId")

            val experimentIds = parseExperimentIds(featureFlagObject.getJSONArray("experimentIds"))

            val variables = parseLiveVariables(featureFlagObject.getJSONArray("variables"))

            featureFlags.add(FeatureFlag(
                    id,
                    key,
                    layerId,
                    experimentIds,
                    variables
            ))
        }

        return featureFlags
    }

    private fun parseVariations(variationJson: JSONArray): List<Variation> {
        val variations = ArrayList<Variation>(variationJson.length())

        for (obj in variationJson) {
            val variationObject = obj as JSONObject
            val id = variationObject.getString("id")
            val key = variationObject.getString("key")
            var featureEnabled: Boolean? = false

            if (variationObject.has("featureEnabled"))
                featureEnabled = variationObject.getBoolean("featureEnabled")

            var liveVariableUsageInstances: List<LiveVariableUsageInstance>? = null
            if (variationObject.has("variables")) {
                liveVariableUsageInstances = parseLiveVariableInstances(variationObject.getJSONArray("variables"))
            }

            variations.add(Variation(id, key, featureEnabled, liveVariableUsageInstances))
        }

        return variations
    }

    private fun parseForcedVariations(forcedVariationJson: JSONObject): Map<String, String> {
        val userIdToVariationKeyMap = HashMap<String, String>()
        val userIdSet = forcedVariationJson.keySet()

        for (userId in userIdSet) {
            userIdToVariationKeyMap.put(userId, forcedVariationJson.get(userId).toString())
        }

        return userIdToVariationKeyMap
    }

    private fun parseTrafficAllocation(trafficAllocationJson: JSONArray): List<TrafficAllocation> {
        val trafficAllocation = ArrayList<TrafficAllocation>(trafficAllocationJson.length())

        for (obj in trafficAllocationJson) {
            val allocationObject = obj as JSONObject
            val entityId = allocationObject.getString("entityId")
            val endOfRange = allocationObject.getInt("endOfRange")

            trafficAllocation.add(TrafficAllocation(entityId, endOfRange))
        }

        return trafficAllocation
    }

    private fun parseAttributes(attributeJson: JSONArray): List<Attribute> {
        val attributes = ArrayList<Attribute>(attributeJson.length())

        for (obj in attributeJson) {
            val attributeObject = obj as JSONObject
            val id = attributeObject.getString("id")
            val key = attributeObject.getString("key")

            attributes.add(Attribute(id, key, attributeObject.optString("segmentId", null)))
        }

        return attributes
    }

    private fun parseEvents(eventJson: JSONArray): List<EventType> {
        val events = ArrayList<EventType>(eventJson.length())

        for (obj in eventJson) {
            val eventObject = obj as JSONObject
            val experimentIds = parseExperimentIds(eventObject.getJSONArray("experimentIds"))

            val id = eventObject.getString("id")
            val key = eventObject.getString("key")

            events.add(EventType(id, key, experimentIds))
        }

        return events
    }

    private fun parseAudiences(audienceJson: JSONArray): List<Audience> {
        val audiences = ArrayList<Audience>(audienceJson.length())

        for (obj in audienceJson) {
            val audienceObject = obj as JSONObject
            val id = audienceObject.getString("id")
            val key = audienceObject.getString("name")
            val conditionString = audienceObject.getString("conditions")

            val conditionJson = JSONArray(conditionString)
            val conditions = parseConditions(conditionJson)
            audiences.add(Audience(id, key, conditions))
        }

        return audiences
    }

    private fun parseConditions(conditionJson: JSONArray): Condition {
        val conditions = ArrayList<Condition>()
        val operand = conditionJson.get(0) as String

        for (i in 1..conditionJson.length() - 1) {
            val obj = conditionJson.get(i)
            if (obj is JSONArray) {
                conditions.add(parseConditions(conditionJson.getJSONArray(i)))
            } else {
                val conditionMap = obj as JSONObject
                var value: String? = null
                if (conditionMap.has("value")) {
                    value = conditionMap.getString("value")
                }
                conditions.add(UserAttribute(
                        conditionMap.get("name") as String,
                        conditionMap.get("type") as String,
                        value
                ))
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
        val groups = ArrayList<Group>(groupJson.length())

        for (obj in groupJson) {
            val groupObject = obj as JSONObject
            val id = groupObject.getString("id")
            val policy = groupObject.getString("policy")
            val experiments = parseExperiments(groupObject.getJSONArray("experiments"), id)
            val trafficAllocations = parseTrafficAllocation(groupObject.getJSONArray("trafficAllocation"))

            groups.add(Group(id, policy, experiments, trafficAllocations))
        }

        return groups
    }

    private fun parseLiveVariables(liveVariablesJson: JSONArray): List<LiveVariable> {
        val liveVariables = ArrayList<LiveVariable>(liveVariablesJson.length())

        for (obj in liveVariablesJson) {
            val liveVariableObject = obj as JSONObject
            val id = liveVariableObject.getString("id")
            val key = liveVariableObject.getString("key")
            val defaultValue = liveVariableObject.getString("defaultValue")
            val type = VariableType.fromString(liveVariableObject.getString("type"))
            var status: VariableStatus? = null
            if (liveVariableObject.has("status")) {
                status = VariableStatus.fromString(liveVariableObject.getString("status"))
            }

            liveVariables.add(LiveVariable(id, key, defaultValue, status, type!!))
        }

        return liveVariables
    }

    private fun parseLiveVariableInstances(liveVariableInstancesJson: JSONArray): List<LiveVariableUsageInstance> {
        val liveVariableUsageInstances = ArrayList<LiveVariableUsageInstance>(liveVariableInstancesJson.length())

        for (obj in liveVariableInstancesJson) {
            val liveVariableInstanceObject = obj as JSONObject
            val id = liveVariableInstanceObject.getString("id")
            val value = liveVariableInstanceObject.getString("value")

            liveVariableUsageInstances.add(LiveVariableUsageInstance(id, value))
        }

        return liveVariableUsageInstances
    }

    private fun parseRollouts(rolloutsJson: JSONArray): List<Rollout> {
        val rollouts = ArrayList<Rollout>(rolloutsJson.length())

        for (obj in rolloutsJson) {
            val rolloutObject = obj as JSONObject
            val id = rolloutObject.getString("id")
            val experiments = parseExperiments(rolloutObject.getJSONArray("experiments"))

            rollouts.add(Rollout(id, experiments))
        }

        return rollouts
    }
}//======== Helper methods ========//
