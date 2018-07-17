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
package com.optimizely.ab.config.parser

import com.optimizely.ab.config.ProjectConfig

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3
import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4
import com.optimizely.ab.config.ProjectConfigTestUtils.verifyProjectConfig

/**
 * Tests for [JsonConfigParser].
 */
class JsonConfigParserTest {

    @Rule
    var thrown = ExpectedException.none()

    @Test
    @Throws(Exception::class)
    fun parseProjectConfigV2() {
        val parser = JsonConfigParser()
        val actual = parser.parseProjectConfig(validConfigJsonV2())
        val expected = validProjectConfigV2()

        verifyProjectConfig(actual, expected)
    }

    @Test
    @Throws(Exception::class)
    fun parseProjectConfigV3() {
        val parser = JsonConfigParser()
        val actual = parser.parseProjectConfig(validConfigJsonV3())
        val expected = validProjectConfigV3()

        verifyProjectConfig(actual, expected)
    }

    @Test
    @Throws(Exception::class)
    fun parseProjectConfigV4() {
        val parser = JsonConfigParser()
        val actual = parser.parseProjectConfig(validConfigJsonV4())
        val expected = validProjectConfigV4()

        verifyProjectConfig(actual, expected)
    }

    /**
     * Verify that invalid JSON results in a [ConfigParseException] being thrown.
     */
    @Test
    @Throws(Exception::class)
    fun invalidJsonExceptionWrapping() {
        thrown.expect(ConfigParseException::class.java)

        val parser = JsonConfigParser()
        parser.parseProjectConfig("invalid config")
    }

    /**
     * Verify that valid JSON without a required field results in a [ConfigParseException] being thrown.
     */
    @Test
    @Throws(Exception::class)
    fun validJsonRequiredFieldMissingExceptionWrapping() {
        thrown.expect(ConfigParseException::class.java)

        val parser = JsonConfigParser()
        parser.parseProjectConfig("{\"valid\": \"json\"}")
    }

    /**
     * Verify that empty string JSON results in a [ConfigParseException] being thrown.
     */
    @Test
    @Throws(Exception::class)
    fun emptyJsonExceptionWrapping() {
        thrown.expect(ConfigParseException::class.java)

        val parser = JsonConfigParser()
        parser.parseProjectConfig("")
    }

    /**
     * Verify that null JSON results in a [ConfigParseException] being thrown.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Testing nullness contract violation")
    @Throws(Exception::class)
    fun nullJsonExceptionWrapping() {
        thrown.expect(ConfigParseException::class.java)

        val parser = JsonConfigParser()
        parser.parseProjectConfig(null!!)
    }
}
