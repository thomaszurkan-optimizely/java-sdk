/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.bucketing

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Variation

class FeatureDecision
/**
 * Initialize a FeatureDecision object.
 * @param experiment The [Experiment] the Feature is associated with.
 * @param variation The [Variation] the user was bucketed into.
 * @param decisionSource The source of the variation.
 */
(
        /** The [Experiment] the Feature is associated with.  */
        var experiment: Experiment?,
        /** The [Variation] the user was bucketed into.  */
        var variation: Variation?,
        /** The source of the [Variation].  */
        var decisionSource: DecisionSource?) {

    enum class DecisionSource {
        EXPERIMENT,
        ROLLOUT
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as FeatureDecision?

        return if (if (variation != null) variation != that!!.variation else that!!.variation != null) false else decisionSource == that.decisionSource
    }

    override fun hashCode(): Int {
        var result = if (variation != null) variation!!.hashCode() else 0
        result = 31 * result + if (decisionSource != null) decisionSource!!.hashCode() else 0
        return result
    }
}
