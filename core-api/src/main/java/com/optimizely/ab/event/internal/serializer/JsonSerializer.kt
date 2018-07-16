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
package com.optimizely.ab.event.internal.serializer

import org.json.JSONObject

internal class JsonSerializer : Serializer {

    override fun <T> serialize(payload: T): String {
        val payloadJsonObject = JSONObject(payload)
        val jsonResponse = payloadJsonObject.toString()
        val stringBuilder = StringBuilder()

        for (i in 0..jsonResponse.length - 1) {
            val ch = jsonResponse[i]
            var nextChar: Char? = null
            if (i + 1 < jsonResponse.length) {
                nextChar = jsonResponse[i + 1]
            }
            if ((Character.isLetter(ch) || Character.isDigit(ch)) && Character.isUpperCase(ch) &&
                    (Character.isLetter(nextChar!!) || Character.isDigit(nextChar))) {
                stringBuilder.append('_')
                stringBuilder.append(Character.toLowerCase(ch))
            } else {
                stringBuilder.append(ch)
            }
        }

        return stringBuilder.toString()
    }
}
