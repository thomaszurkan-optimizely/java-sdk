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
package com.optimizely.ab.event

import javax.annotation.concurrent.Immutable

/**
 * Represents Optimizely tracking and activation events.
 */
@Immutable
class LogEvent(
        //======== Getters ========//

        val requestMethod: RequestMethod,
        val endpointUrl: String,
        val requestParams: Map<String, String>,
        val body: String) {

    //======== Overriding method ========//

    override fun toString(): String {
        return "LogEvent{" +
                "requestMethod=" + requestMethod +
                ", endpointUrl='" + endpointUrl + '\'' +
                ", requestParams=" + requestParams +
                ", body='" + body + '\'' +
                '}'
    }

    //======== Helper classes ========//

    /**
     * The HTTP verb to use when dispatching the log event.
     */
    enum class RequestMethod {
        GET,
        POST
    }
}
