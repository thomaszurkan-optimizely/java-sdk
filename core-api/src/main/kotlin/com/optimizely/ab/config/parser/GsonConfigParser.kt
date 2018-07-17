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
import com.google.gson.GsonBuilder
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.audience.Audience

/**
 * [Gson]-based config parser implementation.
 */
internal class GsonConfigParser : ConfigParser {

    @Throws(ConfigParseException::class)
    override fun parseProjectConfig(json: String): ProjectConfig {
        if (json == null) {
            throw ConfigParseException("Unable to parse null json.")
        }
        if (json.length == 0) {
            throw ConfigParseException("Unable to parse empty json.")
        }
        val gson = GsonBuilder()
                .registerTypeAdapter(Audience::class.java, AudienceGsonDeserializer())
                .registerTypeAdapter(Experiment::class.java, ExperimentGsonDeserializer())
                .registerTypeAdapter(FeatureFlag::class.java, FeatureFlagGsonDeserializer())
                .registerTypeAdapter(Group::class.java, GroupGsonDeserializer())
                .registerTypeAdapter(ProjectConfig::class.java, ProjectConfigGsonDeserializer())
                .create()

        try {
            return gson.fromJson(json, ProjectConfig::class.java!!)
        } catch (e: Exception) {
            throw ConfigParseException("Unable to parse datafile: " + json, e)
        }

    }
}
