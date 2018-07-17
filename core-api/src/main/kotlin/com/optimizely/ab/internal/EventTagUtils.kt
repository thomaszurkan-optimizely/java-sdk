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
package com.optimizely.ab.internal

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EventTagUtils {

    private val logger = LoggerFactory.getLogger(EventTagUtils::class.java!!)

    /**
     * Grab the revenue value from the event tags. "revenue" is a reserved keyword.
     * @param eventTags
     * @return Long
     */
    fun getRevenueValue(eventTags: Map<String, *>): Long? {
        var eventValue: Long? = null
        if (eventTags.containsKey(ReservedEventKey.REVENUE.toString())) {
            val rawValue = eventTags[ReservedEventKey.REVENUE.toString()]
            if (Long::class.java!!.isInstance(rawValue)) {
                eventValue = rawValue as Long
                logger.info("Parsed revenue value \"{}\" from event tags.", eventValue)
            } else if (Int::class.java!!.isInstance(rawValue)) {
                eventValue = (rawValue as Int).toLong()
                logger.info("Parsed revenue value \"{}\" from event tags.", eventValue)
            } else {
                logger.warn("Failed to parse revenue value \"{}\" from event tags.", rawValue)
            }
        }
        return eventValue
    }

    /**
     * Fetch the numeric metric value from event tags. "value" is a reserved keyword.
     */
    fun getNumericValue(eventTags: Map<String, *>): Double? {
        var eventValue: Double? = null
        if (eventTags.containsKey(ReservedEventKey.VALUE.toString())) {
            val rawValue = eventTags[ReservedEventKey.VALUE.toString()]
            if (rawValue is Number) {
                eventValue = rawValue.toDouble()
                logger.info("Parsed numeric metric value \"{}\" from event tags.", eventValue)
            } else {
                logger.warn("Failed to parse numeric metric value \"{}\" from event tags.", rawValue)
            }
        }

        return eventValue
    }
}
