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

import javax.annotation.concurrent.Immutable

/**
 * Represents the Optimizely Attribute configuration.
 *
 * @see [Project JSON](http://developers.optimizely.com/server/reference/index.html.json)
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
class Attribute @JsonCreator
constructor(@param:JsonProperty("id") override val id: String,
            @param:JsonProperty("key") override val key: String,
            @param:JsonProperty("segmentId") val segmentId: String?) : IdKeyMapped {

    constructor(id: String, key: String) : this(id, key, null) {}

    override fun toString(): String {
        return "Attribute{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", segmentId='" + segmentId + '\'' +
                '}'
    }
}
