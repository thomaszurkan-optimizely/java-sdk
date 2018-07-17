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

import java.util.HashMap
import kotlin.collections.Map.Entry

/**
 * A Utils class to help transform maps to [UserProfile] instances.
 */
object UserProfileUtils {

    /**
     * Validate whether a `Map<String, Object>` can be transformed into a [UserProfile].
     * @param map The map to check.
     * @return True if the map can be converted into a [UserProfile].
     * False if the map cannot be converted.
     */
    fun isValidUserProfileMap(map: Map<String, Any>): Boolean {
        // The Map must contain a value for the user ID
        if (!map.containsKey(UserProfileService.userIdKey)) {
            return false
        }
        // The Map must contain a value for the experiment bucket map
        if (!map.containsKey(UserProfileService.experimentBucketMapKey)) {
            return false
        }
        // The value for the experimentBucketMapKey must be a map
        if (map[UserProfileService.experimentBucketMapKey] !is Map<*, *>) {
            return false
        }
        // Try and cast the experimentBucketMap value to a typed map
        val experimentBucketMap: Map<String, Map<String, String>>
        try {
            experimentBucketMap = map[UserProfileService.experimentBucketMapKey] as Map<String, Map<String, String>>
        } catch (classCastException: ClassCastException) {
            return false
        }

        // Check each Decision in the map to make sure it has a variation Id Key
        for (decision in experimentBucketMap.values) {
            if (!decision.containsKey(UserProfileService.variationIdKey)) {
                return false
            }
        }

        // the map is good enough for us to use
        return true
    }

    /**
     * Convert a Map to a [UserProfile] instance.
     * @param map The map to construct the [UserProfile] from.
     * @return A [UserProfile] instance.
     */
    fun convertMapToUserProfile(map: Map<String, Any>): UserProfile {
        val userId = map[UserProfileService.userIdKey] as String
        val experimentBucketMap = map[UserProfileService.experimentBucketMapKey] as Map<String, Map<String, String>>
        val decisions = HashMap<String, Decision>(experimentBucketMap.size)
        for ((key, value) in experimentBucketMap) {
            value.let {
                it.get(UserProfileService.variationIdKey).let {
                    val decision = Decision(it!!)
                    decisions.put(key, decision)

                }

            }
        }
        return UserProfile(userId, decisions)
    }
}
