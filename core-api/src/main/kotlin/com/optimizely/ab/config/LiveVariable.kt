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
import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName

/**
 * Represents a live variable definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LiveVariable @JsonCreator
constructor(@param:JsonProperty("id") override val id: String,
            @param:JsonProperty("key") override val key: String,
            @param:JsonProperty("defaultValue") val defaultValue: String,
            @param:JsonProperty("status") val status: VariableStatus?,
            @param:JsonProperty("type") val type: VariableType) : IdKeyMapped {

    enum class VariableStatus private constructor(val variableStatus: String) {
        @SerializedName("active")
        ACTIVE("active"),

        @SerializedName("archived")
        ARCHIVED("archived");


        companion object {

            fun fromString(variableStatusString: String?): VariableStatus? {
                if (variableStatusString != null) {
                    for (variableStatusEnum in VariableStatus.values()) {
                        if (variableStatusString == variableStatusEnum.variableStatus) {
                            return variableStatusEnum
                        }
                    }
                }

                return null
            }
        }
    }

    enum class VariableType private constructor(val variableType: String) {
        @SerializedName("boolean")
        BOOLEAN("boolean"),

        @SerializedName("integer")
        INTEGER("integer"),

        @SerializedName("string")
        STRING("string"),

        @SerializedName("double")
        DOUBLE("double");


        companion object {

            fun fromString(variableTypeString: String?): VariableType? {
                if (variableTypeString != null) {
                    for (variableTypeEnum in VariableType.values()) {
                        if (variableTypeString == variableTypeEnum.variableType) {
                            return variableTypeEnum
                        }
                    }
                }

                return null
            }
        }
    }

    override fun toString(): String {
        return "LiveVariable{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val variable = o as LiveVariable?

        if (id != variable!!.id) return false
        if (key != variable.key) return false
        if (defaultValue != variable.defaultValue) return false
        return if (type != variable.type) false else status == variable.status
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + defaultValue.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + status!!.hashCode()
        return result
    }
}
