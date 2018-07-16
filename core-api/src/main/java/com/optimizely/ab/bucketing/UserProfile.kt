/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.bucketing

import java.util.HashMap
import kotlin.collections.Map.Entry

/**
 * A class representing a user's profile.
 */
class UserProfile
/**
 * Construct a User Profile instance from explicit components.
 *
 * @param userId              The ID of the user.
 * @param experimentBucketMap The bucketing experimentBucketMap of the user.
 */
(
        /**
         * A user's ID.
         */
        val userId: String,
        /**
         * The bucketing experimentBucketMap of the user.
         */
        val experimentBucketMap: MutableMap<String, Decision>) {

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as UserProfile?

        return if (userId != that!!.userId) false else experimentBucketMap == that.experimentBucketMap
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + experimentBucketMap.hashCode()
        return result
    }

    /**
     * Convert a User Profile instance to a Map.
     *
     * @return A map representation of the user profile instance.
     */
    internal fun toMap(): Map<String, Any> {
        val userProfileMap = HashMap<String, Any>(2)
        userProfileMap.put(UserProfileService.userIdKey, userId)
        val decisionsMap = HashMap<String, Map<String, String>>(experimentBucketMap.size)
        for ((key, value) in experimentBucketMap) {
            decisionsMap.put(key, value.toMap())
        }
        userProfileMap.put(UserProfileService.experimentBucketMapKey, decisionsMap)
        return userProfileMap
    }
}
