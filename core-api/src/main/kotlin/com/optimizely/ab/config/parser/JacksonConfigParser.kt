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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

import com.optimizely.ab.config.ProjectConfig

/**
 * `Jackson`-based config parser implementation.
 */
internal class JacksonConfigParser : ConfigParser {

    @Throws(ConfigParseException::class)
    override fun parseProjectConfig(json: String): ProjectConfig {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addDeserializer(ProjectConfig::class.java, ProjectConfigJacksonDeserializer())
        mapper.registerModule(module)

        try {
            return mapper.readValue<ProjectConfig>(json, ProjectConfig::class.java!!)
        } catch (e: Exception) {
            throw ConfigParseException("Unable to parse datafile: " + json, e)
        }

    }
}
