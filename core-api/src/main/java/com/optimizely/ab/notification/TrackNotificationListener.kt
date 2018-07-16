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

import com.optimizely.ab.event.LogEvent

/**
 * This class handles the track event notification.
 */
abstract class TrackNotificationListener : NotificationListener, TrackNotificationListenerInterface {
    /**
     * Base notify called with var args.  This method parses the parameters and calls the abstract method.
     * @param args - variable argument list based on the type of notification.
     */
    override fun notify(vararg args: Any) {
        assert(args[0] is String)
        val eventKey = args[0] as String
        assert(args[1] is String)
        val userId = args[1] as String
        assert(args[2] is Map<*, *>)
        val attributes = args[2] as Map<String, String>
        assert(args[3] is Map<*, *>)
        val eventTags = args[3] as Map<String, *>
        assert(args[4] is LogEvent)
        val logEvent = args[4] as LogEvent

        onTrack(eventKey, userId, attributes, eventTags, logEvent)
    }

    /**
     * onTrack is called when a track event is triggered
     * @param eventKey - The event key that was triggered.
     * @param userId - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param eventTags - event tags if any were passed in.
     * @param event - The event being recorded.
     */
    abstract override fun onTrack(eventKey: String,
                                  userId: String,
                                  attributes: Map<String, String>,
                                  eventTags: Map<String, *>,
                                  event: LogEvent)
}
