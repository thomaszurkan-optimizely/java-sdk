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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation

import java.io.IOException
import java.util.ArrayList

class GroupJacksonDeserializer : JsonDeserializer<Group>() {

    @Throws(IOException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Group {
        val mapper = ObjectMapper()
        val node = parser.codec.readTree<JsonNode>(parser)

        val id = node.get("id").textValue()
        val policy = node.get("policy").textValue()
        val trafficAllocations = mapper.readValue<List<TrafficAllocation>>(node.get("trafficAllocation").toString(),
                object : TypeReference<List<TrafficAllocation>>() {

                })

        val groupExperimentsJson = node.get("experiments")
        val groupExperiments = ArrayList<Experiment>()
        if (groupExperimentsJson.isArray) {
            for (groupExperimentJson in groupExperimentsJson) {
                groupExperiments.add(parseExperiment(groupExperimentJson, id))
            }
        }

        return Group(id, policy, groupExperiments, trafficAllocations)
    }

    @Throws(IOException::class)
    private fun parseExperiment(experimentJson: JsonNode, groupId: String): Experiment {
        val mapper = ObjectMapper()

        val id = experimentJson.get("id").textValue()
        val key = experimentJson.get("key").textValue()
        val status = experimentJson.get("status").textValue()
        val layerIdJson = experimentJson.get("layerId")
        val layerId = layerIdJson?.textValue()
        val audienceIds = mapper.readValue<List<String>>(experimentJson.get("audienceIds").toString(),
                object : TypeReference<List<String>>() {

                })
        val variations = mapper.readValue<List<Variation>>(experimentJson.get("variations").toString(),
                object : TypeReference<List<Variation>>() {

                })
        val trafficAllocations = mapper.readValue<List<TrafficAllocation>>(experimentJson.get("trafficAllocation").toString(),
                object : TypeReference<List<TrafficAllocation>>() {

                })
        val userIdToVariationKeyMap = mapper.readValue<Map<String, String>>(
                experimentJson.get("forcedVariations").toString(), object : TypeReference<Map<String, String>>() {

        })

        return Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                trafficAllocations, groupId)
    }

}
