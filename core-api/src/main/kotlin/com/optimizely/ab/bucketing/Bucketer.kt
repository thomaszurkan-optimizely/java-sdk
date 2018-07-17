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
package com.optimizely.ab.bucketing

import com.optimizely.ab.annotations.VisibleForTesting
import com.optimizely.ab.bucketing.internal.MurmurHash3
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Group
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.concurrent.Immutable

/**
 * Default Optimizely bucketing algorithm that evenly distributes users using the Murmur3 hash of some provided
 * identifier.
 *
 *
 * The user identifier *must* be provided in the first data argument passed to
 * [.bucket] and *must* be non-null and non-empty.
 *
 * @see [MurmurHash](https://en.wikipedia.org/wiki/MurmurHash)
 */
@Immutable
open class Bucketer(private val projectConfig: ProjectConfig) {

    private fun bucketToEntity(bucketValue: Int, trafficAllocations: List<TrafficAllocation>): String? {
        var currentEndOfRange: Int
        for (currAllocation in trafficAllocations) {
            currentEndOfRange = currAllocation.endOfRange
            if (bucketValue < currentEndOfRange) {
                // for mutually exclusive bucketing, de-allocated space is represented by an empty string
                return if (currAllocation.entityId.isEmpty()) {
                    null
                } else currAllocation.entityId
            }
        }

        return null
    }

    private fun bucketToExperiment(group: Group,
                                   bucketingId: String): Experiment? {
        // "salt" the bucket id using the group id
        val bucketKey = bucketingId + group.id

        val trafficAllocations = group.trafficAllocation

        val hashCode = MurmurHash3.murmurhash3_x86_32(bucketKey, 0, bucketKey.length, MURMUR_HASH_SEED)
        val bucketValue = generateBucketValue(hashCode)
        logger.debug("Assigned bucket {} to user with bucketingId \"{}\" during experiment bucketing.", bucketValue, bucketingId)

        val bucketedExperimentId = bucketToEntity(bucketValue, trafficAllocations)
        return if (bucketedExperimentId != null) {
            projectConfig.experimentIdMapping[bucketedExperimentId]
        } else null

        // user was not bucketed to an experiment in the group
    }

    private fun bucketToVariation(experiment: Experiment,
                                  bucketingId: String): Variation? {
        // "salt" the bucket id using the experiment id
        val experimentId = experiment.id
        val experimentKey = experiment.key
        val combinedBucketId = bucketingId + experimentId

        val trafficAllocations = experiment.trafficAllocation

        val hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length, MURMUR_HASH_SEED)
        val bucketValue = generateBucketValue(hashCode)
        logger.debug("Assigned bucket {} to user with bucketingId \"{}\" when bucketing to a variation.", bucketValue, bucketingId)

        val bucketedVariationId = bucketToEntity(bucketValue, trafficAllocations)
        if (bucketedVariationId != null) {
            val bucketedVariation = experiment.variationIdToVariationMap[bucketedVariationId]
            val variationKey = bucketedVariation?.key
            logger.info("User with bucketingId \"{}\" is in variation \"{}\" of experiment \"{}\".", bucketingId, variationKey,
                    experimentKey)

            return bucketedVariation
        }

        // user was not bucketed to a variation
        logger.info("User with bucketingId \"{}\" is not in any variation of experiment \"{}\".", bucketingId, experimentKey)
        return null
    }

    /**
     * Assign a [Variation] of an [Experiment] to a user based on hashed value from murmurhash3.
     * @param experiment The Experiment in which the user is to be bucketed.
     * @param bucketingId string A customer-assigned value used to create the key for the murmur hash.
     * @return Variation the user is bucketed into or null.
     */
    fun bucket(experiment: Experiment,
               bucketingId: String): Variation? {
        // ---------- Bucket User ----------
        val groupId = experiment.groupId
        // check whether the experiment belongs to a group
        if (!groupId.isEmpty()) {
            val experimentGroup = projectConfig.groupIdMapping[groupId]
            // bucket to an experiment only if group entities are to be mutually exclusive
            if (experimentGroup?.policy == Group.RANDOM_POLICY) {
                val bucketedExperiment = bucketToExperiment(experimentGroup, bucketingId)
                if (bucketedExperiment == null) {
                    logger.info("User with bucketingId \"{}\" is not in any experiment of group {}.", bucketingId, experimentGroup.id)
                    return null
                } else {

                }
                // if the experiment a user is bucketed in within a group isn't the same as the experiment provided,
                // don't perform further bucketing within the experiment
                if (bucketedExperiment.id != experiment.id) {
                    logger.info("User with bucketingId \"{}\" is not in experiment \"{}\" of group {}.", bucketingId, experiment.key,
                            experimentGroup.id)
                    return null
                }

                logger.info("User with bucketingId \"{}\" is in experiment \"{}\" of group {}.", bucketingId, experiment.key,
                        experimentGroup.id)
            }
        }

        return bucketToVariation(experiment, bucketingId)
    }


    //======== Helper methods ========//

    /**
     * Map the given 32-bit hashcode into the range [0, [.MAX_TRAFFIC_VALUE]).
     * @param hashCode the provided hashcode
     * @return a value in the range closed-open range, [0, [.MAX_TRAFFIC_VALUE])
     */
    @VisibleForTesting
    internal open fun generateBucketValue(hashCode: Int): Int {
        // map the hashCode into the range [0, BucketAlgorithm.MAX_TRAFFIC_VALUE)
        val ratio = (hashCode.toLong() and 0xFFFFFFFFL as Long).toDouble() / Math.pow(2.0, 32.0)
        return Math.floor(MAX_TRAFFIC_VALUE * ratio).toInt()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Bucketer::class.java!!)

        private val MURMUR_HASH_SEED = 1

        /**
         * The maximum bucket value (represents 100 Basis Points).
         */
        @VisibleForTesting
        internal val MAX_TRAFFIC_VALUE = 10000
    }


}
