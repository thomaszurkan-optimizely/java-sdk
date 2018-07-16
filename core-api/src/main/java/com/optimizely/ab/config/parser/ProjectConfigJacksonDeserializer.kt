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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.optimizely.ab.config.Attribute
import com.optimizely.ab.config.EventType
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.LiveVariable
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Rollout
import com.optimizely.ab.config.audience.Audience

import java.io.IOException

internal class ProjectConfigJacksonDeserializer : JsonDeserializer<ProjectConfig>() {

    @Throws(IOException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ProjectConfig {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addDeserializer(Audience::class.java, AudienceJacksonDeserializer())
        module.addDeserializer(Group::class.java, GroupJacksonDeserializer())
        mapper.registerModule(module)

        val node = parser.codec.readTree<JsonNode>(parser)

        val accountId = node.get("accountId").textValue()
        val projectId = node.get("projectId").textValue()
        val revision = node.get("revision").textValue()
        val version = node.get("version").textValue()
        val datafileVersion = Integer.parseInt(version)

        val groups = mapper.readValue<List<Group>>(node.get("groups").toString(), object : TypeReference<List<Group>>() {

        })
        val experiments = mapper.readValue<List<Experiment>>(node.get("experiments").toString(),
                object : TypeReference<List<Experiment>>() {

                })

        val attributes: List<Attribute>
        attributes = mapper.readValue(node.get("attributes").toString(), object : TypeReference<List<Attribute>>() {

        })

        val events = mapper.readValue<List<EventType>>(node.get("events").toString(),
                object : TypeReference<List<EventType>>() {

                })
        val audiences = mapper.readValue<List<Audience>>(node.get("audiences").toString(),
                object : TypeReference<List<Audience>>() {

                })

        var anonymizeIP = false
        var liveVariables: List<LiveVariable>? = null
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
            liveVariables = mapper.readValue<List<LiveVariable>>(node.get("variables").toString(),
                    object : TypeReference<List<LiveVariable>>() {

                    })
            anonymizeIP = node.get("anonymizeIP").asBoolean()
        }

        var featureFlags: List<FeatureFlag>? = null
        var rollouts: List<Rollout>? = null
        var botFiltering: Boolean? = null
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            featureFlags = mapper.readValue<List<FeatureFlag>>(node.get("featureFlags").toString(),
                    object : TypeReference<List<FeatureFlag>>() {

                    })
            rollouts = mapper.readValue<List<Rollout>>(node.get("rollouts").toString(),
                    object : TypeReference<List<Rollout>>() {

                    })
            if (node.hasNonNull("botFiltering"))
                botFiltering = node.get("botFiltering").asBoolean()
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