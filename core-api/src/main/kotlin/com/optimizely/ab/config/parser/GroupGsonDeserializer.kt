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
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.TrafficAllocation

import java.lang.reflect.Type
import java.util.ArrayList

class GroupGsonDeserializer : JsonDeserializer<Group> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Group {

        val jsonObject = json.asJsonObject

        val id = jsonObject.get("id").asString
        val policy = jsonObject.get("policy").asString

        val experiments = ArrayList<Experiment>()
        val experimentsJson = jsonObject.getAsJsonArray("experiments")
        for (obj in experimentsJson) {
            val experimentObj = obj as JsonObject
            experiments.add(GsonHelpers.parseExperiment(experimentObj, id, context))
        }

        val trafficAllocations = GsonHelpers.parseTrafficAllocation(jsonObject.getAsJsonArray("trafficAllocation"))

        return Group(id, policy, experiments, trafficAllocations)
    }
}
