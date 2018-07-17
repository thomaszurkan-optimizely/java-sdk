/**
 *
 * Copyright 2017-2018, Optimizely and contributors
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

import com.optimizely.ab.config.audience.AndCondition
import com.optimizely.ab.config.audience.Audience
import com.optimizely.ab.config.audience.Condition
import com.optimizely.ab.config.audience.OrCondition
import com.optimizely.ab.config.audience.UserAttribute

import java.util.ArrayList
import java.util.Collections

object ValidProjectConfigV4 {

    // simple properties
    private val ACCOUNT_ID = "2360254204"
    private val ANONYMIZE_IP = true
    private val BOT_FILTERING = true
    private val PROJECT_ID = "3918735994"
    private val REVISION = "1480511547"
    private val VERSION = "4"

    // attributes
    private val ATTRIBUTE_HOUSE_ID = "553339214"
    val ATTRIBUTE_HOUSE_KEY = "house"
    private val ATTRIBUTE_HOUSE = Attribute(ATTRIBUTE_HOUSE_ID, ATTRIBUTE_HOUSE_KEY)

    private val ATTRIBUTE_NATIONALITY_ID = "58339410"
    val ATTRIBUTE_NATIONALITY_KEY = "nationality"
    private val ATTRIBUTE_NATIONALITY = Attribute(ATTRIBUTE_NATIONALITY_ID, ATTRIBUTE_NATIONALITY_KEY)

    private val ATTRIBUTE_OPT_ID = "583394100"
    val ATTRIBUTE_OPT_KEY = "\$opt_test"
    private val ATTRIBUTE_OPT = Attribute(ATTRIBUTE_OPT_ID, ATTRIBUTE_OPT_KEY)

    // audiences
    private val CUSTOM_DIMENSION_TYPE = "custom_dimension"
    private val AUDIENCE_GRYFFINDOR_ID = "3468206642"
    private val AUDIENCE_GRYFFINDOR_KEY = "Gryffindors"
    val AUDIENCE_GRYFFINDOR_VALUE = "Gryffindor"
    private val AUDIENCE_GRYFFINDOR = Audience(
            AUDIENCE_GRYFFINDOR_ID,
            AUDIENCE_GRYFFINDOR_KEY,
            AndCondition(listOf<Condition>(OrCondition(listOf<Condition>(OrCondition(listOf<Condition>(UserAttribute(ATTRIBUTE_HOUSE_KEY,
                    CUSTOM_DIMENSION_TYPE,
                    AUDIENCE_GRYFFINDOR_VALUE) as Condition))))))
    )
    private val AUDIENCE_SLYTHERIN_ID = "3988293898"
    private val AUDIENCE_SLYTHERIN_KEY = "Slytherins"
    val AUDIENCE_SLYTHERIN_VALUE = "Slytherin"
    private val AUDIENCE_SLYTHERIN = Audience(
            AUDIENCE_SLYTHERIN_ID,
            AUDIENCE_SLYTHERIN_KEY,
            AndCondition(listOf<Condition>(OrCondition(listOf<Condition>(OrCondition(listOf<Condition>(UserAttribute(ATTRIBUTE_HOUSE_KEY,
                    CUSTOM_DIMENSION_TYPE,
                    AUDIENCE_SLYTHERIN_VALUE) as Condition))))))
    )

    private val AUDIENCE_ENGLISH_CITIZENS_ID = "4194404272"
    private val AUDIENCE_ENGLISH_CITIZENS_KEY = "english_citizens"
    val AUDIENCE_ENGLISH_CITIZENS_VALUE = "English"
    private val AUDIENCE_ENGLISH_CITIZENS = Audience(
            AUDIENCE_ENGLISH_CITIZENS_ID,
            AUDIENCE_ENGLISH_CITIZENS_KEY,
            AndCondition(listOf<Condition>(OrCondition(listOf<Condition>(OrCondition(listOf<Condition>(UserAttribute(ATTRIBUTE_NATIONALITY_KEY,
                    CUSTOM_DIMENSION_TYPE,
                    AUDIENCE_ENGLISH_CITIZENS_VALUE) as Condition))))))
    )
    private val AUDIENCE_WITH_MISSING_VALUE_ID = "2196265320"
    private val AUDIENCE_WITH_MISSING_VALUE_KEY = "audience_with_missing_value"
    val AUDIENCE_WITH_MISSING_VALUE_VALUE = "English"
    private val ATTRIBUTE_WITH_VALUE = UserAttribute(
            ATTRIBUTE_NATIONALITY_KEY,
            CUSTOM_DIMENSION_TYPE,
            AUDIENCE_WITH_MISSING_VALUE_VALUE
    )
    private val ATTRIBUTE_WITHOUT_VALUE = UserAttribute(
            ATTRIBUTE_NATIONALITY_KEY,
            CUSTOM_DIMENSION_TYPE,
            null
    )
    private val AUDIENCE_WITH_MISSING_VALUE = Audience(
            AUDIENCE_WITH_MISSING_VALUE_ID,
            AUDIENCE_WITH_MISSING_VALUE_KEY,
            AndCondition(listOf<Condition>(OrCondition(listOf<Condition>(OrCondition(ProjectConfigTestUtils.createListOfObjects<Condition>(
                    ATTRIBUTE_WITH_VALUE,
                    ATTRIBUTE_WITHOUT_VALUE
            ))))))
    )

    // features
    private val FEATURE_BOOLEAN_FEATURE_ID = "4195505407"
    private val FEATURE_BOOLEAN_FEATURE_KEY = "boolean_feature"
    private val FEATURE_FLAG_BOOLEAN_FEATURE = FeatureFlag(
            FEATURE_BOOLEAN_FEATURE_ID,
            FEATURE_BOOLEAN_FEATURE_KEY,
            "",
            emptyList<String>(),
            emptyList<LiveVariable>()
    )
    private val FEATURE_SINGLE_VARIABLE_DOUBLE_ID = "3926744821"
    val FEATURE_SINGLE_VARIABLE_DOUBLE_KEY = "double_single_variable_feature"
    private val VARIABLE_DOUBLE_VARIABLE_ID = "4111654444"
    val VARIABLE_DOUBLE_VARIABLE_KEY = "double_variable"
    val VARIABLE_DOUBLE_DEFAULT_VALUE = "14.99"
    private val VARIABLE_DOUBLE_VARIABLE = LiveVariable(
            VARIABLE_DOUBLE_VARIABLE_ID,
            VARIABLE_DOUBLE_VARIABLE_KEY,
            VARIABLE_DOUBLE_DEFAULT_VALUE, null,
            LiveVariable.VariableType.DOUBLE
    )
    private val FEATURE_SINGLE_VARIABLE_INTEGER_ID = "3281420120"
    val FEATURE_SINGLE_VARIABLE_INTEGER_KEY = "integer_single_variable_feature"
    private val VARIABLE_INTEGER_VARIABLE_ID = "593964691"
    val VARIABLE_INTEGER_VARIABLE_KEY = "integer_variable"
    private val VARIABLE_INTEGER_DEFAULT_VALUE = "7"
    private val VARIABLE_INTEGER_VARIABLE = LiveVariable(
            VARIABLE_INTEGER_VARIABLE_ID,
            VARIABLE_INTEGER_VARIABLE_KEY,
            VARIABLE_INTEGER_DEFAULT_VALUE, null,
            LiveVariable.VariableType.INTEGER
    )
    private val FEATURE_SINGLE_VARIABLE_BOOLEAN_ID = "2591051011"
    val FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY = "boolean_single_variable_feature"
    private val VARIABLE_BOOLEAN_VARIABLE_ID = "3974680341"
    val VARIABLE_BOOLEAN_VARIABLE_KEY = "boolean_variable"
    val VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE = "true"
    private val VARIABLE_BOOLEAN_VARIABLE = LiveVariable(
            VARIABLE_BOOLEAN_VARIABLE_ID,
            VARIABLE_BOOLEAN_VARIABLE_KEY,
            VARIABLE_BOOLEAN_VARIABLE_DEFAULT_VALUE, null,
            LiveVariable.VariableType.BOOLEAN
    )
    private val FEATURE_FLAG_SINGLE_VARIABLE_BOOLEAN = FeatureFlag(
            FEATURE_SINGLE_VARIABLE_BOOLEAN_ID,
            FEATURE_SINGLE_VARIABLE_BOOLEAN_KEY,
            "",
            emptyList<String>(),
            listOf<LiveVariable>(VARIABLE_BOOLEAN_VARIABLE)
    )
    private val FEATURE_SINGLE_VARIABLE_STRING_ID = "2079378557"
    val FEATURE_SINGLE_VARIABLE_STRING_KEY = "string_single_variable_feature"
    private val VARIABLE_STRING_VARIABLE_ID = "2077511132"
    val VARIABLE_STRING_VARIABLE_KEY = "string_variable"
    val VARIABLE_STRING_VARIABLE_DEFAULT_VALUE = "wingardium leviosa"
    private val VARIABLE_STRING_VARIABLE = LiveVariable(
            VARIABLE_STRING_VARIABLE_ID,
            VARIABLE_STRING_VARIABLE_KEY,
            VARIABLE_STRING_VARIABLE_DEFAULT_VALUE, null,
            LiveVariable.VariableType.STRING
    )
    private val ROLLOUT_1_ID = "1058508303"
    private val ROLLOUT_1_EVERYONE_ELSE_EXPERIMENT_ID = "1785077004"
    private val ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID = "1566407342"
    private val ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_STRING_VALUE = "lumos"
    private val ROLLOUT_1_FEATURE_ENABLED_VALUE = true
    private val ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION = Variation(
            ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
            ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
            ROLLOUT_1_FEATURE_ENABLED_VALUE,
            listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance(
                    VARIABLE_STRING_VARIABLE_ID,
                    ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_STRING_VALUE
            ))
    )
    private val ROLLOUT_1_EVERYONE_ELSE_RULE = Experiment(
            ROLLOUT_1_EVERYONE_ELSE_EXPERIMENT_ID,
            ROLLOUT_1_EVERYONE_ELSE_EXPERIMENT_ID,
            Experiment.ExperimentStatus.RUNNING.toString(),
            ROLLOUT_1_ID,
            emptyList<String>(),
            listOf<Variation>(ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    ROLLOUT_1_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
                    5000
            ))
    )
    val ROLLOUT_1 = Rollout(
            ROLLOUT_1_ID,
            listOf<Experiment>(ROLLOUT_1_EVERYONE_ELSE_RULE)
    )
    val FEATURE_FLAG_SINGLE_VARIABLE_STRING = FeatureFlag(
            FEATURE_SINGLE_VARIABLE_STRING_ID,
            FEATURE_SINGLE_VARIABLE_STRING_KEY,
            ROLLOUT_1_ID,
            emptyList<String>(),
            listOf<LiveVariable>(VARIABLE_STRING_VARIABLE)
    )
    private val ROLLOUT_3_ID = "2048875663"
    private val ROLLOUT_3_EVERYONE_ELSE_EXPERIMENT_ID = "3794675122"
    private val ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID = "589640735"
    private val ROLLOUT_3_FEATURE_ENABLED_VALUE = true
    val ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION = Variation(
            ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
            ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
            ROLLOUT_3_FEATURE_ENABLED_VALUE,
            emptyList<LiveVariableUsageInstance>()
    )
    val ROLLOUT_3_EVERYONE_ELSE_RULE = Experiment(
            ROLLOUT_3_EVERYONE_ELSE_EXPERIMENT_ID,
            ROLLOUT_3_EVERYONE_ELSE_EXPERIMENT_ID,
            Experiment.ExperimentStatus.RUNNING.toString(),
            ROLLOUT_3_ID,
            emptyList<String>(),
            listOf<Variation>(ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    ROLLOUT_3_EVERYONE_ELSE_RULE_ENABLED_VARIATION_ID,
                    10000
            ))
    )
    val ROLLOUT_3 = Rollout(
            ROLLOUT_3_ID,
            listOf<Experiment>(ROLLOUT_3_EVERYONE_ELSE_RULE)
    )

    private val FEATURE_MULTI_VARIATE_FEATURE_ID = "3263342226"
    val FEATURE_MULTI_VARIATE_FEATURE_KEY = "multi_variate_feature"
    private val VARIABLE_FIRST_LETTER_ID = "675244127"
    val VARIABLE_FIRST_LETTER_KEY = "first_letter"
    val VARIABLE_FIRST_LETTER_DEFAULT_VALUE = "H"
    private val VARIABLE_FIRST_LETTER_VARIABLE = LiveVariable(
            VARIABLE_FIRST_LETTER_ID,
            VARIABLE_FIRST_LETTER_KEY,
            VARIABLE_FIRST_LETTER_DEFAULT_VALUE, null,
            LiveVariable.VariableType.STRING
    )
    private val VARIABLE_REST_OF_NAME_ID = "4052219963"
    private val VARIABLE_REST_OF_NAME_KEY = "rest_of_name"
    private val VARIABLE_REST_OF_NAME_DEFAULT_VALUE = "arry"
    private val VARIABLE_REST_OF_NAME_VARIABLE = LiveVariable(
            VARIABLE_REST_OF_NAME_ID,
            VARIABLE_REST_OF_NAME_KEY,
            VARIABLE_REST_OF_NAME_DEFAULT_VALUE, null,
            LiveVariable.VariableType.STRING
    )
    private val FEATURE_MUTEX_GROUP_FEATURE_ID = "3263342226"
    val FEATURE_MUTEX_GROUP_FEATURE_KEY = "mutex_group_feature"
    private val VARIABLE_CORRELATING_VARIATION_NAME_ID = "2059187672"
    private val VARIABLE_CORRELATING_VARIATION_NAME_KEY = "correlating_variation_name"
    private val VARIABLE_CORRELATING_VARIATION_NAME_DEFAULT_VALUE = "null"
    private val VARIABLE_CORRELATING_VARIATION_NAME_VARIABLE = LiveVariable(
            VARIABLE_CORRELATING_VARIATION_NAME_ID,
            VARIABLE_CORRELATING_VARIATION_NAME_KEY,
            VARIABLE_CORRELATING_VARIATION_NAME_DEFAULT_VALUE, null,
            LiveVariable.VariableType.STRING
    )

    // group IDs
    private val GROUP_1_ID = "1015968292"
    private val GROUP_2_ID = "2606208781"

    // experiments
    private val LAYER_BASIC_EXPERIMENT_ID = "1630555626"
    private val EXPERIMENT_BASIC_EXPERIMENT_ID = "1323241596"
    val EXPERIMENT_BASIC_EXPERIMENT_KEY = "basic_experiment"
    private val VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID = "1423767502"
    private val VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY = "A"
    private val VARIATION_BASIC_EXPERIMENT_VARIATION_A = Variation(
            VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID,
            VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID = "3433458314"
    private val VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY = "B"
    private val VARIATION_BASIC_EXPERIMENT_VARIATION_B = Variation(
            VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID,
            VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Harry Potter"
    private val BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Tom Riddle"
    private val EXPERIMENT_BASIC_EXPERIMENT = Experiment(
            EXPERIMENT_BASIC_EXPERIMENT_ID,
            EXPERIMENT_BASIC_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_BASIC_EXPERIMENT_ID,
            emptyList<String>(),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_BASIC_EXPERIMENT_VARIATION_A,
                    VARIATION_BASIC_EXPERIMENT_VARIATION_B
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                            BASIC_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_BASIC_EXPERIMENT_VARIATION_A_KEY,
                            VARIATION_BASIC_EXPERIMENT_VARIATION_B_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            VARIATION_BASIC_EXPERIMENT_VARIATION_A_ID,
                            5000
                    ),
                    TrafficAllocation(
                            VARIATION_BASIC_EXPERIMENT_VARIATION_B_ID,
                            10000
                    )
            )
    )
    private val LAYER_FIRST_GROUPED_EXPERIMENT_ID = "3301900159"
    private val EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID = "2738374745"
    private val EXPERIMENT_FIRST_GROUPED_EXPERIMENT_KEY = "first_grouped_experiment"
    private val VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID = "2377378132"
    private val VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY = "A"
    private val VARIATION_FIRST_GROUPED_EXPERIMENT_A = Variation(
            VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID,
            VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID = "1179171250"
    private val VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY = "B"
    private val VARIATION_FIRST_GROUPED_EXPERIMENT_B = Variation(
            VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID,
            VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Harry Potter"
    private val FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Tom Riddle"
    private val EXPERIMENT_FIRST_GROUPED_EXPERIMENT = Experiment(
            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_FIRST_GROUPED_EXPERIMENT_ID,
            listOf<String>(AUDIENCE_GRYFFINDOR_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_FIRST_GROUPED_EXPERIMENT_A,
                    VARIATION_FIRST_GROUPED_EXPERIMENT_B
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                            FIRST_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_FIRST_GROUPED_EXPERIMENT_A_KEY,
                            VARIATION_FIRST_GROUPED_EXPERIMENT_B_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            VARIATION_FIRST_GROUPED_EXPERIMENT_A_ID,
                            5000
                    ),
                    TrafficAllocation(
                            VARIATION_FIRST_GROUPED_EXPERIMENT_B_ID,
                            10000
                    )
            ),
            GROUP_1_ID
    )
    private val LAYER_SECOND_GROUPED_EXPERIMENT_ID = "2625300442"
    private val EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID = "3042640549"
    private val EXPERIMENT_SECOND_GROUPED_EXPERIMENT_KEY = "second_grouped_experiment"
    private val VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID = "1558539439"
    private val VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY = "A"
    private val VARIATION_SECOND_GROUPED_EXPERIMENT_A = Variation(
            VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID,
            VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID = "2142748370"
    private val VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY = "B"
    private val VARIATION_SECOND_GROUPED_EXPERIMENT_B = Variation(
            VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID,
            VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A = "Hermione Granger"
    private val SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B = "Ronald Weasley"
    private val EXPERIMENT_SECOND_GROUPED_EXPERIMENT = Experiment(
            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_SECOND_GROUPED_EXPERIMENT_ID,
            listOf<String>(AUDIENCE_GRYFFINDOR_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_SECOND_GROUPED_EXPERIMENT_A,
                    VARIATION_SECOND_GROUPED_EXPERIMENT_B
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_A,
                            SECOND_GROUPED_EXPERIMENT_FORCED_VARIATION_USER_ID_VARIATION_B
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_SECOND_GROUPED_EXPERIMENT_A_KEY,
                            VARIATION_SECOND_GROUPED_EXPERIMENT_B_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            VARIATION_SECOND_GROUPED_EXPERIMENT_A_ID,
                            5000
                    ),
                    TrafficAllocation(
                            VARIATION_SECOND_GROUPED_EXPERIMENT_B_ID,
                            10000
                    )
            ),
            GROUP_1_ID
    )
    private val LAYER_MULTIVARIATE_EXPERIMENT_ID = "3780747876"
    private val EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID = "3262035800"
    val EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY = "multivariate_experiment"
    private val VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID = "1880281238"
    private val VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY = "Fred"
    private val VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE = true
    private val VARIATION_MULTIVARIATE_EXPERIMENT_FRED = Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY,
            VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE,
            ProjectConfigTestUtils.createListOfObjects(
                    LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "F"
                    ),
                    LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "red"
                    )
            )
    )
    private val VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID = "3631049532"
    private val VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY = "Feorge"
    private val VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE = Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY,
            VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE,
            ProjectConfigTestUtils.createListOfObjects(
                    LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "F"
                    ),
                    LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "eorge"
                    )
            )
    )
    private val VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID = "4204375027"
    val VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY = "Gred"
    val VARIATION_MULTIVARIATE_VARIATION_FEATURE_ENABLED_GRED_KEY = false
    val VARIATION_MULTIVARIATE_EXPERIMENT_GRED = Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY,
            VARIATION_MULTIVARIATE_VARIATION_FEATURE_ENABLED_GRED_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "G"
                    ),
                    LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "red"
                    )
            )
    )
    private val VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID = "2099211198"
    private val VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY = "George"
    private val VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE = Variation(
            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID,
            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY,
            VARIATION_MULTIVARIATE_FEATURE_ENABLED_VALUE,
            ProjectConfigTestUtils.createListOfObjects(
                    LiveVariableUsageInstance(
                            VARIABLE_FIRST_LETTER_ID,
                            "G"
                    ),
                    LiveVariableUsageInstance(
                            VARIABLE_REST_OF_NAME_ID,
                            "eorge"
                    )
            )
    )
    private val MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FRED = "Fred"
    private val MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FEORGE = "Feorge"
    val MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED = "Gred"
    private val MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GEORGE = "George"
    private val EXPERIMENT_MULTIVARIATE_EXPERIMENT = Experiment(
            EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID,
            EXPERIMENT_MULTIVARIATE_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_MULTIVARIATE_EXPERIMENT_ID,
            listOf<String>(AUDIENCE_GRYFFINDOR_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_MULTIVARIATE_EXPERIMENT_FRED,
                    VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE,
                    VARIATION_MULTIVARIATE_EXPERIMENT_GRED,
                    VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE
            ),
            ProjectConfigTestUtils.createMapOfObjects(
                    ProjectConfigTestUtils.createListOfObjects(
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FRED,
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_FEORGE,
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED,
                            MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GEORGE
                    ),
                    ProjectConfigTestUtils.createListOfObjects(
                            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_KEY,
                            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_KEY,
                            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_KEY,
                            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_KEY
                    )
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_FRED_ID,
                            2500
                    ),
                    TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_FEORGE_ID,
                            5000
                    ),
                    TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_GRED_ID,
                            7500
                    ),
                    TrafficAllocation(
                            VARIATION_MULTIVARIATE_EXPERIMENT_GEORGE_ID,
                            10000
                    )
            )
    )

    private val LAYER_DOUBLE_FEATURE_EXPERIMENT_ID = "1278722008"
    private val EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_ID = "2201520193"
    val EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY = "double_single_variable_feature_experiment"
    private val VARIATION_DOUBLE_FEATURE_PI_VARIATION_ID = "1505457580"
    private val VARIATION_DOUBLE_FEATURE_PI_VARIATION_KEY = "pi_variation"
    private val VARIATION_DOUBLE_FEATURE_ENABLED_VALUE = true
    private val VARIATION_DOUBLE_FEATURE_PI_VARIATION = Variation(
            VARIATION_DOUBLE_FEATURE_PI_VARIATION_ID,
            VARIATION_DOUBLE_FEATURE_PI_VARIATION_KEY,
            VARIATION_DOUBLE_FEATURE_ENABLED_VALUE,
            listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance(
                    VARIABLE_DOUBLE_VARIABLE_ID,
                    "3.14"
            ))
    )
    private val VARIATION_DOUBLE_FEATURE_EULER_VARIATION_ID = "119616179"
    private val VARIATION_DOUBLE_FEATURE_EULER_VARIATION_KEY = "euler_variation"
    private val VARIATION_DOUBLE_FEATURE_EULER_VARIATION = Variation(
            VARIATION_DOUBLE_FEATURE_EULER_VARIATION_ID,
            VARIATION_DOUBLE_FEATURE_EULER_VARIATION_KEY,
            listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance(
                    VARIABLE_DOUBLE_VARIABLE_ID,
                    "2.718"
            ))
    )
    private val EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT = Experiment(
            EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_ID,
            EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_DOUBLE_FEATURE_EXPERIMENT_ID,
            listOf<String>(AUDIENCE_SLYTHERIN_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIATION_DOUBLE_FEATURE_PI_VARIATION,
                    VARIATION_DOUBLE_FEATURE_EULER_VARIATION
            ),
            emptyMap<String, String>(),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            VARIATION_DOUBLE_FEATURE_PI_VARIATION_ID,
                            4000
                    ),
                    TrafficAllocation(
                            VARIATION_DOUBLE_FEATURE_EULER_VARIATION_ID,
                            8000
                    )
            )
    )

    private val LAYER_PAUSED_EXPERIMENT_ID = "3949273892"
    private val EXPERIMENT_PAUSED_EXPERIMENT_ID = "2667098701"
    val EXPERIMENT_PAUSED_EXPERIMENT_KEY = "paused_experiment"
    private val VARIATION_PAUSED_EXPERIMENT_CONTROL_ID = "391535909"
    private val VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY = "Control"
    private val VARIATION_PAUSED_EXPERIMENT_CONTROL = Variation(
            VARIATION_PAUSED_EXPERIMENT_CONTROL_ID,
            VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    val PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL = "Harry Potter"
    private val EXPERIMENT_PAUSED_EXPERIMENT = Experiment(
            EXPERIMENT_PAUSED_EXPERIMENT_ID,
            EXPERIMENT_PAUSED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.PAUSED.toString(),
            LAYER_PAUSED_EXPERIMENT_ID,
            emptyList<String>(),
            listOf<Variation>(VARIATION_PAUSED_EXPERIMENT_CONTROL),
            Collections.singletonMap(PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL,
                    VARIATION_PAUSED_EXPERIMENT_CONTROL_KEY),
            listOf<TrafficAllocation>(TrafficAllocation(
                    VARIATION_PAUSED_EXPERIMENT_CONTROL_ID,
                    10000
            ))
    )
    private val LAYER_LAUNCHED_EXPERIMENT_ID = "3587821424"
    private val EXPERIMENT_LAUNCHED_EXPERIMENT_ID = "3072915611"
    val EXPERIMENT_LAUNCHED_EXPERIMENT_KEY = "launched_experiment"
    private val VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID = "1647582435"
    private val VARIATION_LAUNCHED_EXPERIMENT_CONTROL_KEY = "launch_control"
    private val VARIATION_LAUNCHED_EXPERIMENT_CONTROL = Variation(
            VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID,
            VARIATION_LAUNCHED_EXPERIMENT_CONTROL_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val EXPERIMENT_LAUNCHED_EXPERIMENT = Experiment(
            EXPERIMENT_LAUNCHED_EXPERIMENT_ID,
            EXPERIMENT_LAUNCHED_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.LAUNCHED.toString(),
            LAYER_LAUNCHED_EXPERIMENT_ID,
            emptyList<String>(),
            listOf<Variation>(VARIATION_LAUNCHED_EXPERIMENT_CONTROL),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    VARIATION_LAUNCHED_EXPERIMENT_CONTROL_ID,
                    8000
            ))
    )
    private val LAYER_MUTEX_GROUP_EXPERIMENT_1_LAYER_ID = "3755588495"
    private val EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID = "4138322202"
    private val EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_KEY = "mutex_group_2_experiment_1"
    private val VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID = "1394671166"
    private val VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY = "mutex_group_2_experiment_1_variation_1"
    private val VARIATION_MUTEX_GROUP_EXP_FEATURE_ENABLED_VALUE = true
    private val VARIATION_MUTEX_GROUP_EXP_1_VAR_1 = Variation(
            VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID,
            VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY,
            VARIATION_MUTEX_GROUP_EXP_FEATURE_ENABLED_VALUE,
            listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance(
                    VARIABLE_CORRELATING_VARIATION_NAME_ID,
                    VARIATION_MUTEX_GROUP_EXP_1_VAR_1_KEY
            ))
    )
    val EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1 = Experiment(
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_MUTEX_GROUP_EXPERIMENT_1_LAYER_ID,
            emptyList<String>(),
            listOf<Variation>(VARIATION_MUTEX_GROUP_EXP_1_VAR_1),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    VARIATION_MUTEX_GROUP_EXP_1_VAR_1_ID,
                    10000
            )),
            GROUP_2_ID
    )
    private val LAYER_MUTEX_GROUP_EXPERIMENT_2_LAYER_ID = "3818002538"
    private val EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID = "1786133852"
    private val EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_KEY = "mutex_group_2_experiment_2"
    private val VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID = "1619235542"
    private val VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY = "mutex_group_2_experiment_2_variation_2"
    private val VARIATION_MUTEX_GROUP_EXP_2_FEATURE_ENABLED_VALUE = true
    val VARIATION_MUTEX_GROUP_EXP_2_VAR_1 = Variation(
            VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID,
            VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY,
            VARIATION_MUTEX_GROUP_EXP_2_FEATURE_ENABLED_VALUE,
            listOf<LiveVariableUsageInstance>(LiveVariableUsageInstance(
                    VARIABLE_CORRELATING_VARIATION_NAME_ID,
                    VARIATION_MUTEX_GROUP_EXP_2_VAR_1_KEY
            ))
    )
    val EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2 = Experiment(
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID,
            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_MUTEX_GROUP_EXPERIMENT_2_LAYER_ID,
            emptyList<String>(),
            listOf<Variation>(VARIATION_MUTEX_GROUP_EXP_2_VAR_1),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    VARIATION_MUTEX_GROUP_EXP_2_VAR_1_ID,
                    10000
            )),
            GROUP_2_ID
    )

    private val EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID = "748215081"
    val EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY = "experiment_with_malformed_audience"
    private val LAYER_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID = "1238149537"
    private val VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID = "535538389"
    val VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY = "var1"
    private val VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE = Variation(
            VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
            VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY,
            emptyList<LiveVariableUsageInstance>()
    )
    private val EXPERIMENT_WITH_MALFORMED_AUDIENCE = Experiment(
            EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
            EXPERIMENT_WITH_MALFORMED_AUDIENCE_KEY,
            Experiment.ExperimentStatus.RUNNING.toString(),
            LAYER_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
            listOf<String>(AUDIENCE_WITH_MISSING_VALUE_ID),
            listOf<Variation>(VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    VARIATION_EXPERIMENT_WITH_MALFORMED_AUDIENCE_ID,
                    10000
            ))
    )

    // generate groups
    private val GROUP_1 = Group(
            GROUP_1_ID,
            Group.RANDOM_POLICY,
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_FIRST_GROUPED_EXPERIMENT,
                    EXPERIMENT_SECOND_GROUPED_EXPERIMENT
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
                            4000
                    ),
                    TrafficAllocation(
                            EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
                            8000
                    )
            )
    )
    private val GROUP_2 = Group(
            GROUP_2_ID,
            Group.RANDOM_POLICY,
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1,
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2
            ),
            ProjectConfigTestUtils.createListOfObjects(
                    TrafficAllocation(
                            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
                            5000
                    ),
                    TrafficAllocation(
                            EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID,
                            10000
                    )
            )
    )

    // events
    private val EVENT_BASIC_EVENT_ID = "3785620495"
    val EVENT_BASIC_EVENT_KEY = "basic_event"
    private val EVENT_BASIC_EVENT = EventType(
            EVENT_BASIC_EVENT_ID,
            EVENT_BASIC_EVENT_KEY,
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_BASIC_EXPERIMENT_ID,
                    EXPERIMENT_FIRST_GROUPED_EXPERIMENT_ID,
                    EXPERIMENT_SECOND_GROUPED_EXPERIMENT_ID,
                    EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID,
                    EXPERIMENT_LAUNCHED_EXPERIMENT_ID
            )
    )
    private val EVENT_PAUSED_EXPERIMENT_ID = "3195631717"
    val EVENT_PAUSED_EXPERIMENT_KEY = "event_with_paused_experiment"
    private val EVENT_PAUSED_EXPERIMENT = EventType(
            EVENT_PAUSED_EXPERIMENT_ID,
            EVENT_PAUSED_EXPERIMENT_KEY,
            listOf<String>(EXPERIMENT_PAUSED_EXPERIMENT_ID)
    )
    private val EVENT_LAUNCHED_EXPERIMENT_ONLY_ID = "1987018666"
    val EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY = "event_with_launched_experiments_only"
    private val EVENT_LAUNCHED_EXPERIMENT_ONLY = EventType(
            EVENT_LAUNCHED_EXPERIMENT_ONLY_ID,
            EVENT_LAUNCHED_EXPERIMENT_ONLY_KEY,
            listOf<String>(EXPERIMENT_LAUNCHED_EXPERIMENT_ID)
    )

    // rollouts
    val ROLLOUT_2_ID = "813411034"
    private val ROLLOUT_2_RULE_1 = Experiment(
            "3421010877",
            "3421010877",
            Experiment.ExperimentStatus.RUNNING.toString(),
            ROLLOUT_2_ID,
            listOf<String>(AUDIENCE_GRYFFINDOR_ID),
            listOf<Variation>(Variation(
                    "521740985",
                    "521740985",
                    true,
                    ProjectConfigTestUtils.createListOfObjects(
                            LiveVariableUsageInstance(
                                    "675244127",
                                    "G"
                            ),
                            LiveVariableUsageInstance(
                                    "4052219963",
                                    "odric"
                            )
                    )
            )),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    "521740985",
                    5000
            ))
    )
    private val ROLLOUT_2_RULE_2 = Experiment(
            "600050626",
            "600050626",
            Experiment.ExperimentStatus.RUNNING.toString(),
            ROLLOUT_2_ID,
            listOf<String>(AUDIENCE_SLYTHERIN_ID),
            listOf<Variation>(Variation(
                    "180042646",
                    "180042646",
                    true,
                    ProjectConfigTestUtils.createListOfObjects(
                            LiveVariableUsageInstance(
                                    "675244127",
                                    "S"
                            ),
                            LiveVariableUsageInstance(
                                    "4052219963",
                                    "alazar"
                            )
                    )
            )),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    "180042646",
                    5000
            ))
    )
    private val ROLLOUT_2_RULE_3 = Experiment(
            "2637642575",
            "2637642575",
            Experiment.ExperimentStatus.RUNNING.toString(),
            ROLLOUT_2_ID,
            listOf<String>(AUDIENCE_ENGLISH_CITIZENS_ID),
            listOf<Variation>(Variation(
                    "2346257680",
                    "2346257680",
                    true,
                    ProjectConfigTestUtils.createListOfObjects(
                            LiveVariableUsageInstance(
                                    "675244127",
                                    "D"
                            ),
                            LiveVariableUsageInstance(
                                    "4052219963",
                                    "udley"
                            )
                    )
            )),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    "2346257680",
                    5000
            ))
    )
    private val ROLLOUT_2_EVERYONE_ELSE_RULE = Experiment(
            "828245624",
            "828245624",
            Experiment.ExperimentStatus.RUNNING.toString(),
            ROLLOUT_2_ID,
            emptyList<String>(),
            listOf<Variation>(Variation(
                    "3137445031",
                    "3137445031",
                    true,
                    ProjectConfigTestUtils.createListOfObjects(
                            LiveVariableUsageInstance(
                                    "675244127",
                                    "M"
                            ),
                            LiveVariableUsageInstance(
                                    "4052219963",
                                    "uggle"
                            )
                    )
            )),
            emptyMap<String, String>(),
            listOf<TrafficAllocation>(TrafficAllocation(
                    "3137445031",
                    5000
            ))
    )
    val ROLLOUT_2 = Rollout(
            ROLLOUT_2_ID,
            ProjectConfigTestUtils.createListOfObjects(
                    ROLLOUT_2_RULE_1,
                    ROLLOUT_2_RULE_2,
                    ROLLOUT_2_RULE_3,
                    ROLLOUT_2_EVERYONE_ELSE_RULE
            )
    )

    // finish features
    val FEATURE_FLAG_MULTI_VARIATE_FEATURE = FeatureFlag(
            FEATURE_MULTI_VARIATE_FEATURE_ID,
            FEATURE_MULTI_VARIATE_FEATURE_KEY,
            ROLLOUT_2_ID,
            listOf<String>(EXPERIMENT_MULTIVARIATE_EXPERIMENT_ID),
            ProjectConfigTestUtils.createListOfObjects(
                    VARIABLE_FIRST_LETTER_VARIABLE,
                    VARIABLE_REST_OF_NAME_VARIABLE
            )
    )
    val FEATURE_FLAG_MUTEX_GROUP_FEATURE = FeatureFlag(
            FEATURE_MUTEX_GROUP_FEATURE_ID,
            FEATURE_MUTEX_GROUP_FEATURE_KEY,
            "",
            ProjectConfigTestUtils.createListOfObjects(
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_1_EXPERIMENT_ID,
                    EXPERIMENT_MUTEX_GROUP_EXPERIMENT_2_EXPERIMENT_ID
            ),
            listOf<LiveVariable>(VARIABLE_CORRELATING_VARIATION_NAME_VARIABLE)
    )
    val FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE = FeatureFlag(
            FEATURE_SINGLE_VARIABLE_DOUBLE_ID,
            FEATURE_SINGLE_VARIABLE_DOUBLE_KEY,
            "",
            listOf<String>(EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT_ID),
            listOf<LiveVariable>(VARIABLE_DOUBLE_VARIABLE)
    )
    val FEATURE_FLAG_SINGLE_VARIABLE_INTEGER = FeatureFlag(
            FEATURE_SINGLE_VARIABLE_INTEGER_ID,
            FEATURE_SINGLE_VARIABLE_INTEGER_KEY,
            ROLLOUT_3_ID,
            emptyList<String>(),
            listOf<LiveVariable>(VARIABLE_INTEGER_VARIABLE)
    )

    fun generateValidProjectConfigV4(): ProjectConfig {

        // list attributes
        val attributes = ArrayList<Attribute>()
        attributes.add(ATTRIBUTE_HOUSE)
        attributes.add(ATTRIBUTE_NATIONALITY)
        attributes.add(ATTRIBUTE_OPT)

        // list audiences
        val audiences = ArrayList<Audience>()
        audiences.add(AUDIENCE_GRYFFINDOR)
        audiences.add(AUDIENCE_SLYTHERIN)
        audiences.add(AUDIENCE_ENGLISH_CITIZENS)
        audiences.add(AUDIENCE_WITH_MISSING_VALUE)

        // list events
        val events = ArrayList<EventType>()
        events.add(EVENT_BASIC_EVENT)
        events.add(EVENT_PAUSED_EXPERIMENT)
        events.add(EVENT_LAUNCHED_EXPERIMENT_ONLY)

        // list experiments
        val experiments = ArrayList<Experiment>()
        experiments.add(EXPERIMENT_BASIC_EXPERIMENT)
        experiments.add(EXPERIMENT_MULTIVARIATE_EXPERIMENT)
        experiments.add(EXPERIMENT_DOUBLE_FEATURE_EXPERIMENT)
        experiments.add(EXPERIMENT_PAUSED_EXPERIMENT)
        experiments.add(EXPERIMENT_LAUNCHED_EXPERIMENT)
        experiments.add(EXPERIMENT_WITH_MALFORMED_AUDIENCE)

        // list featureFlags
        val featureFlags = ArrayList<FeatureFlag>()
        featureFlags.add(FEATURE_FLAG_BOOLEAN_FEATURE)
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_DOUBLE)
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_INTEGER)
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_BOOLEAN)
        featureFlags.add(FEATURE_FLAG_SINGLE_VARIABLE_STRING)
        featureFlags.add(FEATURE_FLAG_MULTI_VARIATE_FEATURE)
        featureFlags.add(FEATURE_FLAG_MUTEX_GROUP_FEATURE)

        val groups = ArrayList<Group>()
        groups.add(GROUP_1)
        groups.add(GROUP_2)

        // list rollouts
        val rollouts = ArrayList<Rollout>()
        rollouts.add(ROLLOUT_1)
        rollouts.add(ROLLOUT_2)
        rollouts.add(ROLLOUT_3)

        return ProjectConfig(
                ACCOUNT_ID,
                ANONYMIZE_IP,
                BOT_FILTERING,
                PROJECT_ID,
                REVISION,
                VERSION,
                attributes,
                audiences,
                events,
                experiments,
                featureFlags,
                groups,
                emptyList<LiveVariable>(),
                rollouts
        )
    }
}
