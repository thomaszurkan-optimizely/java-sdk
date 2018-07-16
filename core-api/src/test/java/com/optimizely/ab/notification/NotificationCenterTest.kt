package com.optimizely.ab.notification

import ch.qos.logback.classic.Level
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Variation
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.internal.LogbackVerifier
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.mockito.Mockito.mock

class NotificationCenterTest {
    private var notificationCenter: NotificationCenter? = null
    private var activateNotification: ActivateNotificationListener? = null
    private var trackNotification: TrackNotificationListener? = null

    @Rule
    var logbackVerifier = LogbackVerifier()

    @Before
    fun initialize() {
        notificationCenter = NotificationCenter()
        activateNotification = mock<ActivateNotificationListener>(ActivateNotificationListener::class.java)
        trackNotification = mock<TrackNotificationListener>(TrackNotificationListener::class.java)
    }

    @Test
    fun testAddWrongTrackNotificationListener() {
        val notificationId = notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, trackNotification)
        logbackVerifier.expectMessage(Level.WARN, "Notification listener was the wrong type. It was not added to the notification center.")
        assertEquals(notificationId.toLong(), -1)
        assertFalse(notificationCenter!!.removeNotificationListener(notificationId))

    }

    @Test
    fun testAddWrongActivateNotificationListener() {
        val notificationId = notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Track, activateNotification)
        logbackVerifier.expectMessage(Level.WARN, "Notification listener was the wrong type. It was not added to the notification center.")
        assertEquals(notificationId.toLong(), -1)
        assertFalse(notificationCenter!!.removeNotificationListener(notificationId))
    }

    @Test
    fun testAddActivateNotificationTwice() {
        val listener = object : ActivateNotificationListener() {
            override fun onActivate(experiment: Experiment, userId: String, attributes: Map<String, String>, variation: Variation, event: LogEvent) {

            }
        }
        val notificationId = notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, listener)
        val notificationId2 = notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, listener)
        logbackVerifier.expectMessage(Level.WARN, "Notificication listener was already added")
        assertEquals(notificationId2.toLong(), -1)
        assertTrue(notificationCenter!!.removeNotificationListener(notificationId))
        notificationCenter!!.clearAllNotificationListeners()
    }

    @Test
    fun testAddActivateNotification() {
        val notificationId = notificationCenter!!.addActivateNotificationListener { experiment, userId, attributes, variation, event -> }
        assertNotSame(notificationId, -1)
        assertTrue(notificationCenter!!.removeNotificationListener(notificationId))
        notificationCenter!!.clearAllNotificationListeners()
    }

    @Test
    fun testAddTrackNotification() {
        val notificationId = notificationCenter!!.addTrackNotificationListener { eventKey, userId, attributes, eventTags, event -> }
        assertNotSame(notificationId, -1)
        assertTrue(notificationCenter!!.removeNotificationListener(notificationId))
        notificationCenter!!.clearAllNotificationListeners()
    }

    @Test
    fun testNotificationTypeClasses() {
        assertEquals(NotificationCenter.NotificationType.Activate.notificationTypeClass,
                ActivateNotificationListener::class.java)
        assertEquals(NotificationCenter.NotificationType.Track.notificationTypeClass, TrackNotificationListener::class.java)
    }

    @Test
    fun testAddTrackNotificationInterface() {
        val notificationId = notificationCenter!!.addTrackNotificationListener { eventKey, userId, attributes, eventTags, event -> }
        assertNotSame(notificationId, -1)
        assertTrue(notificationCenter!!.removeNotificationListener(notificationId))
        notificationCenter!!.clearAllNotificationListeners()
    }

    @Test
    fun testAddActivateNotificationInterface() {
        val notificationId = notificationCenter!!.addActivateNotificationListener { experiment, userId, attributes, variation, event -> }
        assertNotSame(notificationId, -1)
        assertTrue(notificationCenter!!.removeNotificationListener(notificationId))
        notificationCenter!!.clearAllNotificationListeners()
    }

}
