package ballast.nostr

import fr.acinq.secp256k1.Hex
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import ballast.crypto.CryptoUtils

internal val eventMapper = Json
@OptIn(ExperimentalSerializationApi::class)
internal val arraySerializer: KSerializer<Array<String>> = ArraySerializer(String.serializer())

internal class StringArraySerializer : KSerializer<Array<String>> {
    private val builtinSerializer = arraySerializer
    override val descriptor: SerialDescriptor
        get() = builtinSerializer.descriptor

    override fun deserialize(decoder: Decoder): Array<String> {
        return decoder.decodeSerializableValue(builtinSerializer)
    }

    override fun serialize(encoder: Encoder, value: Array<String>) {
        encoder.encodeSerializableValue(builtinSerializer, value)
    }

}

/**
 * Keeping this function below for legacy purposes.
 */
//fun Event.toJson(): String {
//
//    val listOfTagsJson = if (tags.isEmpty()) "[]"
//    else buildString {
//        append("[")
//        tags.forEach { tagJson ->
//            if (tagJson.size == 3) {
//                val tag = Tag(tagJson[0], tagJson[1], tagJson[2])
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
//    val event = buildString {
//        append("{")
//        append("\"id\":\"$id\",")
//        append("\"pubkey\":\"$pubkey\",")
//        append("\"created_at\":$creationDate,")
//        append("\"kind\":$eventKind,")
//        append("\"tags\":$listOfTagsJson,")
//        append("\"content\":\"$content\",")
//        append("\"sig\":\"$eventSignature\"")
//        append("}")
//    }
//
//    return event
//}

fun Event.isValid(): Boolean {
    val eventId = getEventId(
        this.pubkey, this.creationDate, this.eventKind,
        this.tags, this.content
    )
    if (eventId != this.id) {
        serviceLogger.w("The event id is invalid.")
        return false
    }
    val signatureValidity = CryptoUtils.verifyContentSignature(
        Hex.decode(this.eventSignature),
        Hex.decode(this.pubkey),
        Hex.decode(eventId)
    )
    if (!signatureValidity) {
        serviceLogger.w("The event signature is invalid.\n Please check the pubkey, or content.")
        return false
    }
    return true
}

fun Event.serialize(): String {
    if (!this.isValid()) throw EventValidationError("Generated event is not valid")
    return eventMapper.encodeToString(this)
}

public fun deserializedEvent(eventJson: String): Event {
    val deserializedEvent = eventMapper.decodeFromString<Event>(eventJson)
    if (!deserializedEvent.isValid()) throw EventValidationError("The event is invalid. \n Event: $deserializedEvent")
    return deserializedEvent
}
