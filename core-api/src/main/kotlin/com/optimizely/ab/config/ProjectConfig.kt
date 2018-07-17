/**
 *
 * Copyright 2016-2018, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.optimizely.ab.UnknownEventTypeException
import com.optimizely.ab.UnknownExperimentException
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.error.NoOpErrorHandler
import com.optimizely.ab.error.RaiseExceptionErrorHandler
import com.optimizely.ab.internal.ControlAttribute
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.concurrent.Immutable
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents the Optimizely Project configuration.
 *
 * @see [Project JSON](http://developers.optimizely.com/server/reference/index.html.json)
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
class ProjectConfig// v4 constructor
(// ProjectConfig properties
        val accountId: String,
        val anonymizeIP: Boolean,
        val botFiltering: Boolean?,
        val projectId: String,
        val revision: String,
        val version: String,
        attributes: List<Attribute>,
        audiences: List<Audience>,
        events: List<EventType>,
        experiments: List<Experiment>,
        featureFlags: List<FeatureFlag>?,
        groups: List<Group>,
        liveVariables: List<LiveVariable>?,
        rollouts: List<Rollout>?) {

    enum class Version private constructor(private val version: String) {
        V2("2"),
        V3("3"),
        V4("4");

        override fun toString(): String {
            return version
        }
    }

    val attributes: List<Attribute>
    val audiences: List<Audience>
    val eventTypes: List<EventType>
    val experiments: List<Experiment>
    val featureFlags: List<FeatureFlag>
    val groups: List<Group>
    val liveVariables: List<LiveVariable>?
    val rollouts: List<Rollout>

    // key to entity mappings
    val attributeKeyMapping: Map<String, Attribute>
    val eventNameMapping: Map<String, EventType>
    val experimentKeyMapping: Map<String, Experiment>
    val featureKeyMapping: Map<String, FeatureFlag>
    val liveVariableKeyMapping: Map<String, LiveVariable>

    // id to entity mappings
    val audienceIdMapping: Map<String, Audience>
    val experimentIdMapping: Map<String, Experiment>
    val groupIdMapping: Map<String, Group>
    val rolloutIdMapping: Map<String, Rollout>

    // other mappings
    val liveVariableIdToExperimentsMapping: Map<String, List<Experiment>>
    val variationToLiveVariableUsageInstanceMapping: Map<String, Map<String, LiveVariableUsageInstance>>
    private val variationIdToExperimentMapping: Map<String, Experiment>

    /**
     * Forced variations supersede any other mappings.  They are transient and are not persistent or part of
     * the actual datafile. This contains all the forced variations
     * set by the user by calling [ProjectConfig.setForcedVariation] (it is not the same as the
     * whitelisting forcedVariations data structure in the Experiments class).
     */
    @Transient
    val forcedVariationMapping = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    // v3 constructor
    @JvmOverloads constructor(accountId: String, projectId: String, version: String, revision: String, groups: List<Group>,
                              experiments: List<Experiment>, attributes: List<Attribute>, eventType: List<EventType>,
                              audiences: List<Audience>, anonymizeIP: Boolean = false, liveVariables: List<LiveVariable>? = null) : this(
            accountId,
            anonymizeIP, null,
            projectId,
            revision,
            version,
            attributes,
            audiences,
            eventType,
            experiments, null,
            groups,
            liveVariables, null
    ) {
    }

    init {

        this.attributes = Collections.unmodifiableList(attributes)
        this.audiences = Collections.unmodifiableList(audiences)
        this.eventTypes = Collections.unmodifiableList(events)
        if (featureFlags == null) {
            this.featureFlags = emptyList<FeatureFlag>()
        } else {
            this.featureFlags = Collections.unmodifiableList(featureFlags)
        }
        if (rollouts == null) {
            this.rollouts = emptyList<Rollout>()
        } else {
            this.rollouts = Collections.unmodifiableList(rollouts)
        }

        this.groups = Collections.unmodifiableList(groups)

        val allExperiments = ArrayList<Experiment>()
        allExperiments.addAll(experiments)
        allExperiments.addAll(aggregateGroupExperiments(groups))
        this.experiments = Collections.unmodifiableList(allExperiments)

        val variationIdToExperimentMap = HashMap<String, Experiment>()
        for (experiment in this.experiments) {
            for (variation in experiment.variations) {
                variationIdToExperimentMap.put(variation.id, experiment)
            }
        }
        this.variationIdToExperimentMapping = Collections.unmodifiableMap(variationIdToExperimentMap)

        // generate the name mappers
        this.attributeKeyMapping = ProjectConfigUtils.generateNameMapping(attributes)
        this.eventNameMapping = ProjectConfigUtils.generateNameMapping(this.eventTypes)
        this.experimentKeyMapping = ProjectConfigUtils.generateNameMapping(this.experiments)
        this.featureKeyMapping = ProjectConfigUtils.generateNameMapping(this.featureFlags)

        // generate audience id to audience mapping
        this.audienceIdMapping = ProjectConfigUtils.generateIdMapping(audiences)
        this.experimentIdMapping = ProjectConfigUtils.generateIdMapping(this.experiments)
        this.groupIdMapping = ProjectConfigUtils.generateIdMapping(groups)
        this.rolloutIdMapping = ProjectConfigUtils.generateIdMapping(this.rollouts)

        if (liveVariables == null) {
            this.liveVariables = null
            this.liveVariableKeyMapping = emptyMap<String, LiveVariable>()
            this.liveVariableIdToExperimentsMapping = emptyMap<String, List<Experiment>>()
            this.variationToLiveVariableUsageInstanceMapping = emptyMap<String, Map<String, LiveVariableUsageInstance>>()
        } else {
            this.liveVariables = Collections.unmodifiableList(liveVariables)
            this.liveVariableKeyMapping = ProjectConfigUtils.generateNameMapping(this.liveVariables)
            this.liveVariableIdToExperimentsMapping = ProjectConfigUtils.generateLiveVariableIdToExperimentsMapping(this.experiments)
            this.variationToLiveVariableUsageInstanceMapping = ProjectConfigUtils.generateVariationToLiveVariableUsageInstancesMap(this.experiments)
        }
    }

    /**
     * Helper method to retrieve the [Experiment] for the given experiment key.
     * If [RaiseExceptionErrorHandler] is provided, either an experiment is returned,
     * or an exception is sent to the error handler
     * if there are no experiments in the project config with the given experiment key.
     * If [NoOpErrorHandler] is used, either an experiment or `null` is returned.
     *
     * @param experimentKey the experiment to retrieve from the current project config
     * @param errorHandler the error handler to send exceptions to
     * @return the experiment for given experiment key
     */
    fun getExperimentForKey(experimentKey: String,
                            errorHandler: ErrorHandler): Experiment? {

        val experiment = experimentKeyMapping[experimentKey]

        // if the given experiment key isn't present in the config, log an exception to the error handler
        if (experiment == null) {
            val unknownExperimentError = String.format("Experiment \"%s\" is not in the datafile.", experimentKey)
            logger.error(unknownExperimentError)
            errorHandler.handleError(UnknownExperimentException(unknownExperimentError))
        }

        return experiment
    }

    /**
     * Helper method to retrieve the [EventType] for the given event name.
     * If [RaiseExceptionErrorHandler] is provided, either an event type is returned,
     * or an exception is sent to the error handler if there are no event types in the project config with the given name.
     * If [NoOpErrorHandler] is used, either an event type or `null` is returned.
     *
     * @param eventName the event type to retrieve from the current project config
     * @param errorHandler the error handler to send exceptions to
     * @return the event type for the given event name
     */
    fun getEventTypeForName(eventName: String, errorHandler: ErrorHandler): EventType? {

        val eventType = eventNameMapping[eventName]

        // if the given event name isn't present in the config, log an exception to the error handler
        if (eventType == null) {
            val unknownEventTypeError = String.format("Event \"%s\" is not in the datafile.", eventName)
            logger.error(unknownEventTypeError)
            errorHandler.handleError(UnknownEventTypeException(unknownEventTypeError))
        }

        return eventType
    }


    fun getExperimentForVariationId(variationId: String): Experiment? {
        return this.variationIdToExperimentMapping[variationId]
    }

    private fun aggregateGroupExperiments(groups: List<Group>): List<Experiment> {
        val groupExperiments = ArrayList<Experiment>()
        for (group in groups) {
            groupExperiments.addAll(group.experiments)
        }

        return groupExperiments
    }

    /**
     * Checks is attributeKey is reserved or not and if it exist in attributeKeyMapping
     * @param attributeKey
     * @return AttributeId corresponding to AttributeKeyMapping, AttributeKey when it's a reserved attribute and
     * null when attributeKey is equal to BOT_FILTERING_ATTRIBUTE key.
     */
    fun getAttributeId(projectConfig: ProjectConfig, attributeKey: String): String? {
        var attributeIdOrKey: String? = null
        val attribute = projectConfig.attributeKeyMapping[attributeKey]
        val hasReservedPrefix = attributeKey.startsWith(RESERVED_ATTRIBUTE_PREFIX)
        if (attribute != null) {
            if (hasReservedPrefix) {
                logger.warn("Attribute {} unexpectedly has reserved prefix {}; using attribute ID instead of reserved attribute name.",
                        attributeKey, RESERVED_ATTRIBUTE_PREFIX)
            }
            attributeIdOrKey = attribute.id
        } else if (hasReservedPrefix) {
            attributeIdOrKey = attributeKey
        } else {
            logger.debug("Unrecognized Attribute \"{}\"", attributeKey)
        }
        return attributeIdOrKey
    }

    fun getExperimentsForEventKey(eventKey: String): List<Experiment> {
        val event = eventNameMapping[eventKey]
        if (event != null) {
            val experimentIds = event.experimentIds
            val experiments = ArrayList<Experiment>(experimentIds.size)
            for (experimentId in experimentIds) {
                experimentIdMapping.let {
                    if (it.get(experimentId) != null) {
                        experiments.add(it.get(experimentId)!!)
                    }
                }
            }

            return experiments
        }

        return emptyList<Experiment>()
    }

    fun getAudienceConditionsFromId(audienceId: String): Condition? {
        val audience = audienceIdMapping[audienceId]

        return audience?.conditions
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     *
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

        // if the experiment is not a valid experiment key, don't set it.
        val experiment = experimentKeyMapping[experimentKey]
        if (experiment == null) {
            logger.error("Experiment {} does not exist in ProjectConfig for project {}", experimentKey, projectId)
            return false
        }

        var variation: Variation? = null

        // keep in mind that you can pass in a variationKey that is null if you want to
        // remove the variation.
        if (variationKey != null) {
            variation = experiment.variationKeyToVariationMap[variationKey]
            // if the variation is not part of the experiment, return false.
            if (variation == null) {
                logger.error("Variation {} does not exist for experiment {}", variationKey, experimentKey)
                return false
            }
        }

        // if the user id is invalid, return false.
        if (userId == null || userId.trim { it <= ' ' }.isEmpty()) {
            logger.error("User ID is invalid")
            return false
        }

        val experimentToVariation: ConcurrentHashMap<String, String>
        if (!forcedVariationMapping.containsKey(userId)) {
            (forcedVariationMapping as java.util.Map<String, ConcurrentHashMap<String, String>>).putIfAbsent(userId, ConcurrentHashMap())
        }
        experimentToVariation = forcedVariationMapping[userId]!!

        var retVal = true
        // if it is null remove the variation if it exists.
        if (variationKey == null) {
            val removedVariationId = experimentToVariation.remove(experiment.id)
            if (removedVariationId != null) {
                val removedVariation = experiment.variationIdToVariationMap[removedVariationId]
                if (removedVariation != null) {
                    logger.debug("Variation mapped to experiment \"{}\" has been removed for user \"{}\"", experiment.key, userId)
                } else {
                    logger.debug("Removed forced variation that did not exist in experiment")
                }
            } else {
                logger.debug("No variation for experiment {}", experimentKey)
                retVal = false
            }
        } else {
            val previous = experimentToVariation.put(experiment.id, variation!!.id)
            logger.debug("Set variation \"{}\" for experiment \"{}\" and user \"{}\" in the forced variation map.",
                    variation.key, experiment.key, userId)
            if (previous != null) {
                val previousVariation = experiment.variationIdToVariationMap[previous]
                if (previousVariation != null) {
                    logger.debug("forced variation {} replaced forced variation {} in forced variation map.",
                            variation.key, previousVariation.key)
                }
            }
        }

        return retVal
    }

    /**
     * Gets the forced variation for a given user and experiment.
     *
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     *
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    fun getForcedVariation(experimentKey: String,
                           userId: String): Variation? {

        // if the user id is invalid, return false.
        if (userId == null || userId.trim { it <= ' ' }.isEmpty()) {
            logger.error("User ID is invalid")
            return null
        }

        if (experimentKey == null || experimentKey.isEmpty()) {
            logger.error("experiment key is invalid")
            return null
        }

        val experimentToVariation = forcedVariationMapping[userId]
        if (experimentToVariation != null) {
            val experiment = experimentKeyMapping[experimentKey]
            if (experiment == null) {
                logger.debug("No experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experimentKey, userId)
                return null
            }
            val variationId = experimentToVariation[experiment.id]
            if (variationId != null) {
                val variation = experiment.variationIdToVariationMap[variationId]
                if (variation != null) {
                    logger.debug("Variation \"{}\" is mapped to experiment \"{}\" and user \"{}\" in the forced variation map",
                            variation.key, experimentKey, userId)
                    return variation
                }
            } else {
                logger.debug("No variation for experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experimentKey, userId)
            }
        }
        return null
    }

    override fun toString(): String {
        return "ProjectConfig{" +
                "accountId='" + accountId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", revision='" + revision + '\'' +
                ", version='" + version + '\'' +
                ", anonymizeIP=" + anonymizeIP +
                ", botFiltering=" + botFiltering +
                ", attributes=" + attributes +
                ", audiences=" + audiences +
                ", events=" + eventTypes +
                ", experiments=" + experiments +
                ", featureFlags=" + featureFlags +
                ", groups=" + groups +
                ", liveVariables=" + liveVariables +
                ", rollouts=" + rollouts +
                ", attributeKeyMapping=" + attributeKeyMapping +
                ", eventNameMapping=" + eventNameMapping +
                ", experimentKeyMapping=" + experimentKeyMapping +
                ", featureKeyMapping=" + featureKeyMapping +
                ", liveVariableKeyMapping=" + liveVariableKeyMapping +
                ", audienceIdMapping=" + audienceIdMapping +
                ", experimentIdMapping=" + experimentIdMapping +
                ", groupIdMapping=" + groupIdMapping +
                ", rolloutIdMapping=" + rolloutIdMapping +
                ", liveVariableIdToExperimentsMapping=" + liveVariableIdToExperimentsMapping +
                ", variationToLiveVariableUsageInstanceMapping=" + variationToLiveVariableUsageInstanceMapping +
                ", forcedVariationMapping=" + forcedVariationMapping +
                ", variationIdToExperimentMapping=" + variationIdToExperimentMapping +
                '}'
    }

    companion object {

        // logger
        private val logger = LoggerFactory.getLogger(ProjectConfig::class.java!!)

        val RESERVED_ATTRIBUTE_PREFIX = "\$opt_"
    }
}// v2 constructor
