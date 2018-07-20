/****************************************************************************
 * Copyright 2016-2018, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab

import com.optimizely.ab.annotations.VisibleForTesting
import com.optimizely.ab.bucketing.Bucketer
import com.optimizely.ab.bucketing.DecisionService
import com.optimizely.ab.bucketing.FeatureDecision
import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.LiveVariable
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.parser.ConfigParseException
import com.optimizely.ab.config.parser.DefaultConfigParser
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.error.NoOpErrorHandler
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.internal.BuildVersionInfo
import com.optimizely.ab.event.internal.EventBuilder
import com.optimizely.ab.event.ClientEngine
import com.optimizely.ab.notification.NotificationCenter
import org.slf4j.LoggerFactory
import javax.annotation.concurrent.ThreadSafe
import java.util.ArrayList
import java.util.HashMap

/**
 * Top-level container class for Optimizely functionality.
 * Thread-safe, so can be created as a singleton and safely passed around.
 *
 * Example instantiation:
 * <pre>
 * Optimizely optimizely = Optimizely.builder(projectWatcher, eventHandler).build();
</pre> *
 *
 * To activate an experiment and perform variation specific processing:
 * <pre>
 * Variation variation = optimizely.activate(experimentKey, userId, attributes);
 * if (variation.is("ALGORITHM_A")) {
 * // execute code for algorithm A
 * } else if (variation.is("ALGORITHM_B")) {
 * // execute code for algorithm B
 * } else {
 * // execute code for default algorithm
 * }
</pre> *
 *
 * **NOTE:** by default, all exceptions originating from `Optimizely` calls are suppressed.
 * For example, attempting to activate an experiment that does not exist in the project config will cause an error
 * to be logged, and for the "control" variation to be returned.
 */
