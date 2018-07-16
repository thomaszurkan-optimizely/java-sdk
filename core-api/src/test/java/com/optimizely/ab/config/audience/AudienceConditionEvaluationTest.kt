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
package com.optimizely.ab.config.audience

import com.optimizely.ab.config.Experiment
import org.junit.Before
import org.junit.Test

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Tests for the evaluation of different audience condition types (And, Or, Not, and UserAttribute)
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "mockito verify calls do have a side-effect")
class AudienceConditionEvaluationTest {

    internal var testUserAttributes: MutableMap<String, String>

    @Before
    fun initialize() {
        testUserAttributes = HashMap()
        testUserAttributes.put("browser_type", "chrome")
        testUserAttributes.put("device_type", "Android")
    }

    /**
     * Verify that UserAttribute.evaluate returns true on exact-matching visitor attribute data.
     */
    @Test
    @Throws(Exception::class)
    fun userAttributeEvaluateTrue() {
        val testInstance = UserAttribute("browser_type", "custom_dimension", "chrome")
        assertTrue(testInstance.evaluate(testUserAttributes))
    }

    /**
     * Verify that UserAttribute.evaluate returns false on non-exact-matching visitor attribute data.
     */
    @Test
    @Throws(Exception::class)
    fun userAttributeEvaluateFalse() {
        val testInstance = UserAttribute("browser_type", "custom_dimension", "firefox")
        assertFalse(testInstance.evaluate(testUserAttributes))
    }

    /**
     * Verify that UserAttribute.evaluate returns false on unknown visitor attributes.
     */
    @Test
    @Throws(Exception::class)
    fun userAttributeUnknownAttribute() {
        val testInstance = UserAttribute("unknown_dim", "custom_dimension", "unknown")
        assertFalse(testInstance.evaluate(testUserAttributes))
    }

    /**
     * Verify that NotCondition.evaluate returns true when its condition operand evaluates to false.
     */
    @Test
    @Throws(Exception::class)
    fun notConditionEvaluateTrue() {
        val userAttribute = mock<UserAttribute>(UserAttribute::class.java)
        `when`(userAttribute.evaluate(testUserAttributes)).thenReturn(false)

        val notCondition = NotCondition(userAttribute)
        assertTrue(notCondition.evaluate(testUserAttributes))
        verify(userAttribute, times(1)).evaluate(testUserAttributes)
    }

    /**
     * Verify that NotCondition.evaluate returns false when its condition operand evaluates to true.
     */
    @Test
    @Throws(Exception::class)
    fun notConditionEvaluateFalse() {
        val userAttribute = mock<UserAttribute>(UserAttribute::class.java)
        `when`(userAttribute.evaluate(testUserAttributes)).thenReturn(true)

        val notCondition = NotCondition(userAttribute)
        assertFalse(notCondition.evaluate(testUserAttributes))
        verify(userAttribute, times(1)).evaluate(testUserAttributes)
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    @Throws(Exception::class)
    fun orConditionEvaluateTrue() {
        val userAttribute1 = mock<UserAttribute>(UserAttribute::class.java)
        `when`(userAttribute1.evaluate(testUserAttributes)).thenReturn(true)

        val userAttribute2 = mock<UserAttribute>(UserAttribute::class.java)
        `when`(userAttribute2.evaluate(testUserAttributes)).thenReturn(false)

        val conditions = ArrayList<Condition>()
        conditions.add(userAttribute1)
        conditions.add(userAttribute2)

        val orCondition = OrCondition(conditions)
        assertTrue(orCondition.evaluate(testUserAttributes))
        verify(userAttribute1, times(1)).evaluate(testUserAttributes)
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(0)).evaluate(testUserAttributes)
    }

    /**
     * Verify that OrCondition.evaluate returns false when all of its operand conditions evaluate to false.
     */
    @Test
    @Throws(Exception::class)
    fun orConditionEvaluateFalse() {
        val userAttribute1 = mock<UserAttribute>(UserAttribute::class.java)
        `when`(userAttribute1.evaluate(testUserAttributes)).thenReturn(false)

        val userAttribute2 = mock<UserAttribute>(UserAttribute::class.java)
        `when`(userAttribute2.evaluate(testUserAttributes)).thenReturn(false)

        val conditions = ArrayList<Condition>()
        conditions.add(userAttribute1)
        conditions.add(userAttribute2)

        val orCondition = OrCondition(conditions)
        assertFalse(orCondition.evaluate(testUserAttributes))
        verify(userAttribute1, times(1)).evaluate(testUserAttributes)
        verify(userAttribute2, times(1)).evaluate(testUserAttributes)
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    @Throws(Exception::class)
    fun andConditionEvaluateTrue() {
        val orCondition1 = mock<OrCondition>(OrCondition::class.java)
        `when`(orCondition1.evaluate(testUserAttributes)).thenReturn(true)

        val orCondition2 = mock<OrCondition>(OrCondition::class.java)
        `when`(orCondition2.evaluate(testUserAttributes)).thenReturn(true)

        val conditions = ArrayList<Condition>()
        conditions.add(orCondition1)
        conditions.add(orCondition2)

        val andCondition = AndCondition(conditions)
        assertTrue(andCondition.evaluate(testUserAttributes))
        verify(orCondition1, times(1)).evaluate(testUserAttributes)
        verify(orCondition2, times(1)).evaluate(testUserAttributes)
    }

    /**
     * Verify that AndCondition.evaluate returns false when any one of its operand conditions evaluate to false.
     */
    @Test
    @Throws(Exception::class)
    fun andConditionEvaluateFalse() {
        val orCondition1 = mock<OrCondition>(OrCondition::class.java)
        `when`(orCondition1.evaluate(testUserAttributes)).thenReturn(false)

        val orCondition2 = mock<OrCondition>(OrCondition::class.java)
        `when`(orCondition2.evaluate(testUserAttributes)).thenReturn(true)

        val conditions = ArrayList<Condition>()
        conditions.add(orCondition1)
        conditions.add(orCondition2)

        val andCondition = AndCondition(conditions)
        assertFalse(andCondition.evaluate(testUserAttributes))
        verify(orCondition1, times(1)).evaluate(testUserAttributes)
        // shouldn't be called due to short-circuiting in 'And' evaluation
        verify(orCondition2, times(0)).evaluate(testUserAttributes)
    }

    /**
     * Verify that [UserAttribute.evaluate]
     * called when its attribute value is null
     * returns True when the user's attribute value is also null
     * True when the attribute is not in the map
     * False when empty string is used.
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun nullValueEvaluate() {
        val attributeName = "attribute_name"
        val attributeType = "attribute_type"
        val attributeValue: String? = null
        val nullValueAttribute = UserAttribute(
                attributeName,
                attributeType,
                attributeValue
        )

        assertTrue(nullValueAttribute.evaluate(emptyMap<String, String>()))
        assertTrue(nullValueAttribute.evaluate(Collections.singletonMap<String, String>(attributeName, attributeValue)))
        assertFalse(nullValueAttribute.evaluate(Collections.singletonMap(attributeName, "")))
    }
}
