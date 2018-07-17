/**
 *
 * Copyright 2017, Optimizely and contributors
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

/**
 * Represents a FeatureFlag definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class FeatureFlag @JsonCreator
constructor(@param:JsonProperty("id") override val id: String,
            @param:JsonProperty("key") override val key: String,
            @param:JsonProperty("rolloutId") val rolloutId: String,
            @param:JsonProperty("experimentIds") val experimentIds: List<String>,
            @param:JsonProperty("variables") val variables: List<LiveVariable>) : IdKeyMapped {
    val variableKeyToLiveVariableMap: Map<String, LiveVariable>

    init {
        this.variableKeyToLiveVariableMap = ProjectConfigUtils.generateNameMapping(variables)
    }

    override fun toString(): String {
        return "FeatureFlag{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", rolloutId='" + rolloutId + '\'' +
                ", experimentIds=" + experimentIds +
                ", variables=" + variables +
                ", variableKeyToLiveVariableMap=" + variableKeyToLiveVariableMap +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as FeatureFlag?

        if (id != that!!.id) return false
        if (key != that.key) return false
        if (rolloutId != that.rolloutId) return false
        if (experimentIds != that.experimentIds) return false
        return if (variables != that.variables) false else variableKeyToLiveVariableMap == that.variableKeyToLiveVariableMap
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + rolloutId.hashCode()
        result = 31 * result + experimentIds.hashCode()
        result = 31 * result + variables.hashCode()
        result = 31 * result + variableKeyToLiveVariableMap.hashCode()
        return result
    }
}
