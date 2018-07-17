/**
 *
 * Copyright 2017, Optimizely and contributors
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

/**
 * Class encapsulating user profile service functionality.
 *
 * Override with your own implementation for storing and retrieving the user profile.
 */
interface UserProfileService {

    /**
     * Fetch the user profile map for the user ID.
     *
     * @param userId The ID of the user whose profile will be retrieved.
     * @return a Map representing the user's profile.
     * The returned `Map<String, Object>` of the user profile will have the following structure.
     * {
     * userIdKey : String userId,
     * decisionsKey : `Map<String, Map<String, String>>` decisions {
     * String experimentId : `Map<String, String>` decision {
     * variationIdKey : String variationId
     * }
     * }
     * }
     * @throws Exception Passes on whatever exceptions the implementation may throw.
     */
    @Throws(Exception::class)
    fun lookup(userId: String): Map<String, Any>

    /**
     * Save the user profile Map sent to this method.
     *
     * @param userProfile The Map representing the user's profile.
     * @throws Exception Can throw an exception if the user profile was not saved properly.
     */
    @Throws(Exception::class)
    fun save(userProfile: Map<String, Any>)

    companion object {

        /** The key for the user ID. Returns a String. */
        val userIdKey = "user_id"
        /** The key for the decisions Map. Returns a `Map<String, Map<String, String>>`. */
        val experimentBucketMapKey = "experiment_bucket_map"
        /** The key for the variation Id within a decision Map.  */
        val variationIdKey = "variation_id"
    }
}