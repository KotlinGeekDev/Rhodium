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

package rhodium.crypto

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

import fr.acinq.secp256k1.Hex
import kotlinx.coroutines.CancellationException
import rhodium.crypto.tlv.entity.*
import rhodium.logging.serviceLogger

object Nip19Parser {
    private val nip19PlusNip46regex: Regex =
        Regex(
            "(nostr:)?@?(nsec1|npub1|nevent1|naddr1|note1|nprofile1|nrelay1|nembed1|ncryptsec1)([qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)([\\S]*)",
            RegexOption.IGNORE_CASE,
        )

    val nip19regex: Regex =
        Regex(
            "(nostr:)?@?(nsec1|npub1|nevent1|naddr1|note1|nprofile1|nrelay1|nembed1)([qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)([\\S]*)",
            RegexOption.IGNORE_CASE,
        )

    data class ParseReturn(
        val entity: Entity,
        val nip19raw: String,
        val additionalChars: String? = null,
    )

    fun tryParseAndClean(uri: String?): String? {
        if (uri == null) return null

        try {
            val matcher = nip19PlusNip46regex.find(uri)
            if (matcher == null) {
                return null
            }

            val type = matcher.groups[2]?.value // npub1
            val key = matcher.groups[3]?.value // bech32

            return type + key
        } catch (e: Throwable) {
            serviceLogger.e("NIP19 Parser: Issue trying to Decode NIP19 $uri: ${e.message}", e)
        }

        return null
    }

    fun parse(uri: String?): ParseReturn? {
        if (uri == null) return null

        try {
            val matcher = nip19regex.find(uri)
            if (matcher == null) {
                return null
            }

            val type = matcher.groups[2]?.value // npub1
            val key = matcher.groups[3]?.value // bech32
            val additionalChars = matcher.groups[4]?.value // additional chars

            if (type == null) return null

            return parseComponents(type, key, additionalChars?.ifEmpty { null })
        } catch (e: Throwable) {
            serviceLogger.e("NIP19 Parser: Issue trying to Decode NIP19 $uri: ${e.message}", e)
        }

        return null
    }

    fun parseComponents(
        type: String,
        key: String?,
        additionalChars: String?,
    ): ParseReturn? =
        try {
            val nip19 = (type + key)
            val bytes = nip19.bechToBytes()

            when (type.lowercase()) {
                "nsec1" -> NSec.parse(bytes)
                "npub1" -> NPub.parse(bytes)
                "note1" -> Note.parse(bytes)
                "nprofile1" -> NProfile.parse(bytes)
                "nevent1" -> NEvent.parse(bytes)
                "nrelay1" -> NRelay.parse(bytes)
                "naddr1" -> NAddress.parse(bytes)
//                "nembed1" -> NEmbed.parse(bytes)
                else -> null
            }?.let {
                ParseReturn(it, nip19, additionalChars)
            }
        } catch (e: Throwable) {
            serviceLogger.e("NIP19 Parser: Issue trying to Decode NIP19 $key: ${e.message}", e)
            null
        }

    fun parseAll(content: String): List<Entity> {
        val matcher2 = nip19regex.findAll(content)
        val returningList = mutableListOf<Entity>()
        matcher2.forEach { result ->
            val type = result.groups[2]?.value // npub1
            val key = result.groups[3]?.value // bech32
            val additionalChars = result.groups[4]?.value // additional chars

            if (type != null) {
                val parsed = parseComponents(type, key, additionalChars)?.entity

                if (parsed != null) {
                    returningList.add(parsed)
                }
            }
        }
        return returningList
    }
}

fun decodePublicKey(key: String): ByteArray =
    when (val parsed = Nip19Parser.parse(key)?.entity) {
        is NSec -> Identity(privKey = key.bechToBytes()).pubKey
        is NPub -> parsed.hex.toBytes()
        is NProfile -> parsed.hex.toBytes()
        else -> Hex.decode(key) // crashes on purpose
    }

fun decodePrivateKeyAsHexOrNull(key: String): String? =
    try {
        when (val parsed = Nip19Parser.parse(key)?.entity) {
            is NSec -> parsed.hex
            is NPub -> null
            is NProfile -> null
            is Note -> null
            is NEvent -> null
//            is NEmbed -> null
            is NRelay -> null
            is NAddress -> null
            else -> Hex.decode(key).toHexString()
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }

fun decodePublicKeyAsHexOrNull(key: String): String? =
    try {
        when (val parsed = Nip19Parser.parse(key)?.entity) {
            is NSec -> Identity(privKey = key.bechToBytes()).pubKey.toHexString()
            is NPub -> parsed.hex
            is NProfile -> parsed.hex
            is Note -> null
            is NEvent -> null
//            is NEmbed -> null
            is NRelay -> null
            is NAddress -> null
            else -> Hex.decode(key).toHexString()
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }

fun decodeEventIdAsHexOrNull(key: String): String? =
    try {
        when (val parsed = Nip19Parser.parse(key)?.entity) {
            is NSec -> null
            is NPub -> null
            is NProfile -> null
            is Note -> parsed.hex
            is NEvent -> parsed.hex
            is NAddress -> parsed.aTag()
//            is NEmbed -> null
            is NRelay -> null
            else -> Hex.decode(key).toHexString()
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }