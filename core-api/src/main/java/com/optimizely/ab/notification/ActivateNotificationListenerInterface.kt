package com.optimizely.ab.notification

import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Variation
import com.optimizely.ab.event.LogEvent

interface ActivateNotificationListenerInterface {
    /**
     * onActivate called when an activate was triggered
     * @param experiment - The experiment object being activated.
     * @param userId - The userId passed into activate.
     * @param attributes - The filtered attribute list passed into activate
     * @param variation - The variation that was returned from activate.
     * @param event - The impression event that was triggered.
     */
    fun onActivate(experiment: Experiment,
                   userId: String,
                   attributes: Map<String, String>,
                   variation: Variation,
                   event: LogEvent)

}