@ThreadSafe
class Optimizely private constructor(@VisibleForTesting
                                     /**
                                      * @return the current [ProjectConfig] instance.
                                      */
                                     val projectConfig: ProjectConfig,
                                     @VisibleForTesting internal val decisionService: DecisionService,
                                     @VisibleForTesting internal val eventHandler: EventHandler,
                                     @VisibleForTesting internal val eventBuilder: EventBuilder,
                                     @VisibleForTesting internal val errorHandler: ErrorHandler,
                                     val userProfileService: UserProfileService?) {
    val notificationCenter = NotificationCenter()

    // Do work here that should be done once per Optimizely lifecycle
    @VisibleForTesting
    internal fun initialize() {

    }

    @Throws(UnknownExperimentException::class)
    @JvmOverloads
    fun activate(experimentKey: String,
                 userId: String,
                 attributes: Map<String, String> = emptyMap<String, String>()): Variation? {

        if (experimentKey == null) {
            logger.error("The experimentKey parameter must be nonnull.")
            return null
        }

        if (!validateUserId(userId)) {
            logger.info("Not activating user for experiment \"{}\".", experimentKey)
            return null
        }

        val currentConfig = projectConfig

        val experiment = currentConfig.getExperimentForKey(experimentKey, errorHandler)
        if (experiment == null) {
            // if we're unable to retrieve the associated experiment, return null
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experimentKey)
            return null
        }

        return activate(currentConfig, experiment, userId, attributes)
    }

    @JvmOverloads
    fun activate(experiment: Experiment,
                 userId: String,
                 attributes: Map<String, String> = emptyMap<String, String>()): Variation? {

        val currentConfig = projectConfig

        return activate(currentConfig, experiment, userId, attributes)
    }

    private fun activate(projectConfig: ProjectConfig,
                         experiment: Experiment,
                         userId: String,
                         attributes: Map<String, String>): Variation? {

        if (!validateUserId(userId)) {
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.key)
            return null
        }
        // determine whether all the given attributes are present in the project config. If not, filter out the unknown
        // attributes.
        val filteredAttributes = filterAttributes(projectConfig, attributes)

        // bucket the user to the given experiment and dispatch an impression event
        val variation = decisionService.getVariation(experiment, userId, filteredAttributes)
        if (variation == null) {
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.key)
            return null
        }

        sendImpression(projectConfig, experiment, userId, filteredAttributes, variation)

        return variation
    }

    private fun sendImpression(projectConfig: ProjectConfig,
                               experiment: Experiment,
                               userId: String,
                               filteredAttributes: Map<String, String>,
                               variation: Variation) {
        if (experiment.isRunning) {
            val impressionEvent = eventBuilder.createImpressionEvent(
                    projectConfig,
                    experiment,
                    variation,
                    userId,
                    filteredAttributes)
            logger.info("Activating user \"{}\" in experiment \"{}\".", userId, experiment.key)
            logger.debug(
                    "Dispatching impression event to URL {} with params {} and payload \"{}\".",
                    impressionEvent.endpointUrl, impressionEvent.requestParams, impressionEvent.body)
            try {
                eventHandler.dispatchEvent(impressionEvent)
            } catch (e: Exception) {
                logger.error("Unexpected exception in event dispatcher", e)
            }

            notificationCenter.sendNotifications(NotificationCenter.NotificationType.Activate, experiment, userId,
                    filteredAttributes, variation, impressionEvent)
        } else {
            logger.info("Experiment has \"Launched\" status so not dispatching event during activation.")
        }
    }

    //======== track calls ========//

    @Throws(UnknownEventTypeException::class)
    fun track(eventName: String,
              userId: String) {
        track(eventName, userId, emptyMap<String, String>(), emptyMap<String, Any>())
    }

    @Throws(UnknownEventTypeException::class)
    @JvmOverloads
    fun track(eventName: String,
              userId: String,
              attributes: Map<String, String>,
              eventTags: Map<String, *> = emptyMap<String, String>()) {
        var eventTags = eventTags

        if (!validateUserId(userId)) {
            logger.info("Not tracking event \"{}\".", eventName)
            return
        }

        if (eventName == null || eventName.trim { it <= ' ' }.isEmpty()) {
            logger.error("Event Key is null or empty when non-null and non-empty String was expected.")
            logger.info("Not tracking event for user \"{}\".", userId)
            return
        }

        val currentConfig = projectConfig

        val eventType = currentConfig.getEventTypeForName(eventName, errorHandler)
        if (eventType == null) {
            // if no matching event type could be found, do not dispatch an event
            logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId)
            return
        }

        // determine whether all the given attributes are present in the project config. If not, filter out the unknown
        // attributes.
        val filteredAttributes = filterAttributes(currentConfig, attributes)

        if (eventTags == null) {
            logger.warn("Event tags is null when non-null was expected. Defaulting to an empty event tags map.")
            eventTags = emptyMap<String, String>()
        }

        val experimentsForEvent = projectConfig.getExperimentsForEventKey(eventName)
        val experimentVariationMap = HashMap<Experiment, Variation>(experimentsForEvent.size)
        for (experiment in experimentsForEvent) {
            if (experiment.isRunning) {
                val variation = decisionService.getVariation(experiment, userId, filteredAttributes)
                if (variation != null) {
                    experimentVariationMap.put(experiment, variation)
                }
            } else {
                logger.info(
                        "Not tracking event \"{}\" for experiment \"{}\" because experiment has status \"Launched\".",
                        eventType.key, experiment.key)
            }
        }

        // create the conversion event request parameters, then dispatch
        val conversionEvent = eventBuilder.createConversionEvent(
                projectConfig,
                experimentVariationMap,
                userId,
                eventType.id,
                eventType.key,
                filteredAttributes,
                eventTags)

        if (conversionEvent == null) {
            logger.info("There are no valid experiments for event \"{}\" to track.", eventName)
            logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId)
            return
        }

        logger.info("Tracking event \"{}\" for user \"{}\".", eventName, userId)
        logger.debug("Dispatching conversion event to URL {} with params {} and payload \"{}\".",
                conversionEvent.endpointUrl, conversionEvent.requestParams, conversionEvent.body)
        try {
            eventHandler.dispatchEvent(conversionEvent)
        } catch (e: Exception) {
            logger.error("Unexpected exception in event dispatcher", e)
        }

        notificationCenter.sendNotifications(NotificationCenter.NotificationType.Track, eventName, userId,
                filteredAttributes, eventTags, conversionEvent)
    }

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return True if the feature is enabled.
     * False if the feature is disabled.
     * False if the feature is not found.
     */
    @JvmOverloads
    fun isFeatureEnabled(featureKey: String,
                         userId: String,
                         attributes: Map<String, String> = emptyMap<String, String>()): Boolean {
        if (featureKey == null) {
            logger.warn("The featureKey parameter must be nonnull.")
            return false
        } else if (userId == null) {
            logger.warn("The userId parameter must be nonnull.")
            return false
        }
        val featureFlag = projectConfig.featureKeyMapping[featureKey]
        if (featureFlag == null) {
            logger.info("No feature flag was found for key \"{}\".", featureKey)
            return false
        }

        val filteredAttributes = filterAttributes(projectConfig, attributes)

        val featureDecision = decisionService.getVariationForFeature(featureFlag, userId, filteredAttributes)
        if (featureDecision.variation != null) {
            if (featureDecision.decisionSource == FeatureDecision.DecisionSource.EXPERIMENT) {
                sendImpression(
                        projectConfig,
                        featureDecision.experiment!!,
                        userId,
                        filteredAttributes,
                        featureDecision.variation!!)
            } else {
                logger.info("The user \"{}\" is not included in an experiment for feature \"{}\".",
                        userId, featureKey)
            }
            featureDecision.variation?.featureEnabled?.let {
                if (it) {
                    logger.info("Feature \"{}\" is enabled for user \"{}\".", featureKey, userId)
                    return true
                }
            }
        }

        logger.info("Feature \"{}\" is not enabled for user \"{}\".", featureKey, userId)
        return false
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Boolean value of the boolean single variable feature.
     * Null if the feature or variable could not be found.
     */
    @JvmOverloads
    fun getFeatureVariableBoolean(featureKey: String,
                                  variableKey: String,
                                  userId: String,
                                  attributes: Map<String, String> = emptyMap<String, String>()): Boolean? {
        val variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.BOOLEAN
        )
        return if (variableValue != null) {
            java.lang.Boolean.parseBoolean(variableValue)
        } else null
    }

    /**
     * Get the Double value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Double value of the double single variable feature.
     * Null if the feature or variable could not be found.
     */
    @JvmOverloads
    fun getFeatureVariableDouble(featureKey: String,
                                 variableKey: String,
                                 userId: String,
                                 attributes: Map<String, String> = emptyMap<String, String>()): Double? {
        val variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.DOUBLE
        )
        if (variableValue != null) {
            try {
                return java.lang.Double.parseDouble(variableValue)
            } catch (exception: NumberFormatException) {
                logger.error("NumberFormatException while trying to parse \"" + variableValue +
                        "\" as Double. " + exception)
            }

        }
        return null
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Integer value of the integer single variable feature.
     * Null if the feature or variable could not be found.
     */
    @JvmOverloads
    fun getFeatureVariableInteger(featureKey: String,
                                  variableKey: String,
                                  userId: String,
                                  attributes: Map<String, String> = emptyMap<String, String>()): Int? {
        val variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.INTEGER
        )
        if (variableValue != null) {
            try {
                return Integer.parseInt(variableValue)
            } catch (exception: NumberFormatException) {
                logger.error("NumberFormatException while trying to parse \"" + variableValue +
                        "\" as Integer. " + exception.toString())
            }

        }
        return null
    }

    /**
     * Get the String value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The String value of the string single variable feature.
     * Null if the feature or variable could not be found.
     */
    @JvmOverloads
    fun getFeatureVariableString(featureKey: String,
                                 variableKey: String,
                                 userId: String,
                                 attributes: Map<String, String> = emptyMap<String, String>()): String? {
        return getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.STRING)
    }

    @VisibleForTesting
    internal fun getFeatureVariableValueForType(featureKey: String,
                                                variableKey: String,
                                                userId: String,
                                                attributes: Map<String, String>,
                                                variableType: LiveVariable.VariableType): String? {
        if (featureKey == null) {
            logger.warn("The featureKey parameter must be nonnull.")
            return null
        } else if (variableKey == null) {
            logger.warn("The variableKey parameter must be nonnull.")
            return null
        } else if (userId == null) {
            logger.warn("The userId parameter must be nonnull.")
            return null
        }
        val featureFlag = projectConfig.featureKeyMapping[featureKey]
        if (featureFlag == null) {
            logger.info("No feature flag was found for key \"{}\".", featureKey)
            return null
        }

        val variable = featureFlag.variableKeyToLiveVariableMap[variableKey]
        if (variable == null) {
            logger.info("No feature variable was found for key \"{}\" in feature flag \"{}\".",
                    variableKey, featureKey)
            return null
        } else if (variable.type != variableType) {
            logger.info("The feature variable \"" + variableKey +
                    "\" is actually of type \"" + variable.type.toString() +
                    "\" type. You tried to access it as type \"" + variableType.toString() +
                    "\". Please use the appropriate feature variable accessor.")
            return null
        }

        var variableValue = variable.defaultValue

        val featureDecision = decisionService.getVariationForFeature(featureFlag, userId, attributes)
        if (featureDecision.variation != null) {
            val liveVariableUsageInstance = featureDecision.variation!!.variableIdToLiveVariableUsageInstanceMap[variable.id]
            if (liveVariableUsageInstance != null) {
                variableValue = liveVariableUsageInstance.value
            } else {
                variableValue = variable.defaultValue
            }
        } else {
            logger.info("User \"{}\" was not bucketed into any variation for feature flag \"{}\". " + "The default value \"{}\" for \"{}\" is being returned.",
                    userId, featureKey, variableValue, variableKey
            )
        }

        return variableValue
    }

    /**
     * Get the list of features that are enabled for the user.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return List of the feature keys that are enabled for the user if the userId is empty it will
     * return Empty List.
     */
    fun getEnabledFeatures(userId: String, attributes: MutableMap<String, String>): List<String> {
        val enabledFeaturesList = ArrayList<String>()

        if (!validateUserId(userId)) {
            return enabledFeaturesList
        }

        for (featureFlag in projectConfig.featureFlags) {
            val featureKey = featureFlag.key
            if (isFeatureEnabled(featureKey, userId, attributes))
                enabledFeaturesList.add(featureKey)
        }

        return enabledFeaturesList
    }

    @Throws(UnknownExperimentException::class)
    @JvmOverloads
    fun getVariation(experiment: Experiment,
                     userId: String,
                     attributes: Map<String, String> = emptyMap<String, String>()): Variation? {

        val filteredAttributes = filterAttributes(projectConfig, attributes)

        return decisionService.getVariation(experiment, userId, filteredAttributes)
    }

    @JvmOverloads
    fun getVariation(experimentKey: String,
                     userId: String,
                     attributes: Map<String, String> = emptyMap<String, String>()): Variation? {
        if (!validateUserId(userId)) {
            return null
        }

        if (experimentKey == null || experimentKey.trim { it <= ' ' }.isEmpty()) {
            logger.error("The experimentKey parameter must be nonnull.")
            return null
        }

        val currentConfig = projectConfig

        val experiment = currentConfig.getExperimentForKey(experimentKey, errorHandler) ?: // if we're unable to retrieve the associated experiment, return null
                return null

        val filteredAttributes = filterAttributes(projectConfig, attributes)

        return decisionService.getVariation(experiment, userId, filteredAttributes)
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     * If the variationKey is not in the experiment, this call fails.
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     * @param variationKey The variation key to force the user into.  If the variation key is null
     * then the forcedVariation for that experiment is removed.
     *
     * @return boolean A boolean value that indicates if the set completed successfully.
     */
    fun setForcedVariation(experimentKey: String,
                           userId: String,
                           variationKey: String?): Boolean {


        return projectConfig.setForcedVariation(experimentKey, userId, variationKey)
    }

    /**
     * Gets the forced variation for a given user and experiment.
     * This method just calls into the [com.optimizely.ab.config.ProjectConfig.getForcedVariation]
     * method of the same signature.
     *
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     *
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    fun getForcedVariation(experimentKey: String,
                           userId: String): Variation? {
        return projectConfig.getForcedVariation(experimentKey, userId)
    }

    //======== Helper methods ========//

    /**
     * Helper method to verify that the given attributes map contains only keys that are present in the
     * [ProjectConfig].
     *
     * @param projectConfig the current project config
     * @param attributes the attributes map to validate and potentially filter. Attributes which starts with reserved key
     * [ProjectConfig.RESERVED_ATTRIBUTE_PREFIX] are kept.
     * @return the filtered attributes map (containing only attributes that are present in the project config) or an
     * empty map if a null attributes object is passed in
     */
    private fun filterAttributes(projectConfig: ProjectConfig,
                                 attributes: Map<String, String>): Map<String, String> {
        var attributes = attributes
        if (attributes == null) {
            logger.warn("Attributes is null when non-null was expected. Defaulting to an empty attributes map.")
            return emptyMap<String, String>()
        }

        var unknownAttributes: MutableList<String>? = null

        val attributeKeyMapping = projectConfig.attributeKeyMapping
        for ((key) in attributes) {
            if (!attributeKeyMapping.containsKey(key) && !key.startsWith(ProjectConfig.RESERVED_ATTRIBUTE_PREFIX)) {
                if (unknownAttributes == null) {
                    unknownAttributes = ArrayList()
                }
                unknownAttributes.add(key)
            }
        }

        if (unknownAttributes != null) {
            logger.warn("Attribute(s) {} not in the datafile.", unknownAttributes)
            // make a copy of the passed through attributes, then remove the unknown list
            attributes = HashMap(attributes)
            for (unknownAttribute in unknownAttributes) {
                attributes.remove(unknownAttribute)
            }
        }

        return attributes
    }

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private fun validateUserId(userId: String?): Boolean {
        if (userId == null) {
            logger.error("The user ID parameter must be nonnull.")
            return false
        }
        if (userId.trim { it <= ' ' }.isEmpty()) {
            logger.error("Non-empty user ID required")
            return false
        }

        return true
    }

    /**
     * [Optimizely] instance builder.
     *
     *
     * **NOTE**, the default value for [.eventHandler] is a [NoOpErrorHandler] instance, meaning that the
     * created [Optimizely] object will **NOT** throw exceptions unless otherwise specified.
     *
     * @see .builder
     */
    class Builder(private val datafile: String,
                  private val eventHandler: EventHandler) {
        private var bucketer: Bucketer? = null
        private var decisionService: DecisionService? = null
        private var errorHandler: ErrorHandler? = null
        private var eventBuilder: EventBuilder? = null
        private var clientEngine: ClientEngine? = null
        private var clientVersion: String? = null
        private var projectConfig: ProjectConfig? = null
        private var userProfileService: UserProfileService? = null

        fun withBucketing(bucketer: Bucketer): Builder {
            this.bucketer = bucketer
            return this
        }

        fun withDecisionService(decisionService: DecisionService): Builder {
            this.decisionService = decisionService
            return this
        }

        fun withErrorHandler(errorHandler: ErrorHandler): Builder {
            this.errorHandler = errorHandler
            return this
        }

        fun withUserProfileService(userProfileService: UserProfileService): Builder {
            this.userProfileService = userProfileService
            return this
        }

        fun withClientEngine(clientEngine: ClientEngine): Builder {
            this.clientEngine = clientEngine
            return this
        }

        fun withClientVersion(clientVersion: String): Builder {
            this.clientVersion = clientVersion
            return this
        }

        fun withEventBuilder(eventBuilder: EventBuilder): Builder {
            this.eventBuilder = eventBuilder
            return this
        }

        // Helper function for making testing easier
        fun withConfig(projectConfig: ProjectConfig): Builder {
            this.projectConfig = projectConfig
            return this
        }

        @Throws(ConfigParseException::class)
        fun build(): Optimizely {
            if (projectConfig == null) {
                projectConfig = Optimizely.getProjectConfig(datafile)
            }

            if (bucketer == null && projectConfig != null) {
                bucketer = Bucketer(projectConfig!!)
            }

            if (clientEngine == null) {
                clientEngine = ClientEngine.JAVA_SDK
            }

            if (clientVersion == null) {
                clientVersion = BuildVersionInfo.VERSION
            }


            if (eventBuilder == null && clientEngine != null && clientVersion != null) {
                eventBuilder = EventBuilder(clientEngine!!, clientVersion!!)
            }

            if (errorHandler == null) {
                errorHandler = NoOpErrorHandler()
            }

            if (decisionService == null) {
                decisionService = DecisionService(bucketer!!, errorHandler!!, projectConfig!!, userProfileService)
            }

            val optimizely = Optimizely(projectConfig!!, decisionService!!, eventHandler, eventBuilder!!, errorHandler!!, userProfileService)
            optimizely.initialize()
            return optimizely
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Optimizely::class.java!!)

        /**
         * @return a [ProjectConfig] instance given a json string
         */
        @Throws(ConfigParseException::class)
        private fun getProjectConfig(datafile: String?): ProjectConfig {
            if (datafile == null) {
                throw ConfigParseException("Unable to parse null datafile.")
            }
            if (datafile.length == 0) {
                throw ConfigParseException("Unable to parse empty datafile.")
            }

            val projectConfig = DefaultConfigParser.instance.parseProjectConfig(datafile)

            if (projectConfig.version == "1") {
                throw ConfigParseException("This version of the Java SDK does not support version 1 datafiles. " + "Please use a version 2 or 3 datafile with this SDK.")
            }

            return projectConfig
        }

        //======== Builder ========//

        fun builder(datafile: String,
                    eventHandler: EventHandler): Builder {
            return Builder(datafile, eventHandler)
        }
    }
}//======== activate calls ========//
//======== FeatureFlag APIs ========//
/**
 * Determine whether a boolean feature is enabled.
 * Send an impression event if the user is bucketed into an experiment using the feature.
 *
 * @param featureKey The unique key of the feature.
 * @param userId The ID of the user.
 * @return True if the feature is enabled.
 * False if the feature is disabled.
 * False if the feature is not found.
 */
/**
 * Get the Boolean value of the specified variable in the feature.
 * @param featureKey The unique key of the feature.
 * @param variableKey The unique key of the variable.
 * @param userId The ID of the user.
 * @return The Boolean value of the boolean single variable feature.
 * Null if the feature could not be found.
 */
/**
 * Get the Double value of the specified variable in the feature.
 * @param featureKey The unique key of the feature.
 * @param variableKey The unique key of the variable.
 * @param userId The ID of the user.
 * @return The Double value of the double single variable feature.
 * Null if the feature or variable could not be found.
 */
/**
 * Get the Integer value of the specified variable in the feature.
 * @param featureKey The unique key of the feature.
 * @param variableKey The unique key of the variable.
 * @param userId The ID of the user.
 * @return The Integer value of the integer single variable feature.
 * Null if the feature or variable could not be found.
 */
/**
 * Get the String value of the specified variable in the feature.
 * @param featureKey The unique key of the feature.
 * @param variableKey The unique key of the variable.
 * @param userId The ID of the user.
 * @return The String value of the string single variable feature.
 * Null if the feature or variable could not be found.
 */
//======== getVariation calls ========//
