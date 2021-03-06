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

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonParseException

import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken

import com.optimizely.ab.config.audience.AndCondition
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.config.audience.NotCondition
import com.optimizely.ab.config.audience.OrCondition
import com.optimizely.ab.config.audience.UserAttribute

import java.lang.reflect.Type

import java.util.ArrayList

class AudienceGsonDeserializer : JsonDeserializer<Audience> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Audience {
        val gson = Gson()
        val parser = JsonParser()
        val jsonObject = json.asJsonObject

        val id = jsonObject.get("id").asString
        val name = jsonObject.get("name").asString

        val conditionsElement = parser.parse(jsonObject.get("conditions").asString)

        val rawObjectList = gson.fromJson<List<Condition>>(conditionsElement, List::class.java)
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
                val conditionMap = rawObjectList[i] as LinkedTreeMap<String, String>
                if (conditionMap["name"] != null  && conditionMap["type"] != null && conditionMap["value"] != null) {
                    conditions.add(UserAttribute(conditionMap["name"]!!, conditionMap["type"]!!,
                            conditionMap["value"]!!))
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
