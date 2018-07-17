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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Factory for generating [ConfigParser] instances, based on the json parser available on the classpath.
 */
object DefaultConfigParser {

    private val logger = LoggerFactory.getLogger(DefaultConfigParser::class.java!!)

    val instance: ConfigParser
        get() = LazyHolder.INSTANCE

    //======== Helper methods ========//

    /**
     * Creates and returns a [ConfigParser] using a json parser available on the classpath.
     * @return the created config parser
     * @throws MissingJsonParserException if there are no supported json parsers available on the classpath
     */
    private fun create(): ConfigParser {
        val configParser: ConfigParser

        if (isPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            configParser = JacksonConfigParser()
        } else if (isPresent("com.google.gson.Gson")) {
            configParser = GsonConfigParser()
        } else if (isPresent("org.json.simple.JSONObject")) {
            configParser = JsonSimpleConfigParser()
        } else if (isPresent("org.json.JSONObject")) {
            configParser = JsonConfigParser()
        } else {
            throw MissingJsonParserException("unable to locate a JSON parser. " + "Please see <link> for more information")
        }

        logger.info("using json parser: {}", configParser.javaClass.getSimpleName())
        return configParser
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

    private object LazyHolder {
        public val INSTANCE = create()
    }
}
