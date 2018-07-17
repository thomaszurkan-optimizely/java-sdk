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
package com.optimizely.ab.config

import org.junit.Test

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat

/**
 * Tests for [Variation].
 */
class VariationTest {

    /**
     * Not a grammatical error: verify that [Variation. is] is comparing the provided value
     * with that given by [Variation.getKey].
     */
    @Test
    @Throws(Exception::class)
    fun isUsesVariationKey() {
        val variation = Variation("1234", "key")
        assertThat(variation.`is`("blah"), `is`(false))

        // we shouldn't be comparing the ids
        assertThat(variation.`is`("1234"), `is`(false))

        // we *should* be comparing keys
        assertThat(variation.`is`("key"), `is`(true))
    }
}