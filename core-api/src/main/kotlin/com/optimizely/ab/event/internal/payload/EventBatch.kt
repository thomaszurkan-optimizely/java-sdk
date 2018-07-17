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
import com.fasterxml.jackson.annotation.JsonValue
import com.optimizely.ab.event.internal.BuildVersionInfo

class EventBatch(
        @JsonProperty("account_id")
        var accountId: String,
        var visitors: List<Visitor>,
        @JsonProperty("anonymize_ip")
        var anonymizeIp: Boolean? = null,
        @JsonProperty("client_name")
        var clientName: String? = ClientEngine.JAVA_SDK.clientEngineValue,
        @JsonProperty("client_version")
        var clientVersion: String? = BuildVersionInfo.VERSION,
        @JsonProperty("project_id")
        var projectId: String? = null,
        var revision: String? = null
) {
    enum class ClientEngine private constructor(val clientEngineValue: String) {
        JAVA_SDK("java-sdk"),
        ANDROID_SDK("android-sdk"),
        ANDROID_TV_SDK("android-tv-sdk")
    }

}