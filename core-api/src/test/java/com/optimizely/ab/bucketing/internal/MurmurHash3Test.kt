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
package com.optimizely.ab.bucketing.internal

import org.junit.Test

import java.nio.charset.Charset
import java.util.Random

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

import org.junit.Assert.assertEquals

/**
 * @author yonik
 *
 * See http://github.com/yonik/java_util for future updates to this file.
 *
 *
 * **NOTICE:** changes made to the original
 *
 *  * package changed
 *  * removed `testCorrectValues`
 *  * findbugs fixes & removing `System.out.println`
 *
 *
 * optimizely
 * somerandomvalue
 */
class MurmurHash3Test {

    private val utf8Charset = Charset.forName("UTF-8")

    private fun doString(s: String, pre: Int = 0, post: Int = 0) {
        val utf8 = s.toByteArray(utf8Charset)
        val hash1 = MurmurHash3.murmurhash3_x86_32(utf8, pre, utf8.size - pre - post, 123456789)
        var hash2 = MurmurHash3.murmurhash3_x86_32(s, pre, s.length - pre - post, 123456789)
        if (hash1 != hash2) {
            // second time for debugging...
            hash2 = MurmurHash3.murmurhash3_x86_32(s, pre, s.length - pre - post, 123456789)
        }
        assertEquals(hash1.toLong(), hash2.toLong())
    }

    @Test
    @SuppressFBWarnings(value = *arrayOf("SF_SWITCH_FALLTHROUGH", "SF_SWITCH_NO_DEFAULT"), justification = "deliberate")
    fun testStringHash() {
        doString("hello!")
        doString("ABCD")
        doString("\u0123")
        doString("\u2345")
        doString("\u2345\u1234")

        val r = Random()
        val sb = StringBuilder(40)
        for (i in 0..99999) {
            sb.setLength(0)
            val pre = r.nextInt(3)
            val post = r.nextInt(3)
            val len = r.nextInt(16)

            for (j in 0..pre - 1) {
                val codePoint = r.nextInt(0x80)
                sb.appendCodePoint(codePoint)
            }

            for (j in 0..len - 1) {
                var codePoint: Int
                do {
                    var max = 0
                    when (r.nextInt() and 0x3) {
                        0 -> max = 0x80
                        1 -> max = 0x800
                        2 -> max = 0xffff + 1
                        3 -> max = Character.MAX_CODE_POINT + 1 // up to 4 bytes
                    }// 1 UTF8 bytes
                    // up to 2 bytes
                    // up to 3 bytes

                    codePoint = r.nextInt(max)
                } while (codePoint < 0xffff && (Character.isHighSurrogate(codePoint.toChar()) || Character.isLowSurrogate(codePoint.toChar())))

                sb.appendCodePoint(codePoint)
            }

            for (j in 0..post - 1) {
                val codePoint = r.nextInt(0x80)
                sb.appendCodePoint(codePoint)
            }

            val s = sb.toString()
            val middle = s.substring(pre, s.length - post)

            doString(s)
            doString(middle)
            doString(s, pre, post)
        }

    }
}


// Here is my C++ code to produce the list of answers to check against.
/***************************************************
 * #include <iostream>
 * #include "MurmurHash3.h"
 * using namespace std;
 *
 * int main(int argc, char** argv) {
 * char* val = strdup("Now is the time for all good men to come to the aid of their country");
 * int max = strlen(val);
 * int hash=0;
 * for (int i=0; i<max></max>; i++) {
 * hash = hash*31 + (val[i] & 0xff);
 * // we want to make sure that some high bits are set on the bytes
 * // to catch errors like signed vs unsigned shifting, etc.
 * val[i] = (char)hash;
 * }
 * uint32_t seed = 1;
 * for (int len=0; len<max></max>; len++) {
 * seed = seed * 0x9e3779b1;
 * int result;
 * MurmurHash3_x86_32(val, len, seed, &result);
 * cout << "0x" << hex << result << ",";
 * }
 * cout << endl;
 * cout << endl;
 * // now 128 bit
 * seed = 1;
 * for (int len=0; len<max></max>; len++) {
 * seed = seed * 0x9e3779b1;
 * long result[2];
 * MurmurHash3_x64_128(val, len, seed, &result);
 * cout << "0x" << hex << result[0] << "L,0x" << result[1] << "L," ;
 * }
 *
 * cout << endl;
 * }
</iostream> */