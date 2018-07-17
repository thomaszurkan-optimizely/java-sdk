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

import java.util.Collections
import javax.annotation.concurrent.Immutable

/**
 * Represents the Optimizely Experiment configuration.
 *
 * @see [Project JSON](http://developers.optimizely.com/server/reference/index.html.json)
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
class Experiment(override val id: String,
                 override val key: String,
                 status: String?,
                 val layerId: String?,
                 audienceIds: List<String>,
                 variations: List<Variation>,
                 val userIdToVariationKeyMap: Map<String, String>,
                 trafficAllocation: List<TrafficAllocation>,
                 val groupId: String) : IdKeyMapped {
    val status: String

    val audienceIds: List<String>
    val variations: List<Variation>
    val trafficAllocation: List<TrafficAllocation>

    val variationKeyToVariationMap: Map<String, Variation>
    val variationIdToVariationMap: Map<String, Variation>

    enum class ExperimentStatus private constructor(private val experimentStatus: String) {
        RUNNING("Running"),
        LAUNCHED("Launched"),
        PAUSED("Paused"),
        NOT_STARTED("Not started"),
        ARCHIVED("Archived");

        override fun toString(): String {
            return experimentStatus
        }
    }

    @JsonCreator
    constructor(@JsonProperty("id") id: String,
                @JsonProperty("key") key: String,
                @JsonProperty("status") status: String,
                @JsonProperty("layerId") layerId: String,
                @JsonProperty("audienceIds") audienceIds: List<String>,
                @JsonProperty("variations") variations: List<Variation>,
                @JsonProperty("forcedVariations") userIdToVariationKeyMap: Map<String, String>,
                @JsonProperty("trafficAllocation") trafficAllocation: List<TrafficAllocation>) : this(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap, trafficAllocation, "") {
    }

    init {
        this.status = status ?: ExperimentStatus.NOT_STARTED.toString()
        this.audienceIds = Collections.unmodifiableList(audienceIds)
        this.variations = Collections.unmodifiableList(variations)
        this.trafficAllocation = Collections.unmodifiableList(trafficAllocation)
        this.variationKeyToVariationMap = ProjectConfigUtils.generateNameMapping(variations)
        this.variationIdToVariationMap = ProjectConfigUtils.generateIdMapping(variations)
    }

    val isActive: Boolean
        get() = status == ExperimentStatus.RUNNING.toString() || status == ExperimentStatus.LAUNCHED.toString()

    val isRunning: Boolean
        get() = status == ExperimentStatus.RUNNING.toString()

    val isLaunched: Boolean
        get() = status == ExperimentStatus.LAUNCHED.toString()

    override fun toString(): String {
        return "Experiment{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", groupId='" + groupId + '\'' +
                ", status='" + status + '\'' +
                ", audienceIds=" + audienceIds +
                ", variations=" + variations +
                ", variationKeyToVariationMap=" + variationKeyToVariationMap +
                ", userIdToVariationKeyMap=" + userIdToVariationKeyMap +
                ", trafficAllocation=" + trafficAllocation +
                '}'
    }
}
