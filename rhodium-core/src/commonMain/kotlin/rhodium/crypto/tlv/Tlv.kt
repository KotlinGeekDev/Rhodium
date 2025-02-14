/**
 * Copyright (c) 2022 KotlinGeekDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package rhodium.crypto.tlv

/**
 * Copyright (c) 2024 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.ditchoom.buffer.ByteOrder
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.wrap
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import rhodium.crypto.toHexString

class Tlv(
    val data: Map<Byte, List<ByteArray>>,
) {
    fun asInt(type: Byte) = data[type]?.mapNotNull { it.toInt32() }

    fun asHex(type: Byte) = data[type]?.map { it.toHexString() }

    fun asString(type: Byte) = data[type]?.map { ByteString(it).decodeToString() }

    fun firstAsInt(type: Byte) = data[type]?.firstOrNull()?.toInt32()

    fun firstAsHex(type: Byte) = data[type]?.firstOrNull()?.toHexString()

    fun firstAsString(type: Byte) = data[type]?.firstOrNull()?.run { ByteString(this).decodeToString() }

    fun asStringList(type: Byte) = data[type]?.map { ByteString(it).decodeToString() }

    companion object {
        fun parse(data: ByteArray): Tlv {
            val result = mutableMapOf<Byte, MutableList<ByteArray>>()
            var rest = data
            while (rest.isNotEmpty()) {
                val t = rest[0]
                val l = rest[1].toUByte().toInt()
                val v = rest.sliceArray(IntRange(2, (2 + l) - 1))
                rest = rest.sliceArray(IntRange(2 + l, rest.size - 1))
                if (v.size < l) continue

                if (!result.containsKey(t)) {
                    result[t] = mutableListOf()
                }
                result[t]?.add(v)
            }
            return Tlv(result)
        }
    }
}

fun ByteArray.toInt32(): Int? {
    if (size != 4) return null

    return PlatformBuffer.wrap(this.copyOfRange(0, 4), ByteOrder.BIG_ENDIAN).readInt()
}