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
package com.optimizely.ab.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

import java.util.Collections

/**
 * Represents the Optimizely Variation configuration.
 *
 * @see [Project JSON](http://developers.optimizely.com/server/reference/index.html.json)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Variation @JsonCreator
constructor(@param:JsonProperty("id") override val id: String,
            @param:JsonProperty("key") override val key: String,
            @JsonProperty("featureEnabled") featureEnabled: Boolean?,
            @JsonProperty("variables") liveVariableUsageInstances: List<LiveVariableUsageInstance>?) : IdKeyMapped {
    val featureEnabled: Boolean?
    val liveVariableUsageInstances: List<LiveVariableUsageInstance>?
    val variableIdToLiveVariableUsageInstanceMap: Map<String, LiveVariableUsageInstance>

    @JvmOverloads constructor(id: String,
                              key: String,
                              liveVariableUsageInstances: List<LiveVariableUsageInstance>? = null) : this(id, key, false, liveVariableUsageInstances) {
    }

    init {
        if (featureEnabled != null)
            this.featureEnabled = featureEnabled
        else
            this.featureEnabled = false
        if (liveVariableUsageInstances == null) {
            this.liveVariableUsageInstances = emptyList<LiveVariableUsageInstance>()
        } else {
            this.liveVariableUsageInstances = liveVariableUsageInstances
        }
        this.variableIdToLiveVariableUsageInstanceMap = ProjectConfigUtils.generateIdMapping(this.liveVariableUsageInstances)
    }

    fun `is`(otherKey: String): Boolean {
        return key == otherKey
    }

    override fun toString(): String {
        return "Variation{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                '}'
    }
}