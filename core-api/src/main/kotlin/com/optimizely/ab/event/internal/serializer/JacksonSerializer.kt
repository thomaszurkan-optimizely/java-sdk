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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy

internal class JacksonSerializer : Serializer {

    private val mapper = ObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.SNAKE_CASE)

    override fun <T> serialize(payload: T): String {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        try {
            return mapper.writeValueAsString(payload)
        } catch (e: JsonProcessingException) {
            throw SerializationException("Unable to serialize payload", e)
        }

    }
}
