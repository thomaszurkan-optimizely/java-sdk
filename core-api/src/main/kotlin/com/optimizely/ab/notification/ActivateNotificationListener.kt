/**
 *
 * Copyright 2017, Optimizely and contributors
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

package com.optimizely.ab.notification

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Variation
import com.optimizely.ab.event.LogEvent


abstract class ActivateNotificationListener : NotificationListener, ActivateNotificationListenerInterface {

    /**
     * Base notify called with var args.  This method parses the parameters and calls the abstract method.
     * @param args - variable argument list based on the type of notification.
     */
    override fun notify(vararg args: Any) {
        assert(args[0] is Experiment)
        val experiment = args[0] as Experiment
        assert(args[1] is String)
        val userId = args[1] as String
        assert(args[2] is Map<*, *>)
        val attributes = args[2] as Map<String, String>
        assert(args[3] is Variation)
        val variation = args[3] as Variation
        assert(args[4] is LogEvent)
        val logEvent = args[4] as LogEvent

        onActivate(experiment, userId, attributes, variation, logEvent)
    }

    /**
     * onActivate called when an activate was triggered
     * @param experiment - The experiment object being activated.
     * @param userId - The userId passed into activate.
     * @param attributes - The filtered attribute list passed into activate
     * @param variation - The variation that was returned from activate.
     * @param event - The impression event that was triggered.
     */
    abstract override fun onActivate(experiment: Experiment,
                                     userId: String,
                                     attributes: Map<String, String>,
                                     variation: Variation,
                                     event: LogEvent)

}

