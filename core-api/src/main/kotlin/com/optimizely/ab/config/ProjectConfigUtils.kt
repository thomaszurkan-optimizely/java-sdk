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

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

object ProjectConfigUtils {

    /**
     * Helper method for creating convenience mappings from key to entity
     */
    fun <T : IdKeyMapped> generateNameMapping(nameables: List<T>): Map<String, T> {
        val nameMapping = HashMap<String, T>()
        for (nameable in nameables) {
            nameMapping.put(nameable.key, nameable)
        }

        return Collections.unmodifiableMap(nameMapping)
    }

    /**
     * Helper method for creating convenience mappings from ID to entity
     */
    fun <T : IdMapped> generateIdMapping(nameables: List<T>): Map<String, T> {
        val nameMapping = HashMap<String, T>()
        for (nameable in nameables) {
            nameMapping.put(nameable.id, nameable)
        }

        return Collections.unmodifiableMap(nameMapping)
    }

    /**
     * Helper method to create a map from a live variable to all the experiments using it
     */
    fun generateLiveVariableIdToExperimentsMapping(
            experiments: List<Experiment>): Map<String, List<Experiment>> {

        val variableIdToExperiments = HashMap<String, List<Experiment>>()
        for (experiment in experiments) {
            if (!experiment.variations.isEmpty()) {
                // if a live variable is used by an experiment, it will have instances in all variations so we can
                // short-circuit after getting the live variables for the first variation
                val variation = experiment.variations[0]
                if (variation.liveVariableUsageInstances != null) {
                    for (usageInstance in variation.liveVariableUsageInstances) {

                        var experimentsUsingVariable: MutableList<Experiment>? = null;

                        if (variableIdToExperiments[usageInstance.id] != null) {
                            experimentsUsingVariable = ArrayList(variableIdToExperiments[usageInstance.id])
                        }
                        if (experimentsUsingVariable == null) {
                            experimentsUsingVariable = ArrayList()
                        }

                        experimentsUsingVariable.add(experiment)
                        variableIdToExperiments.put(usageInstance.id, experimentsUsingVariable)
                    }
                }
            }
        }

        return variableIdToExperiments
    }

    /**
     * Helper method to create a map from variation ID to variable ID to [LiveVariableUsageInstance]
     */
    fun generateVariationToLiveVariableUsageInstancesMap(
            experiments: List<Experiment>): Map<String, Map<String, LiveVariableUsageInstance>> {

        val liveVariableValueMap = HashMap<String, Map<String, LiveVariableUsageInstance>>()
        for (experiment in experiments) {
            for (variation in experiment.variations) {
                if (variation.liveVariableUsageInstances != null) {
                    for (usageInstance in variation.liveVariableUsageInstances) {
                        var liveVariableIdToValueMap: MutableMap<String, LiveVariableUsageInstance>? = HashMap(liveVariableValueMap[variation.id])
                        if (liveVariableIdToValueMap == null) {
                            liveVariableIdToValueMap = HashMap()
                        }

                        liveVariableIdToValueMap.put(usageInstance.id, usageInstance)
                        liveVariableValueMap.put(variation.id, liveVariableIdToValueMap)
                    }
                }
            }
        }

        return liveVariableValueMap
    }
}
