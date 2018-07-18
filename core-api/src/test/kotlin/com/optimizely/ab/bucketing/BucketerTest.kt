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
package com.optimizely.ab.bucketing

import ch.qos.logback.classic.Level
import com.optimizely.ab.bucketing.internal.MurmurHash3
import com.optimizely.ab.categories.ExhaustiveTest
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.TrafficAllocation
import com.optimizely.ab.config.Variation
import com.optimizely.ab.internal.LogbackVerifier
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.ExpectedException

import java.util.Arrays
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

import com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/**
 * Tests for [Bucketer].
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
class BucketerTest {

    @Rule
    var thrown = ExpectedException.none()

    @Rule
    var logbackVerifier = LogbackVerifier()

    /**
     * Verify that [Bucketer.generateBucketValue] correctly handles negative hashCodes.
     */
    @Test
    @Throws(Exception::class)
    fun generateBucketValueForNegativeHashCodes() {
        val algorithm = Bucketer(validProjectConfigV2())
        val actual = algorithm.generateBucketValue(-1)
        assertTrue("generated bucket value is not in range: " + actual,
                actual > 0 && actual < Bucketer.MAX_TRAFFIC_VALUE)
    }

    /**
     * Verify that across the entire 32-bit hashCode space, all generated bucket values fall within the range
     * [0, [Bucketer.MAX_TRAFFIC_VALUE]) and that there's an even distribution over 50/50 split.
     */
    @Test
    @Category(ExhaustiveTest::class)
    @Throws(Exception::class)
    fun generateBucketValueDistribution() {
        Assume.assumeTrue(java.lang.Boolean.valueOf(System.getenv("CI"))!!)
        var lowerHalfCount: Long = 0
        var totalCount: Long = 0
        var outOfRangeCount = 0

        val algorithm = Bucketer(validProjectConfigV2())
        for (i in Integer.MIN_VALUE..Integer.MAX_VALUE - 1) {
            val bucketValue = algorithm.generateBucketValue(i)

            totalCount++
            if (bucketValue < Bucketer.MAX_TRAFFIC_VALUE / 2) {
                lowerHalfCount++
            }
            if (bucketValue < 0 || bucketValue >= Bucketer.MAX_TRAFFIC_VALUE) {
                outOfRangeCount++
            }
        }

        // verify that all values are in the expected range and that 50% of the values are in the lower half
        assertThat(outOfRangeCount, `is`(0))
        assertThat(Math.round(lowerHalfCount.toDouble() / totalCount * 100), `is`(50L))
    }

    /**
     * Verify that generated bucket values match expected output.
     */
    @Test
    @Throws(Exception::class)
    fun bucketNumberGeneration() {
        val MURMUR_HASH_SEED = 1
        val experimentId = 1886780721
        var hashCode: Int

        val algorithm = Bucketer(validProjectConfigV2())

        var combinedBucketId: String

        combinedBucketId = "ppid1" + experimentId
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length, MURMUR_HASH_SEED)
        assertThat(algorithm.generateBucketValue(hashCode), `is`(5254))

        combinedBucketId = "ppid2" + experimentId
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length, MURMUR_HASH_SEED)
        assertThat(algorithm.generateBucketValue(hashCode), `is`(4299))

        combinedBucketId = "ppid2" + (experimentId + 1)
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length, MURMUR_HASH_SEED)
        assertThat(algorithm.generateBucketValue(hashCode), `is`(2434))

        combinedBucketId = "ppid3" + experimentId
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length, MURMUR_HASH_SEED)
        assertThat(algorithm.generateBucketValue(hashCode), `is`(5439))

        combinedBucketId = "a very very very very very very very very very very very very very very very long ppd " +
                "string" + experimentId
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length, MURMUR_HASH_SEED)
        assertThat(algorithm.generateBucketValue(hashCode), `is`(6128))
    }

    /**
     * Given an experiment with 4 variations, verify that bucket values are correctly mapped to the associated range.
     */
    @Test
    @Throws(Exception::class)
    fun bucketToMultipleVariations() {
        val audienceIds = emptyList<String>()

        // create an experiment with 4 variations using ranges: [0 -> 999, 1000 -> 4999, 5000 -> 5999, 6000 -> 9999]
        val variations = Arrays.asList(
                Variation("1", "var1"),
                Variation("2", "var2"),
                Variation("3", "var3"),
                Variation("4", "var4")
        )

        val trafficAllocations = Arrays.asList(
                TrafficAllocation("1", 1000),
                TrafficAllocation("2", 5000),
                TrafficAllocation("3", 6000),
                TrafficAllocation("4", 10000)
        )

        val experiment = Experiment("1234", "exp_key", "Running", "1", audienceIds, variations,
                emptyMap<String, String>(), trafficAllocations, "")

        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)

        // verify bucketing to the first variation
        bucketValue.set(0)
        assertThat<Variation>(algorithm.bucket(experiment, "user1"), `is`(variations[0]))
        bucketValue.set(500)
        assertThat<Variation>(algorithm.bucket(experiment, "user2"), `is`(variations[0]))
        bucketValue.set(999)
        assertThat<Variation>(algorithm.bucket(experiment, "user3"), `is`(variations[0]))

        // verify the second variation
        bucketValue.set(1000)
        assertThat<Variation>(algorithm.bucket(experiment, "user4"), `is`(variations[1]))
        bucketValue.set(4000)
        assertThat<Variation>(algorithm.bucket(experiment, "user5"), `is`(variations[1]))
        bucketValue.set(4999)
        assertThat<Variation>(algorithm.bucket(experiment, "user6"), `is`(variations[1]))

        // ...and the rest
        bucketValue.set(5100)
        assertThat<Variation>(algorithm.bucket(experiment, "user7"), `is`(variations[2]))
        bucketValue.set(6500)
        assertThat<Variation>(algorithm.bucket(experiment, "user8"), `is`(variations[3]))
    }

    /**
     * Verify that in certain cases, users aren't assigned any variation and null is returned.
     */
    @Test
    @Throws(Exception::class)
    fun bucketToControl() {
        val bucketingId = "blah"
        val userId = "user1"

        val audienceIds = emptyList<String>()

        val variations = listOf<Variation>(Variation("1", "var1"))

        val trafficAllocations = listOf<TrafficAllocation>(TrafficAllocation("1", 999))

        val experiment = Experiment("1234", "exp_key", "Running", "1", audienceIds, variations,
                emptyMap<String, String>(), trafficAllocations, "")

        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 0 to user with bucketingId \"$bucketingId\" when bucketing to a variation.")
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"$bucketingId\" is in variation \"var1\" of experiment \"exp_key\".")

        // verify bucketing to the first variation
        bucketValue.set(0)
        assertThat<Variation>(algorithm.bucket(experiment, bucketingId), `is`<Variation>(variations.get(0)))

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 1000 to user with bucketingId \"$bucketingId\" when bucketing to a variation.")
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"$bucketingId\" is not in any variation of experiment \"exp_key\".")

        // verify bucketing to no variation (null)
        bucketValue.set(1000)
        assertNull(algorithm.bucket(experiment, bucketingId))
    }


    //========== Tests for Grouped experiments ==========//

    /**
     * Verify that [Bucketer.bucket] returns the proper variation when a user is
     * in the group experiment.
     */
    @Test
    @Throws(Exception::class)
    fun bucketUserInExperiment() {
        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(3000)

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[0].experiments
        val groupExperiment = groupExperiments[0]
        logbackVerifier.expectMessage(Level.DEBUG,
                "Assigned bucket 3000 to user with bucketingId \"blah\" during experiment bucketing.")
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"blah\" is in experiment \"group_etag2\" of group 42.")
        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket 3000 to user with bucketingId \"blah\" when bucketing to a variation.")
        logbackVerifier.expectMessage(Level.INFO,
                "User with bucketingId \"blah\" is in variation \"e2_vtag1\" of experiment \"group_etag2\".")
        assertThat<Variation>(algorithm.bucket(groupExperiment, "blah"), `is`(groupExperiment.variations[0]))
    }

    /**
     * Verify that [Bucketer.bucket] doesn't return a variation when a user isn't bucketed
     * into the group experiment.
     */
    @Test
    @Throws(Exception::class)
    fun bucketUserNotInExperiment() {
        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(3000)

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[0].experiments
        val groupExperiment = groupExperiments[1]
        // the user should be bucketed to a different experiment than the one provided, resulting in no variation being
        // returned.
        logbackVerifier.expectMessage(Level.DEBUG,
                "Assigned bucket 3000 to user with bucketingId \"blah\" during experiment bucketing.")
        logbackVerifier.expectMessage(Level.INFO,
                "User with bucketingId \"blah\" is not in experiment \"group_etag1\" of group 42")
        assertNull(algorithm.bucket(groupExperiment, "blah"))
    }

    /**
     * Verify that [Bucketer.bucket] doesn't return a variation when the user is bucketed to
     * the traffic space of a deleted experiment within a random group.
     */
    @Test
    @Throws(Exception::class)
    fun bucketUserToDeletedExperimentSpace() {
        val bucketValue = AtomicInteger()
        val bucketIntVal = 9000
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(bucketIntVal)

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[0].experiments
        val groupExperiment = groupExperiments[1]

        logbackVerifier.expectMessage(Level.DEBUG, "Assigned bucket $bucketIntVal to user with bucketingId \"blah\" during experiment bucketing.")
        logbackVerifier.expectMessage(Level.INFO, "User with bucketingId \"blah\" is not in any experiment of group 42.")
        assertNull(algorithm.bucket(groupExperiment, "blah"))
    }

    /**
     * Verify that [Bucketer.bucket] returns a variation when the user falls into an
     * experiment within an overlapping group.
     */
    @Test
    @Throws(Exception::class)
    fun bucketUserToVariationInOverlappingGroupExperiment() {
        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(0)

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[1].experiments
        val groupExperiment = groupExperiments[0]
        val expectedVariation = groupExperiment.variations[0]

        logbackVerifier.expectMessage(
                Level.INFO,
                "User with bucketingId \"blah\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".")
        assertThat<Variation>(algorithm.bucket(groupExperiment, "blah"), `is`(expectedVariation))
    }

    /**
     * Verify that [Bucketer.bucket] doesn't return a variation when the user doesn't fall
     * into an experiment within an overlapping group.
     */
    @Test
    @Throws(Exception::class)
    fun bucketUserNotInOverlappingGroupExperiment() {
        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(3000)

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[1].experiments
        val groupExperiment = groupExperiments[0]

        logbackVerifier.expectMessage(Level.INFO,
                "User with bucketingId \"blah\" is not in any variation of experiment \"overlapping_etag1\".")

        assertNull(algorithm.bucket(groupExperiment, "blah"))
    }

    @Test
    fun testBucketWithBucketingId() {
        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(0)
        val bucketingId = "blah"
        val userId = "blahUser"

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[1].experiments
        val groupExperiment = groupExperiments[0]
        val expectedVariation = groupExperiment.variations[0]

        logbackVerifier.expectMessage(
                Level.INFO,
                "User with bucketingId \"$bucketingId\" is in variation \"e1_vtag1\" of experiment \"overlapping_etag1\".")
        assertThat<Variation>(algorithm.bucket(groupExperiment, bucketingId), `is`(expectedVariation))

    }

    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    fun testBucketWithNullBucketingId() {
        val bucketValue = AtomicInteger()
        val algorithm = mockBucketAlgorithm(bucketValue)
        bucketValue.set(0)

        val projectConfig = validProjectConfigV2()
        val groupExperiments = projectConfig.groups[1].experiments
        val groupExperiment = groupExperiments[0]

        try {
            algorithm.bucket(groupExperiment, "")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e)
        }

    }

    //======== Helper methods ========//

    /**
     * Sets up a mock algorithm that returns an expected bucket value.
     *
     * @param bucketValue the expected bucket value holder
     * @return the mock bucket algorithm
     */
    private fun mockBucketAlgorithm(bucketValue: AtomicInteger): Bucketer {
        return object : Bucketer(validProjectConfigV2()) {
            internal override fun generateBucketValue(hashCode: Int): Int {
                return bucketValue.get()
            }
        }
    }
}
