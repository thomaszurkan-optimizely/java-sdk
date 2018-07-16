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
package com.optimizely.ab.event.internal.serializer

import com.optimizely.ab.event.internal.payload.EventBatch

import org.json.JSONObject

import org.junit.Test

import java.io.IOException

import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionWithSessionId
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionWithSessionIdJson
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionWithSessionId
import com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionWithSessionIdJson

import org.junit.Assert.assertTrue

class JsonSerializerTest {

    private val serializer = JsonSerializer()

    @Test
    @Throws(IOException::class)
    fun serializeImpression() {
        val impression = generateImpression()
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        val actual = JSONObject(serializer.serialize(impression))
        val expected = JSONObject(generateImpressionJson())

        assertTrue(actual.similar(expected))
    }

    @Test
    @Throws(IOException::class)
    fun serializeImpressionWithSessionId() {
        val impression = generateImpressionWithSessionId()
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        val actual = JSONObject(serializer.serialize(impression))
        val expected = JSONObject(generateImpressionWithSessionIdJson())

        assertTrue(actual.similar(expected))
    }

    @Test
    @Throws(IOException::class)
    fun serializeConversion() {
        val conversion = generateConversion()
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        val actual = JSONObject(serializer.serialize(conversion))
        val expected = JSONObject(generateConversionJson())

        assertTrue(actual.similar(expected))
    }

    @Test
    @Throws(IOException::class)
    fun serializeConversionWithSessionId() {
        val conversion = generateConversionWithSessionId()
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        val actual = JSONObject(serializer.serialize(conversion))
        val expected = JSONObject(generateConversionWithSessionIdJson())

        assertTrue(actual.similar(expected))
    }
}
