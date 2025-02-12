@file:JvmName("EventUtils")
package ballast.nostr

import fr.acinq.secp256k1.Secp256k1
import ballast.crypto.CryptoUtils
import ballast.crypto.toBytes
import ballast.crypto.toHexString
import ballast.currentSystemTimestamp
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

object Events {

    internal fun generateEvent(eventKind: Int,
                               tags: List<Tag>,
                               content: String,
                               privateKeyHex: String,
                               publicKeyHex: String, timeStamp: Long = currentSystemTimestamp()): Event {
        //val currentUnixTime = currentSystemTimestamp()
        val privKey = privateKeyHex.toBytes()
        val genPubKey = Secp256k1.pubkeyCreate(privKey).drop(1).take(32).toByteArray()
        val genPubKeyHex = genPubKey.toHexString()
        if (genPubKeyHex != publicKeyHex)
            throw EventValidationError("The pubkeys don't match. Expected :$publicKeyHex \n Generated:$genPubKeyHex ")
        val eventID = getEventId(publicKeyHex, timeStamp, eventKind, tags, content)

        val eventIDRaw = eventID.toBytes()
        val signature = CryptoUtils.signContent(privateKeyHex.toBytes(), eventIDRaw)
        val signatureString = signature.toHexString()
        val normalizedTags = tags.map {
            Tag(it.identifier,
                it.description,
                it.recommendedRelayUrl,
                it.petname)
        }

        return Event(eventID, publicKeyHex, timeStamp, eventKind, normalizedTags, content, signatureString)
    }

    @JvmStatic
    fun MetadataEvent(privkey: String,
                      pubkey: String,
                      timeStamp: Long = currentSystemTimestamp(),
                      tags: List<Tag> = emptyList(),
                      kind: Int = EventKind.METADATA.kind, profile: String): Event {

        return generateEvent(kind, tags, profile, privkey, pubkey, timeStamp)
    }

    @JvmStatic
    fun TextEvent(privkey: String, pubkey: String,
                  tags: List<Tag> = emptyList(),
                  timeStamp: Long = currentSystemTimestamp(),
                  kind: Int = EventKind.TEXT_NOTE.kind, content: String): Event {
        return generateEvent(kind, tags, content, privkey, pubkey, timeStamp)
    }

    @JvmStatic
    fun RelayRecommendationEvent(privkey: String, pubkey: String,
                                 tags: List<Tag> = emptyList(),
                                 timeStamp: Long = currentSystemTimestamp(),
                                 kind: Int = EventKind.RELAY_RECOMMENDATION.kind,
                                 content: String): Event {
        if (!(content.startsWith("wss") || content.startsWith("ws"))) {
            throw EventValidationError("Content $content is not a valid relay URL.")
            }
        return generateEvent(kind, tags, content, privkey, pubkey, timeStamp)
    }

    @JvmStatic
    fun FollowEvent(privkey: String, pubkey: String,
                    tags: List<Tag>,
                    timeStamp: Long = currentSystemTimestamp(),
                    kind: Int = EventKind.CONTACT_LIST.kind,
                    content: String): Event {
        return generateEvent(kind, tags, content, privkey, pubkey, timeStamp)
    }

    @JvmStatic
    fun DirectMessageEvent(privkey: String, pubkey: String,
                           tags: List<Tag> = emptyList(),
                           timeStamp: Long = currentSystemTimestamp(),
                           kind: Int = EventKind.ENCRYPTED_DM.kind,
                           content: String): Event {
        return generateEvent(kind, tags, content, privkey, pubkey, timeStamp)
    }

    @JvmStatic
    fun DeletionEvent(privkey: String, pubkey: String,
                      tags: List<Tag> = emptyList(),
                      timeStamp: Long = currentSystemTimestamp(),
                      kind: Int = EventKind.MARKED_FOR_DELETION.kind,
                      content: String): Event {
        return generateEvent(kind, tags, content, privkey, pubkey, timeStamp)
    }

    @JvmStatic
    fun AuthEvent(privkey: String, pubkey: String,
                  relayUrl: String, challengeString: String,
                  timeStamp: Long = currentSystemTimestamp()
    ): Event {
        val authEventTags = mutableListOf<Tag>()
        authEventTags.add(Tag("relay", relayUrl))
        authEventTags.add(Tag("challenge", challengeString))
        return generateEvent(EventKind.AUTH.kind, authEventTags, "", privkey, pubkey, timeStamp)
    }

}