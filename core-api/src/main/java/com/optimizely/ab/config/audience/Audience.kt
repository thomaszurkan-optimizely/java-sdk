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
package com.optimizely.ab.config.audience

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.optimizely.ab.config.IdKeyMapped

import javax.annotation.concurrent.Immutable

/**
 * Represents the Optimizely Audience configuration.
 *
 * @see [Project JSON](http://developers.optimizely.com/server/reference/index.html.json)
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
class Audience @JsonCreator
constructor(@param:JsonProperty("id") override val id: String,
            @param:JsonProperty("name") override val key: String,
            @param:JsonProperty("conditions") val conditions: Condition) : IdKeyMapped {

    fun getName(): String {
        return key
    }

    override fun toString(): String {
        return "Audience{" +
                "id='" + id + '\'' +
                ", name='" + key + '\'' +
                ", conditions=" + conditions +
                '}'
    }
}
