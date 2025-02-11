
package ktnostr.nostr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import ktnostr.crypto.CryptoUtils
import ktnostr.crypto.toHexString

/**
 * The Event class representing the Nostr Event.
 * The Event is of the form:
 *     Event(event_id, pubkey, creation_date, kind, tags, content, signature)
 * @param id The event id, as a string
 * @param pubkey The public key of the event's author, as a 32-byte string
 * @param creationDate The Unix timestamp of the event, as a number
 * @param eventKind The event kind, as a number
 * @param tags The list of tags associated with the event
 * @param content The event's content, as a string
 * @param eventSignature The event's signature, as a 64-byte string
 */
@Serializable
data class Event(
    val id: String,
    val pubkey: String,
    @SerialName("created_at") val creationDate: Long,
    @SerialName("kind") val eventKind: Int,
    val tags: List<Tag>,
    val content: String,
    @SerialName("sig") val eventSignature: String
)



 internal fun getEventId(
    pubkey: String, timeStamp: Long, eventKind: Int,
    tags: List<Tag>, content: String
): String {
    val jsonToHash = rawEventJson0(pubkey, timeStamp, eventKind, tags, content)

    val jsonHash = CryptoUtils.contentHash(jsonToHash)
    return jsonHash.toHexString()
}

internal fun rawEventJson0(
    pubkey: String, timeStamp: Long, eventKind: Int,
    tags: List<Tag>, content: String
): String {
    val serializedRawEvent = buildJsonArray {
        add(0)
        add(pubkey)
        add(timeStamp)
        add(eventKind)
        val tagListElement = eventMapper.encodeToJsonElement(ListSerializer(Tag.TagSerializer()), tags)
        add(tagListElement)
        add(content)
    }
    return serializedRawEvent.toString()
}


/**
 * This function is kept here for legacy purposes
 */
//internal fun rawEventJson(pubkey: String, timeStamp: Long, eventKind: Int,
//                 tags: List<List<String>>, content: String): String {
//
//    val listOfTagsJson = if (tags.isEmpty()) "[]"
//    else buildString {
//        append("[")
//        tags.forEach { tagArray ->
//            if(tagArray.size < 2) throw Error("Invalid tag structure")
//            if (tagArray.size ==3) {
//                val tag = Tag(tagArray[0], tagArray[1], tagArray[2])
//                append(
//                    "[\"${tag.identifier}\",\"${tag.description}\"${
//                        if (tag.recommendedRelayUrl.isNullOrEmpty()) ""
//                        else ",\"${tag.recommendedRelayUrl}\""
//                    }]"
//                )
//                append(",")
//            }
//
//        }
//        deleteAt(lastIndexOf(","))
//        append("]")
//    }
//
//    val jsonEventRaw = buildString {
//        append("[")
//        append("0,")
//        append("\"$pubkey\",")
//        append("$timeStamp,")
//        append("$eventKind,")
//        append("$listOfTagsJson,")
//        append("\"$content\"")
//        append("]")
//    }
//    return jsonEventRaw
//}


/**
 * This object represents the various event kinds
 * currently used on Nostr. Not all are supported, though.
 */
enum class EventKind(val kind: Int) {
    /**
     * Represents profile creation and modification on Nostr.
     */
    METADATA(0),

    /**
     * Represents published notes or posts or 'tweets'.
     */
    TEXT_NOTE(1),

    /**
     * Represents relay recommendations, for sharing relays. For helping with censorship-resistance.
     */
    RELAY_RECOMMENDATION(2),

    /**
     * Represents contact lists, for sharing profiles, and (probably) building
     * friend lists.
     */
    CONTACT_LIST(3),

    /**
     * Represents encrypted messages.
     */
    ENCRYPTED_DM(4),

    /**
     * Represents posts marked for deletion.
     */
    MARKED_FOR_DELETION(5),

    /**
     * This is used for auth events constructed by the client.
     */
    AUTH(22242),
    /**
     * This is used for comments on any posts other than Kind 1 posts(i.e, blogposts, shared file events,etc).
     */
    COMMENT(1111);

}
