package com.optimizely.ab.internal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.Appender
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.ArgumentMatcher
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.verification.VerificationMode
import org.slf4j.LoggerFactory

import java.util.LinkedList

import org.mockito.Matchers.argThat
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.initMocks

/**
 * From http://techblog.kenshoo.com/2013/08/junit-rule-for-verifying-logback-logging.html
 */
class LogbackVerifier : TestRule {

    private val expectedEvents = LinkedList<ExpectedLogEvent>()

    @Mock
    private val appender: Appender<ILoggingEvent>? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                before()
                try {
                    base.evaluate()
                    verify()
                } finally {
                    after()
                }
            }
        }
    }

    @JvmOverloads
    fun expectMessage(level: Level, msg: String = "", throwableClass: Class<out Throwable>) {
        expectMessage(level, msg, null, times(1))
    }

    @JvmOverloads
    fun expectMessage(level: Level, msg: String = "") {
        expectMessage(level, msg, null, times(1))
    }

    fun expectMessage(level: Level, msg: String, times: VerificationMode) {
        expectMessage(level, msg, null, times)
    }

    fun expectMessage(level: Level,
                      msg: String,
                      throwableClass: Class<out Throwable>?,
                      times: VerificationMode) {
        expectedEvents.add(ExpectedLogEvent(level, msg, throwableClass, times))
    }

    private fun before() {
        initMocks(this)
        `when`(appender!!.name).thenReturn("MOCK")
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).addAppender(appender)
    }

    @Throws(Throwable::class)
    private fun verify() {
        for (expectedEvent in expectedEvents) {
            Mockito.verify<Appender<ILoggingEvent>>(appender, expectedEvent.times).doAppend(argThat(object : ArgumentMatcher<ILoggingEvent>() {
                override fun matches(argument: Any): Boolean {
                    return expectedEvent.matches(argument as ILoggingEvent)
                }
            }))
        }
    }

    private fun after() {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).detachAppender(appender)
    }

    private class ExpectedLogEvent constructor(private val level: Level,
                                                       private val message: String,
                                                       private val throwableClass: Class<out Throwable>?,
                                                       public val times: VerificationMode) {

        public fun matches(actual: ILoggingEvent): Boolean {
            var match = actual.formattedMessage.contains(message)
            match = match and (actual.level == level)
            match = match and matchThrowables(actual)
            return match
        }

        private fun matchThrowables(actual: ILoggingEvent): Boolean {
            val eventProxy = actual.throwableProxy
            return throwableClass == null || eventProxy != null && throwableClass.name == eventProxy.className
        }
    }
}
