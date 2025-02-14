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

package rhodium.crypto.tlv.entity

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

import rhodium.crypto.bechToBytes
import rhodium.crypto.tlv.Tlv
import rhodium.crypto.tlv.TlvBuilder
import rhodium.crypto.tlv.TlvTypes
import rhodium.logging.serviceLogger

data class NAddress(
    val kind: Int,
    val author: String,
    val dTag: String,
    val relay: List<String>,
) : Entity {
    fun aTag(): String = "$kind:$author:$dTag"

    companion object {
        fun parse(naddr: String): NAddress? {
            try {
                val key = naddr.removePrefix("nostr:")

                if (key.startsWith("naddr")) {
                    return parse(key.bechToBytes())
                }
            } catch (e: Throwable) {
                serviceLogger.w("Issue trying to Decode NIP19 $this: ${e.message}", e)
                // e.printStackTrace()
            }

            return null
        }

        fun parse(bytes: ByteArray): NAddress? {

            if (bytes.isEmpty()) return null

            val tlv = Tlv.parse(bytes)

            val d = tlv.firstAsString(TlvTypes.SPECIAL.id) ?: ""
            val relay = tlv.asStringList(TlvTypes.RELAY.id) ?: emptyList()
            val author = tlv.firstAsHex(TlvTypes.AUTHOR.id) ?: return null
            val kind = tlv.firstAsInt(TlvTypes.KIND.id) ?: return null

            return NAddress(kind, author, d, relay)
        }

        fun create(
            kind: Int,
            pubKeyHex: String,
            dTag: String,
            relay: String?,
        ): String =
            TlvBuilder()
                .apply {
                    addString(TlvTypes.SPECIAL.id, dTag)
                    addStringIfNotNull(TlvTypes.RELAY.id, relay)
                    addHex(TlvTypes.AUTHOR.id, pubKeyHex)
                    addInt(TlvTypes.KIND.id, kind)
                }.build()
                .toNAddress()
    }
}