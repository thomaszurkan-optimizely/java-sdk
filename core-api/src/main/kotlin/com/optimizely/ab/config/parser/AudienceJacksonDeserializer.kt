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
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.reflect.TypeToken

import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.AndCondition
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.config.audience.UserAttribute
import com.optimizely.ab.config.audience.NotCondition
import com.optimizely.ab.config.audience.OrCondition

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class AudienceJacksonDeserializer : JsonDeserializer<Audience>() {

    @Throws(IOException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Audience {
        val mapper = ObjectMapper()
        val node = parser.codec.readTree<JsonNode>(parser)

        val id = node.get("id").textValue()
        val name = node.get("name").textValue()
        val rawObjectList = mapper.readValue(node.get("conditions").textValue(), List::class.java)
        val conditions = parseConditions(rawObjectList)

        return Audience(id, name, conditions)
    }

    private fun parseConditions(rawObjectList: List<*>): Condition {
        val conditions = ArrayList<Condition>()
        val operand = rawObjectList[0] as String

        for (i in 1..rawObjectList.size - 1) {
            val obj = rawObjectList[i]
            if (obj is List<*>) {
                val objectList = rawObjectList[i] as List<*>
                conditions.add(parseConditions(objectList))
            } else {
                val conditionMap = rawObjectList[i] as HashMap<String, String>
                conditionMap["name"]?.let {
                    conditions.add(UserAttribute(it, conditionMap["type"]!!,
                            conditionMap["value"]))

                }
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
}

