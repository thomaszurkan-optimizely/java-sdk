/**
 *
 * Copyright 2017-2018, Optimizely and contributors
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
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.HashMap


/**
 * This class handles impression and conversion notificationsListeners. It replaces NotificationBroadcaster and is intended to be
 * more flexible.
 */
class NotificationCenter {
    /**
     * NotificationType is used for the notification types supported.
     */
    enum class NotificationType private constructor(// Track was called.  Track a conversion event

            val notificationTypeClass: Class<*>) {

        Activate(ActivateNotificationListener::class.java), // Activate was called. Track an impression event
        Track(TrackNotificationListener::class.java)
    }


    // the notification id is incremented and is assigned as the callback id, it can then be used to remove the notification.
    private var notificationListenerID = 1

    // notification holder holds the id as well as the notification.
    private class NotificationHolder internal constructor(internal var notificationId: Int, internal var notificationListener: NotificationListener)

    // private list of notification by notification type.
    // we used a list so that notification order can mean something.
    private val notificationsListeners = HashMap<NotificationType, ArrayList<NotificationHolder>>()

    /**
     * Instantiate a new NotificationCenter
     */
    init {
        notificationsListeners.put(NotificationType.Activate, ArrayList())
        notificationsListeners.put(NotificationType.Track, ArrayList())
    }

    /**
     * Convenience method to support lambdas as callbacks in later version of Java (8+).
     * @param activateNotificationListenerInterface
     * @return greater than zero if added.
     */
    fun addActivateNotificationListener(activateNotificationListenerInterface: (Any, Any, Any, Any, Any) -> Unit): Int {
        return if (activateNotificationListenerInterface is ActivateNotificationListener) {
            addNotificationListener(NotificationType.Activate, activateNotificationListenerInterface as NotificationListener)
        } else {
            addNotificationListener(NotificationType.Activate, object : ActivateNotificationListener() {
                override fun onActivate(experiment: Experiment, userId: String, attributes: Map<String, String>, variation: Variation, event: LogEvent) {
                    activateNotificationListenerInterface(experiment, userId, attributes, variation, event)
                }
            })
        }
    }

    /**
     * Convenience method to support lambdas as callbacks in later versions of Java (8+)
     * @param trackNotificationListenerInterface
     * @return greater than zero if added.
     */
    fun addTrackNotificationListener(trackNotificationListenerInterface: (Any, Any, Any, Any, Any) -> Unit): Int {
        return if (trackNotificationListenerInterface is TrackNotificationListener) {
            addNotificationListener(NotificationType.Activate, trackNotificationListenerInterface as NotificationListener)
        } else {
            addNotificationListener(NotificationType.Track, object : TrackNotificationListener() {
                override fun onTrack(eventKey: String, userId: String, attributes: Map<String, String>, eventTags: Map<String, *>, event: LogEvent) {
                    trackNotificationListenerInterface(eventKey, userId, attributes, eventTags, event)
                }
            })
        }
    }

    /**
     * Add a notification listener to the notification center.
     *
     * @param notificationType - enum NotificationType to add.
     * @param notificationListener - Notification to add.
     * @return the notification id used to remove the notification.  It is greater than 0 on success.
     */
    fun addNotificationListener(notificationType: NotificationType, notificationListener: NotificationListener): Int {

        val clazz = notificationType.notificationTypeClass
        if (clazz == null || !clazz.isInstance(notificationListener)) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.")
            return -1
        }

        notificationsListeners[notificationType]?.iterator()?.forEach { holder ->
            if (holder.notificationListener === notificationListener) {
                logger.warn("Notificication listener was already added")
                return -1
            }
        }
        val id = notificationListenerID++
        notificationsListeners[notificationType]?.add(NotificationHolder(id, notificationListener))
        logger.info("Notification listener {} was added with id {}", notificationListener.toString(), id)
        return id
    }

    /**
     * Remove the notification listener based on the notificationId passed back from addNotificationListener.
     * @param notificationID the id passed back from add notification.
     * @return true if removed otherwise false (if the notification is already registered, it returns false).
     */
    fun removeNotificationListener(notificationID: Int): Boolean {
        for (type in NotificationType.values()) {
            notificationsListeners[type]?.iterator()?.forEach { holder ->
                if (holder.notificationId == notificationID) {
                    notificationsListeners[type]?.remove(holder)
                    logger.info("Notification listener removed {}", notificationID)
                    return true
                }
            }
        }

        logger.warn("Notification listener with id {} not found", notificationID)

        return false
    }

    /**
     * Clear out all the notification listeners.
     */
    fun clearAllNotificationListeners() {
        for (type in NotificationType.values()) {
            clearNotificationListeners(type)
        }
    }

    /**
     * Clear notification listeners by notification type.
     * @param notificationType type of notificationsListeners to remove.
     */
    fun clearNotificationListeners(notificationType: NotificationType) {
        notificationsListeners[notificationType]?.clear()
    }

    // fire a notificaiton of a certain type.  The arg list changes depending on the type of notification sent.
    fun sendNotifications(notificationType: NotificationType, vararg args: Any) {
        val holders = notificationsListeners[notificationType]
        holders?.iterator()?.forEach { holder ->
            try {
                holder.notificationListener.notify(*args)
            } catch (e: Exception) {
                logger.error("Unexpected exception calling notification listener {}", holder.notificationId, e)
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(NotificationCenter::class.java!!)
    }

}
