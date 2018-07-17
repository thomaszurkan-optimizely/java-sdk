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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlin.experimental.and
import kotlin.experimental.or

object MurmurHash3 {

    /**
     * @param data the origin data
     * @param offset the offset into the data array
     * @param len the length of the data array to use
     * @param seed the murmur hash seed
     * @return the MurmurHash3_x86_32 hash
     */
    @SuppressFBWarnings(value = *arrayOf("SF_SWITCH_FALLTHROUGH", "SF_SWITCH_NO_DEFAULT"), justification = "deliberate")
    fun murmurhash3_x86_32(data: ByteArray, offset: Int, len: Int, seed: Int): Int {

        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593

        var h1 = seed
        val roundedEnd = offset + (len and 0xfffffffc.toInt())  // round down to 4 byte block

        var i = offset
        while (i < roundedEnd) {
            // little endian load order
            var k1 = (data[i] and 0xff.toByte()) or (((data[i + 1] and 0xff.toByte()).toInt() shl 8).toByte()) or ((data[i + 2] and 0xff.toByte()).toInt() shl 16).toByte() or (data[i + 3].toInt() shl 24).toByte()
            k1 = (k1 * c1).toByte()
            k1 = ((k1.toInt() shl 15) or k1.toInt().ushr(17)).toByte()  // ROTL32(k1,15);
            k1 = (k1 * c2).toByte()

            h1 = h1 xor k1.toInt()
            h1 = h1 shl 13 or h1.ushr(19)  // ROTL32(h1,13);
            h1 = h1 * 5 + 0xe6546b64.toInt()
            i += 4
        }

        // tail
        var k1 = 0

        when (len and 0x03) {
            3 -> {
                k1 = (data[roundedEnd + 2] and 0xff.toByte()).toInt() shl 16
                k1 = k1 or ((data[roundedEnd + 1] and 0xff.toByte()).toInt() shl 8)
                k1 = k1 or ((data[roundedEnd] and 0xff.toByte()).toInt())
                k1 *= c1
                k1 = k1 shl 15 or k1.ushr(17)  // ROTL32(k1,15);
                k1 *= c2
                h1 = h1 xor k1
            }
        // fallthrough
            2 -> {
                k1 = k1 or ((data[roundedEnd + 1] and 0xff.toByte()).toInt() shl 8)
                k1 = k1 or ((data[roundedEnd] and 0xff.toByte()).toInt())
                k1 *= c1
                k1 = k1 shl 15 or k1.ushr(17)
                k1 *= c2
                h1 = h1 xor k1
            }
        // fallthrough
            1 -> {
                k1 = k1 or ((data[roundedEnd] and 0xff.toByte()).toInt())
                k1 *= c1
                k1 = k1 shl 15 or k1.ushr(17)
                k1 *= c2
                h1 = h1 xor k1
            }
        }

        // finalization
        h1 = h1 xor len

        // fmix(h1);
        h1 = h1 xor h1.ushr(16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor h1.ushr(13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor h1.ushr(16)

        return h1
    }


    /**
     * This is more than 2x faster than hashing the result of String.getBytes().
     *
     * @param data the origin data
     * @param offset the offset into the data array
     * @param len the length of the data array to use
     * @param seed the murmur hash seed
     * @return the MurmurHash3_x86_32 hash of the UTF-8 bytes of the String without actually encoding
     * the string to a temporary buffer
     */
    fun murmurhash3_x86_32(data: CharSequence, offset: Int, len: Int, seed: Int): Int {

        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593

        var h1 = seed

        var pos = offset
        val end = offset + len
        var k1 = 0
        var k2: Int
        var shift = 0
        var bits: Int
        var nBytes = 0   // length in UTF8 bytes


        while (pos < end) {
            val code = data[pos++].toInt()
            if (code < 0x80) {
                k2 = code
                bits = 8

                /***
                 * // optimized ascii implementation (currently slower!!! code size?)
                 * if (shift == 24) {
                 * k1 = k1 | (code << 24);
                 *
                 * k1 *= c1;
                 * k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
                 * k1 *= c2;
                 *
                 * h1 ^= k1;
                 * h1 = (h1 << 13) | (h1 >>> 19);  // ROTL32(h1,13);
                 * h1 = h1*5+0xe6546b64;
                 *
                 * shift = 0;
                 * nBytes += 4;
                 * k1 = 0;
                 * } else {
                 * k1 |= code << shift;
                 * shift += 8;
                 * }
                 * continue;
                 */

            } else if (code < 0x800) {
                k2 = 0xC0 or (code shr 6) or (0x80 or (code and 0x3F) shl 8)
                bits = 16
            } else if (code < 0xD800 || code > 0xDFFF || pos >= end) {
                // we check for pos>=end to encode an unpaired surrogate as 3 bytes.
                k2 = 0xE0 or (code shr 12) or (0x80 or (code shr 6 and 0x3F) shl 8) or (0x80 or (code and 0x3F) shl 16)
                bits = 24
            } else {
                // surrogate pair
                // int utf32 = pos < end ? (int) data.charAt(pos++) : 0;
                var utf32 = data[pos++].toInt()
                utf32 = (code - 0xD7C0 shl 10) + (utf32 and 0x3FF)
                k2 = 0xff and (0xF0 or (utf32 shr 18)) or (0x80 or (utf32 shr 12 and 0x3F) shl 8) or (0x80 or (utf32 shr 6 and 0x3F) shl 16) or (0x80 or (utf32 and 0x3F) shl 24)
                bits = 32
            }


            k1 = k1 or (k2 shl shift)

            // int used_bits = 32 - shift;  // how many bits of k2 were used in k1.
            // int unused_bits = bits - used_bits; //  (bits-(32-shift)) == bits+shift-32  == bits-newshift

            shift += bits
            if (shift >= 32) {
                // mix after we have a complete word

                k1 *= c1
                k1 = k1 shl 15 or k1.ushr(17)  // ROTL32(k1,15);
                k1 *= c2

                h1 = h1 xor k1
                h1 = h1 shl 13 or h1.ushr(19)  // ROTL32(h1,13);
                h1 = h1 * 5 + 0xe6546b64.toInt()

                shift -= 32
                // unfortunately, java won't let you shift 32 bits off, so we need to check for 0
                if (shift != 0) {
                    k1 = k2.ushr(bits - shift)   // bits used == bits - newshift
                } else {
                    k1 = 0
                }
                nBytes += 4
            }

        } // inner

        // handle tail
        if (shift > 0) {
            nBytes += shift shr 3
            k1 *= c1
            k1 = k1 shl 15 or k1.ushr(17)  // ROTL32(k1,15);
            k1 *= c2
            h1 = h1 xor k1
        }

        // finalization
        h1 = h1 xor nBytes

        // fmix(h1);
        h1 = h1 xor h1.ushr(16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor h1.ushr(13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor h1.ushr(16)

        return h1
    }
}