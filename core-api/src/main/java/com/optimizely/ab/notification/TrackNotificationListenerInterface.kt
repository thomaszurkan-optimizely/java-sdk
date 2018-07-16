package com.optimizely.ab.notification

import com.optimizely.ab.event.LogEvent

interface TrackNotificationListenerInterface {
    /**
     * onTrack is called when a track event is triggered
     * @param eventKey - The event key that was triggered.
     * @param userId - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param eventTags - event tags if any were passed in.
     * @param event - The event being recorded.
     */
    fun onTrack(eventKey: String,
                userId: String,
                attributes: Map<String, String>,
                eventTags: Map<String, *>,
                event: LogEvent)

}
