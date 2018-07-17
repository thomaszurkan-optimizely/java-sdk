/**
 *
 * Copyright 2018, Optimizely and contributors
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
package com.optimizely.ab.event.internal.payload

import com.fasterxml.jackson.annotation.JsonProperty

data class Event(
    var timestamp: Long = 0,
    var uuid: String,
    @JsonProperty("entity_id")
    var entityId: String? = null,
    var key: String? = null,
    var quantity: Number? = null,
    var revenue: Number? = null,
    var tags: Map<String, *>? = null,
    var type: String,
    var value: Number? = null
)
