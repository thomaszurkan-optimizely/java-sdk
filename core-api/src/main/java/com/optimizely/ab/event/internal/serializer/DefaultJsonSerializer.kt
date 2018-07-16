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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.optimizely.ab.config.parser.MissingJsonParserException

/**
 * Factory for generating [Serializer] instances, based on the json library available on the classpath.
 */
object DefaultJsonSerializer {

    private val logger = LoggerFactory.getLogger(DefaultJsonSerializer::class.java!!)

    val instance: Serializer
        get() = LazyHolder.INSTANCE

    //======== Helper methods ========//

    /**
     * Creates and returns a [Serializer] using a json library available on the classpath.
     * @return the created serializer
     * @throws MissingJsonParserException if there are no supported json libraries available on the classpath
     */
    private fun create(): Serializer {
        val serializer: Serializer

        if (isPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            serializer = JacksonSerializer()
        } else if (isPresent("com.google.gson.Gson")) {
            serializer = GsonSerializer()
        } else if (isPresent("org.json.simple.JSONObject")) {
            serializer = JsonSimpleSerializer()
        } else if (isPresent("org.json.JSONObject")) {
            serializer = JsonSerializer()
        } else {
            throw MissingJsonParserException("unable to locate a JSON parser. " + "Please see <link> for more information")
        }

        logger.info("using json serializer: {}", serializer.javaClass.getSimpleName())
        return serializer
    }

    private fun isPresent(className: String): Boolean {
        try {
            Class.forName(className)
            return true
        } catch (e: ClassNotFoundException) {
            return false
        }

    }

    //======== Lazy-init Holder ========//

    public object LazyHolder {
        public val INSTANCE = create()
    }
}
