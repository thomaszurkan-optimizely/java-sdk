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

import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.parser.ConfigParseException
import com.optimizely.ab.event.NoopEventHandler

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup

import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.Properties
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * JMH benchmarks for public [Optimizely] functions
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
class OptimizelyBenchmark {

    private var optimizely: Optimizely? = null
    private val random = Random()

    private var activateGroupExperimentUserId: String? = null
    private var activateGroupExperimentAttributesUserId: String? = null
    private var trackGroupExperimentUserId: String? = null
    private var trackGroupExperimentAttributesUserId: String? = null

    @Param("10", "25", "50")
    private val numExperiments: Int = 0

    @Setup
    @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification = "stream is safely closed")
    @Throws(IOException::class, ConfigParseException::class)
    fun setup() {
        val properties = Properties()
        val propertiesStream = javaClass.getResourceAsStream("/benchmark.properties")
        properties.load(propertiesStream)
        propertiesStream.close()

        val datafilePathTemplate = properties.getProperty("datafilePathTemplate")
        val datafilePath = String.format(datafilePathTemplate, numExperiments)

        val activateGroupExperimentPropTemplate = properties.getProperty("activateGroupExperimentUserIdPropTemplate")
        activateGroupExperimentUserId = properties.getProperty(
                String.format(activateGroupExperimentPropTemplate, numExperiments))

        val activateGroupExperimentAttributesPropTemplate = properties.getProperty("activateGroupExperimentAttributesUserIdPropTemplate")
        activateGroupExperimentAttributesUserId = properties.getProperty(
                String.format(activateGroupExperimentAttributesPropTemplate, numExperiments))

        val trackGroupExperimentPropTemplate = properties.getProperty("trackGroupExperimentUserIdPropTemplate")
        trackGroupExperimentUserId = properties.getProperty(
                String.format(trackGroupExperimentPropTemplate, numExperiments))

        val trackGroupExperimentAttributesPropTemplate = properties.getProperty("trackGroupExperimentAttributesUserIdPropTemplate")
        trackGroupExperimentAttributesUserId = properties.getProperty(
                String.format(trackGroupExperimentAttributesPropTemplate, numExperiments))

        optimizely = Optimizely.builder(BenchmarkUtils.getProfilingDatafile(datafilePath),
                NoopEventHandler()).build()
    }

    @Benchmark
    fun measureGetVariationWithNoAttributes(): Variation? {
        return optimizely!!.getVariation("testExperiment2", "optimizely_user" + random.nextInt())
    }

    @Benchmark
    fun measureGetVariationWithAttributes(): Variation? {
        return optimizely!!.getVariation("testExperimentWithFirefoxAudience", "optimizely_user" + random.nextInt(),
                Collections.singletonMap("browser_type", "firefox"))
    }

    @Benchmark
    fun measureGetVariationWithForcedVariation(): Variation? {
        return optimizely!!.getVariation("testExperiment2", "variation_user")
    }

    @Benchmark
    fun measureGetVariationForGroupExperimentWithNoAttributes(): Variation? {
        return optimizely!!.getVariation("mutex_exp2", activateGroupExperimentUserId!!)
    }

    @Benchmark
    fun measureGetVariationForGroupExperimentWithAttributes(): Variation? {
        return optimizely!!.getVariation("mutex_exp1", activateGroupExperimentAttributesUserId!!,
                Collections.singletonMap("browser_type", "chrome"))
    }

    @Benchmark
    fun measureActivateWithNoAttributes(): Variation? {
        return optimizely!!.activate("testExperiment2", "optimizely_user" + random.nextInt())
    }

    @Benchmark
    fun measureActivateWithAttributes(): Variation? {
        return optimizely!!.activate("testExperimentWithFirefoxAudience", "optimizely_user" + random.nextInt(),
                Collections.singletonMap("browser_type", "firefox"))
    }

    @Benchmark
    fun measureActivateWithForcedVariation(): Variation? {
        return optimizely!!.activate("testExperiment2", "variation_user")
    }

    @Benchmark
    fun measureActivateForGroupExperimentWithNoAttributes(): Variation? {
        return optimizely!!.activate("mutex_exp2", activateGroupExperimentUserId!!)
    }

    @Benchmark
    fun measureActivateForGroupExperimentWithAttributes(): Variation? {
        return optimizely!!.activate("mutex_exp1", activateGroupExperimentAttributesUserId!!,
                Collections.singletonMap("browser_type", "chrome"))
    }

    @Benchmark
    fun measureActivateForGroupExperimentWithForcedVariation(): Variation? {
        return optimizely!!.activate("mutex_exp2", "user_a")
    }

    @Benchmark
    fun measureTrackWithNoAttributes() {
        optimizely!!.track("testEventWithMultipleExperiments", "optimizely_user" + random.nextInt())
    }

    @Benchmark
    fun measureTrackWithAttributes() {
        optimizely!!.track("testEventWithMultipleExperiments", "optimizely_user" + random.nextInt(),
                Collections.singletonMap("browser_type", "firefox"))
    }

    @Benchmark
    fun measureTrackWithGroupExperimentsNoAttributes() {
        optimizely!!.track("testEventWithMultipleExperiments", trackGroupExperimentUserId!!)
    }

    @Benchmark
    fun measureTrackWithGroupExperimentsAndAttributes() {
        optimizely!!.track("testEventWithMultipleExperiments", trackGroupExperimentAttributesUserId!!,
                Collections.singletonMap("browser_type", "chrome"))
    }

    @Benchmark
    fun measureTrackWithGroupExperimentsAndForcedVariation() {
        optimizely!!.track("testEventWithMultipleExperiments", "user_a")
    }
}
