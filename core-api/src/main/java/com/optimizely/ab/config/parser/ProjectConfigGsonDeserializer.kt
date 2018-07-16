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

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.optimizely.ab.config.Attribute
import com.optimizely.ab.config.EventType
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.LiveVariable
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Rollout
import com.optimizely.ab.config.audience.Audience

import java.lang.reflect.Type

/**
 * GSON [ProjectConfig] deserializer to allow the constructor to be used.
 */
class ProjectConfigGsonDeserializer : JsonDeserializer<ProjectConfig> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ProjectConfig {
        val jsonObject = json.asJsonObject

        val accountId = jsonObject.get("accountId").asString
        val projectId = jsonObject.get("projectId").asString
        val revision = jsonObject.get("revision").asString
        val version = jsonObject.get("version").asString
        val datafileVersion = Integer.parseInt(version)

        // generic list type tokens
        val groupsType = object : TypeToken<List<Group>>() {

        }.type
        val experimentsType = object : TypeToken<List<Experiment>>() {

        }.type
        val attributesType = object : TypeToken<List<Attribute>>() {

        }.type
        val eventsType = object : TypeToken<List<EventType>>() {

        }.type
        val audienceType = object : TypeToken<List<Audience>>() {

        }.type

        val groups = context.deserialize<List<Group>>(jsonObject.get("groups").asJsonArray, groupsType)
        val experiments = context.deserialize<List<Experiment>>(jsonObject.get("experiments").asJsonArray, experimentsType)

        val attributes: List<Attribute>
        attributes = context.deserialize(jsonObject.get("attributes"), attributesType)

        val events = context.deserialize<List<EventType>>(jsonObject.get("events").asJsonArray, eventsType)
        val audiences = context.deserialize<List<Audience>>(jsonObject.get("audiences").asJsonArray, audienceType)

        var anonymizeIP = false
        // live variables should be null if using V2
        var liveVariables: List<LiveVariable>? = null
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
            val liveVariablesType = object : TypeToken<List<LiveVariable>>() {

            }.type
            liveVariables = context.deserialize<List<LiveVariable>>(jsonObject.getAsJsonArray("variables"), liveVariablesType)

            anonymizeIP = jsonObject.get("anonymizeIP").asBoolean
        }

        var featureFlags: List<FeatureFlag>? = null
        var rollouts: List<Rollout>? = null
        var botFiltering: Boolean? = null
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            val featureFlagsType = object : TypeToken<List<FeatureFlag>>() {

            }.type
            featureFlags = context.deserialize<List<FeatureFlag>>(jsonObject.getAsJsonArray("featureFlags"), featureFlagsType)
            val rolloutsType = object : TypeToken<List<Rollout>>() {

            }.type
            rollouts = context.deserialize<List<Rollout>>(jsonObject.get("rollouts").asJsonArray, rolloutsType)
            if (jsonObject.has("botFiltering"))
                botFiltering = jsonObject.get("botFiltering").asBoolean
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
    }
}
