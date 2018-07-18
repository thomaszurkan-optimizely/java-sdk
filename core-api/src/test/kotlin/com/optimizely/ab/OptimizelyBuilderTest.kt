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
package com.optimizely.ab

import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.ProjectConfigTestUtils
import com.optimizely.ab.config.parser.ConfigParseException
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.error.NoOpErrorHandler
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.internal.BuildVersionInfo
import com.optimizely.ab.event.internal.EventBuilder
import com.optimizely.ab.event.internal.payload.EventBatch.ClientEngine
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV2
import com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3
import com.optimizely.ab.event.internal.payload.EventBatch
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.mockito.Mockito.mock

/**
 * Tests for [Optimizely.builder].
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
class OptimizelyBuilderTest {

    @get:Rule
    public var thrown = ExpectedException.none()

    @get:Rule
    public var rule = MockitoJUnit.rule()

    @Mock private val mockEventHandler: EventHandler? = null

    @Mock private val mockErrorHandler: ErrorHandler? = null

    @Test
    @Throws(Exception::class)
    fun withEventHandler() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .build()

        assertThat(optimizelyClient.eventHandler, `is`(mockEventHandler))
    }

    @Test
    @Throws(Exception::class)
    fun projectConfigV2() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .build()

        ProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.projectConfig, validProjectConfigV2())
    }

    @Test
    @Throws(Exception::class)
    fun projectConfigV3() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV3(), mockEventHandler!!)
                .build()

        ProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.projectConfig, validProjectConfigV3())
    }

    @Test
    @Throws(Exception::class)
    fun withErrorHandler() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .withErrorHandler(mockErrorHandler!!)
                .build()

        assertThat(optimizelyClient.errorHandler, `is`<ErrorHandler>(mockErrorHandler))
    }

    @Test
    @Throws(Exception::class)
    fun withDefaultErrorHandler() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .build()

        assertThat(optimizelyClient.errorHandler, instanceOf<Any>(NoOpErrorHandler::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun withUserProfileService() {
        val userProfileService = mock<UserProfileService>(UserProfileService::class.java)
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .withUserProfileService(userProfileService)
                .build()

        assertThat<UserProfileService>(optimizelyClient.userProfileService, `is`(userProfileService))
    }

    @Test
    @Throws(Exception::class)
    fun withDefaultClientEngine() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .build()

        assertThat<EventBatch.ClientEngine>((optimizelyClient.eventBuilder as EventBuilder).clientEngine, `is`(ClientEngine.JAVA_SDK))
    }

    @Test
    @Throws(Exception::class)
    fun withAndroidSDKClientEngine() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .withClientEngine(ClientEngine.ANDROID_SDK)
                .build()

        assertThat<EventBatch.ClientEngine>((optimizelyClient.eventBuilder as EventBuilder).clientEngine, `is`(ClientEngine.ANDROID_SDK))
    }

    @Test
    @Throws(Exception::class)
    fun withAndroidTVSDKClientEngine() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .withClientEngine(ClientEngine.ANDROID_TV_SDK)
                .build()

        assertThat<EventBatch.ClientEngine>((optimizelyClient.eventBuilder as EventBuilder).clientEngine, `is`(ClientEngine.ANDROID_TV_SDK))
    }

    @Test
    @Throws(Exception::class)
    fun withDefaultClientVersion() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .build()

        assertThat((optimizelyClient.eventBuilder as EventBuilder).clientVersion, `is`(BuildVersionInfo.VERSION))
    }

    @Test
    @Throws(Exception::class)
    fun withCustomClientVersion() {
        val optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler!!)
                .withClientVersion("0.0.0")
                .build()

        assertThat((optimizelyClient.eventBuilder as EventBuilder).clientVersion, `is`("0.0.0"))
    }

    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Testing nullness contract violation")
    @Test
    @Throws(Exception::class)
    fun builderThrowsConfigParseExceptionForNullDatafile() {
        thrown.expect(ConfigParseException::class.java)
        Optimizely.builder("", mockEventHandler!!).build()
    }

    @Test
    @Throws(Exception::class)
    fun builderThrowsConfigParseExceptionForEmptyDatafile() {
        thrown.expect(ConfigParseException::class.java)
        Optimizely.builder("", mockEventHandler!!).build()
    }

    @Test
    @Throws(Exception::class)
    fun builderThrowsConfigParseExceptionForInvalidDatafile() {
        thrown.expect(ConfigParseException::class.java)
        Optimizely.builder("{invalidDatafile}", mockEventHandler!!).build()
    }

    companion object {


        private val userId = "userId"
        private var noAudienceDatafile: String? = null
        private var noAudienceProjectConfig: ProjectConfig? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setUp() {
            noAudienceDatafile = noAudienceProjectConfigJsonV2()
            noAudienceProjectConfig = noAudienceProjectConfigV2()
        }
    }
}
