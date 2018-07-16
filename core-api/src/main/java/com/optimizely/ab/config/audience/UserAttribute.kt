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
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
class UserAttribute(val name: String, val type: String, val value: String?) : Condition {

    override fun evaluate(attributes: Map<String, String>): Boolean {
        val userAttributeValue = attributes[name]

        return if (value != null) { // if there is a value in the condition
            // check user attribute value is equal
            value == userAttributeValue
        } else if (userAttributeValue != null) { // if the datafile value is null but user has a value for this attribute
            // return false since null != nonnull
            false
        } else { // both are null
            true
        }
    }

    override fun toString(): String {
        return "{name='" + name + "\'" +
                ", type='" + type + "\'" +
                ", value='" + value + "\'" +
                "}"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as UserAttribute?

        if (name != that!!.name) return false
        if (type != that.type) return false
        return if (value != null) value == that.value else that.value == null
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}
