/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                        *
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

import com.optimizely.ab.OptimizelyRuntimeException
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.FeatureFlag
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Rollout
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.internal.ExperimentUtils
import com.optimizely.ab.internal.ControlAttribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashMap

/**
 * Optimizely's decision service that determines which variation of an experiment the user will be allocated to.
 *
 * The decision service contains all logic around how a user decision is made. This includes all of the following:
 * 1. Checking experiment status
 * 2. Checking whitelisting
 * 3. Checking sticky bucketing
 * 4. Checking audience targeting
 * 5. Using Murmurhash3 to bucket the user.
 */
class DecisionService
/**
 * Initialize a decision service for the Optimizely client.
 * @param bucketer Base bucketer to allocate new users to an experiment.
 * @param errorHandler The error handler of the Optimizely client.
 * @param projectConfig Optimizely Project Config representing the datafile.
 * @param userProfileService UserProfileService implementation for storing user info.
 */
(private val bucketer: Bucketer,
 private val errorHandler: ErrorHandler,
 private val projectConfig: ProjectConfig,
 private val userProfileService: UserProfileService?) {

    /**
     * Get a [Variation] of an [Experiment] for a user to be allocated into.
     *
     * @param experiment The Experiment the user will be bucketed into.
     * @param userId The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @return The [Variation] the user is allocated into.
     */
    fun getVariation(experiment: Experiment,
                     userId: String,
                     filteredAttributes: Map<String, String>): Variation? {

        if (!ExperimentUtils.isExperimentActive(experiment)) {
            return null
        }

        // look for forced bucketing first.
        var variation = projectConfig.getForcedVariation(experiment.key, userId)

        // check for whitelisting
        if (variation == null) {
            variation = getWhitelistedVariation(experiment, userId)
        }

        if (variation != null) {
            return variation
        }

        // fetch the user profile map from the user profile service
        var userProfile: UserProfile? = null

        if (userProfileService != null) {
            try {
                val userProfileMap = userProfileService.lookup(userId)
                if (userProfileMap == null) {
                    logger.info("We were unable to get a user profile map from the UserProfileService.")
                } else if (UserProfileUtils.isValidUserProfileMap(userProfileMap)) {
                    userProfile = UserProfileUtils.convertMapToUserProfile(userProfileMap)
                } else {
                    logger.warn("The UserProfileService returned an invalid map.")
                }
            } catch (exception: Exception) {
                logger.error(exception.message)
                errorHandler.handleError(OptimizelyRuntimeException(exception))
            }

        }

        // check if user exists in user profile
        if (userProfile != null) {
            variation = getStoredVariation(experiment, userProfile)
            // return the stored variation if it exists
            if (variation != null) {
                return variation
            }
        } else { // if we could not find a user profile, make a new one
            userProfile = UserProfile(userId, HashMap())
        }

        if (ExperimentUtils.isUserInExperiment(projectConfig, experiment, filteredAttributes)) {
            var bucketingId = userId
            if (filteredAttributes.containsKey(ControlAttribute.BUCKETING_ATTRIBUTE.toString())) {
                bucketingId = filteredAttributes[ControlAttribute.BUCKETING_ATTRIBUTE.toString()]!!
            }
            variation = bucketer.bucket(experiment, bucketingId)

            if (variation != null) {
                if (userProfileService != null) {
                    saveVariation(experiment, variation, userProfile)
                } else {
                    logger.info("This decision will not be saved since the UserProfileService is null.")
                }
            }

            return variation
        }
        logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userId, experiment.key)

        return null
    }

    /**
     * Get the variation the user is bucketed into for the FeatureFlag
     * @param featureFlag The feature flag the user wants to access.
     * @param userId User Identifier
     * @param filteredAttributes A map of filtered attributes.
     * @return [FeatureDecision]
     */
    fun getVariationForFeature(featureFlag: FeatureFlag,
                               userId: String,
                               filteredAttributes: Map<String, String>): FeatureDecision {
        if (!featureFlag.experimentIds.isEmpty()) {
            for (experimentId in featureFlag.experimentIds) {
                val experiment = projectConfig.experimentIdMapping[experimentId]
                val variation = this.getVariation(experiment!!, userId, filteredAttributes)
                if (variation != null) {
                    return FeatureDecision(experiment, variation,
                            FeatureDecision.DecisionSource.EXPERIMENT)
                }
            }
        } else {
            logger.info("The feature flag \"{}\" is not used in any experiments.", featureFlag.key)
        }

        val featureDecision = getVariationForFeatureInRollout(featureFlag, userId, filteredAttributes)
        if (featureDecision.variation == null) {
            logger.info("The user \"{}\" was not bucketed into a rollout for feature flag \"{}\".",
                    userId, featureFlag.key)
        } else {
            logger.info("The user \"{}\" was bucketed into a rollout for feature flag \"{}\".",
                    userId, featureFlag.key)
        }
        return featureDecision
    }

    /**
     * Try to bucket the user into a rollout rule.
     * Evaluate the user for rules in priority order by seeing if the user satisfies the audience.
     * Fall back onto the everyone else rule if the user is ever excluded from a rule due to traffic allocation.
     * @param featureFlag The feature flag the user wants to access.
     * @param userId User Identifier
     * @param filteredAttributes A map of filtered attributes.
     * @return [FeatureDecision]
     */
    internal fun getVariationForFeatureInRollout(featureFlag: FeatureFlag,
                                                 userId: String,
                                                 filteredAttributes: Map<String, String>): FeatureDecision {
        // use rollout to get variation for feature
        if (featureFlag.rolloutId.isEmpty()) {
            logger.info("The feature flag \"{}\" is not used in a rollout.", featureFlag.key)
            return FeatureDecision(null, null, null)
        }
        val rollout = projectConfig.rolloutIdMapping[featureFlag.rolloutId]
        if (rollout == null) {
            logger.error("The rollout with id \"{}\" was not found in the datafile for feature flag \"{}\".",
                    featureFlag.rolloutId, featureFlag.key)
            return FeatureDecision(null, null, null)
        }

        // for all rules before the everyone else rule
        val rolloutRulesLength = rollout.experiments.size
        var bucketingId = userId
        if (filteredAttributes.containsKey(ControlAttribute.BUCKETING_ATTRIBUTE.toString())) {
            bucketingId = filteredAttributes[ControlAttribute.BUCKETING_ATTRIBUTE.toString()]!!
        }
        var variation: Variation?
        for (i in 0..rolloutRulesLength - 1 - 1) {
            val rolloutRule = rollout.experiments[i]
            val audience = projectConfig.audienceIdMapping[rolloutRule.audienceIds[0]]
            if (ExperimentUtils.isUserInExperiment(projectConfig, rolloutRule, filteredAttributes)) {
                variation = bucketer.bucket(rolloutRule, bucketingId)
                if (variation == null) {
                    break
                }
                return FeatureDecision(rolloutRule, variation,
                        FeatureDecision.DecisionSource.ROLLOUT)
            } else {
                logger.debug("User \"{}\" did not meet the conditions to be in rollout rule for audience \"{}\".",
                        userId, audience?.getName())
            }
        }

        // get last rule which is the fall back rule
        val finalRule = rollout.experiments[rolloutRulesLength - 1]
        if (ExperimentUtils.isUserInExperiment(projectConfig, finalRule, filteredAttributes)) {
            variation = bucketer.bucket(finalRule, bucketingId)
            if (variation != null) {
                return FeatureDecision(finalRule, variation,
                        FeatureDecision.DecisionSource.ROLLOUT)
            }
        }
        return FeatureDecision(null, null, null)
    }

    /**
     * Get the variation the user has been whitelisted into.
     * @param experiment [Experiment] in which user is to be bucketed.
     * @param userId User Identifier
     * @return null if the user is not whitelisted into any variation
     * [Variation] the user is bucketed into if the user has a specified whitelisted variation.
     */
    internal fun getWhitelistedVariation(experiment: Experiment, userId: String): Variation? {
        // if a user has a forced variation mapping, return the respective variation
        val userIdToVariationKeyMap = experiment.userIdToVariationKeyMap
        if (userIdToVariationKeyMap.containsKey(userId)) {
            val forcedVariationKey = userIdToVariationKeyMap[userId]
            val forcedVariation = experiment.variationKeyToVariationMap[forcedVariationKey]
            if (forcedVariation != null) {
                logger.info("User \"{}\" is forced in variation \"{}\".", userId, forcedVariationKey)
            } else {
                logger.error("Variation \"{}\" is not in the datafile. Not activating user \"{}\".",
                        forcedVariationKey, userId)
            }
            return forcedVariation
        }
        return null
    }

    /**
     * Get the [Variation] that has been stored for the user in the [UserProfileService] implementation.
     * @param experiment [Experiment] in which the user was bucketed.
     * @param userProfile [UserProfile] of the user.
     * @return null if the [UserProfileService] implementation is null or the user was not previously bucketed.
     * else return the [Variation] the user was previously bucketed into.
     */
    internal fun getStoredVariation(experiment: Experiment,
                                    userProfile: UserProfile): Variation? {
        // ---------- Check User Profile for Sticky Bucketing ----------
        // If a user profile instance is present then check it for a saved variation
        val experimentId = experiment.id
        val experimentKey = experiment.key
        val decision = userProfile.experimentBucketMap[experimentId]
        if (decision != null) {
            val variationId = decision.variationId
            val savedVariation = projectConfig
                    .experimentIdMapping[experimentId]?.variationIdToVariationMap?.get(variationId)
            if (savedVariation != null) {
                logger.info("Returning previously activated variation \"{}\" of experiment \"{}\" " + "for user \"{}\" from user profile.",
                        savedVariation.key, experimentKey, userProfile.userId)
                // A variation is stored for this combined bucket id
                return savedVariation
            } else {
                logger.info("User \"{}\" was previously bucketed into variation with ID \"{}\" for experiment \"{}\", " + "but no matching variation was found for that user. We will re-bucket the user.",
                        userProfile.userId, variationId, experimentKey)
                return null
            }
        } else {
            logger.info("No previously activated variation of experiment \"{}\" " + "for user \"{}\" found in user profile.",
                    experimentKey, userProfile.userId)
            return null
        }
    }

    /**
     * Save a [Variation] of an [Experiment] for a user in the [UserProfileService].
     *
     * @param experiment The experiment the user was buck
     * @param variation The Variation to save.
     * @param userProfile A [UserProfile] instance of the user information.
     */
    internal fun saveVariation(experiment: Experiment,
                               variation: Variation,
                               userProfile: UserProfile) {
        // only save if the user has implemented a user profile service
        if (userProfileService != null) {
            val experimentId = experiment.id
            val variationId = variation.id
            val decision: Decision
            if (userProfile.experimentBucketMap.containsKey(experimentId)) {
                decision = userProfile.experimentBucketMap[experimentId]!!
                decision.variationId = variationId
            } else {
                decision = Decision(variationId)
            }
            userProfile.experimentBucketMap.put(experimentId, decision)

            try {
                userProfileService.save(userProfile.toMap())
                logger.info("Saved variation \"{}\" of experiment \"{}\" for user \"{}\".",
                        variationId, experimentId, userProfile.userId)
            } catch (exception: Exception) {
                logger.warn("Failed to save variation \"{}\" of experiment \"{}\" for user \"{}\".",
                        variationId, experimentId, userProfile.userId)
                errorHandler.handleError(OptimizelyRuntimeException(exception))
            }

        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DecisionService::class.java!!)
    }
}
