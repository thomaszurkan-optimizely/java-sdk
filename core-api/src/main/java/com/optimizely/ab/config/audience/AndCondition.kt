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

import javax.annotation.concurrent.Immutable

/**
 * Represents an 'And' conditions condition operation.
 */
@Immutable
class AndCondition(val conditions: List<Condition>) : Condition {

    override fun evaluate(attributes: Map<String, String>): Boolean {
        for (condition in conditions) {
            if (!condition.evaluate(attributes))
                return false
        }

        return true
    }

    override fun toString(): String {
        val s = StringBuilder()
        s.append("[and, ")
        for (i in conditions.indices) {
            s.append(conditions[i])
            if (i < conditions.size - 1)
                s.append(", ")
        }
        s.append("]")

        return s.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AndCondition)
            return false

        val otherAndCondition = other as AndCondition?

        return conditions == otherAndCondition!!.conditions
    }

    override fun hashCode(): Int {
        return conditions.hashCode()
    }
}

