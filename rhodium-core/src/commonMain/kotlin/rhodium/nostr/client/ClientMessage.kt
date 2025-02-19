package rhodium.nostr.client

import com.benasher44.uuid.bytes
import com.benasher44.uuid.uuid4
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import rhodium.nostr.Event
import rhodium.nostr.NostrFilter

@Serializable(with = ClientMessage.MessageSerializer::class)
sealed class ClientMessage(open val messageType: String){
    companion object MessageSerializer : KSerializer<ClientMessage> {
        private val listSerializer = ListSerializer(elementSerializer = String.serializer())

        override val descriptor: SerialDescriptor
            get() = listSerializer.descriptor

        override fun deserialize(decoder: Decoder): ClientMessage {
            val jsonDecoder = (decoder as JsonDecoder)
            val message = jsonDecoder.decodeJsonElement().jsonArray
            val marker = message[0].jsonPrimitive.content
            println("Message type : $marker")
            val clientMessage = when(marker) {

                "REQ" -> {
                    val requestMessage = decodeToRequestMessage(jsonDecoder, message)
                    requestMessage
                }
                "EVENT" -> {
                    val clientEventMessage = decodeToEventMessage(jsonDecoder, message)
                    clientEventMessage
                }
                "CLOSE" -> {
                    val closeRequest = decodeToCloseRequest(jsonDecoder, message)
                    closeRequest
                }
                else -> {
                    throw SerializationException("Unrecognized message type: $marker")
                }
            }
            return clientMessage
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: ClientMessage) {
            val jsonElementEncoder = (encoder as JsonEncoder).json
            val encodedMessage = buildJsonArray {
                val type = jsonElementEncoder.encodeToJsonElement(value.messageType)
                add(type)
                when(value){
                    is ClientEventMessage -> {
                        val eventElement = jsonElementEncoder.encodeToJsonElement(Event.serializer(), value.event)
                        add(eventElement)
                    }
                    is CloseRequest -> {
                        val idElement = jsonElementEncoder.encodeToJsonElement(value.subscriptionId)
                        add(idElement)
                    }
                    is RequestMessage -> {
                        val idElement = jsonElementEncoder.parseToJsonElement(value.subscriptionId)
                        val filtersElement = value.filters?.map { filter ->
                            jsonElementEncoder.encodeToJsonElement(NostrFilter.serializer(), filter)
                        }
                        add(idElement)
                        if (filtersElement != null) {
                            addAll(filtersElement)
                        } else {
                            error("NostrFilter error: could not build filter.")
                        }
                    }
                }
            }
            encoder.encodeJsonElement(encodedMessage)
        }
    }
}

@Serializable(with = ClientMessage.MessageSerializer::class)
data class ClientAuthMessage(
    override val messageType: String = "AUTH",
    val authEvent: Event
): ClientEventMessage(messageType, authEvent)

@Serializable(with = ClientMessage.MessageSerializer::class)
open class ClientEventMessage(
    override val messageType: String = "EVENT",
    val event: Event
) : ClientMessage(messageType) {
    override fun toString() = """
        Message type -> $messageType
        Event -> $event
    """.trimIndent()
}

@Serializable(with = ClientMessage.MessageSerializer::class)
open class RequestMessage(
    override val messageType: String = "REQ",
    open val subscriptionId: String = uuid4().bytes.decodeToString().substring(0, 5),
    val filters: List<NostrFilter>?
) : ClientMessage(messageType) {
    override fun toString() = """
        Message type -> $messageType
        SubscriptionId -> $subscriptionId
        Filters -> $filters
    """.trimIndent()

    companion object {
        fun singleFilterRequest(
            subscriptionId: String = uuid4().bytes.decodeToString().substring(0, 5),
            filter: NostrFilter
        ): RequestMessage {
            return RequestMessage(messageType = "REQ", subscriptionId, listOf(filter))
        }
    }
}

@Serializable(with = ClientMessage.MessageSerializer::class)
data class CountRequest(
    override val messageType: String = "COUNT",
    override val subscriptionId: String,
    val countFilters: List<NostrFilter>?
): RequestMessage(messageType, subscriptionId, filters = countFilters)

@Serializable(with = ClientMessage.MessageSerializer::class)
data class CloseRequest(
    override val messageType: String = "CLOSE",
    val subscriptionId: String
) : ClientMessage(messageType)

private fun decodeToRequestMessage(jsonDecoder: JsonDecoder, jsonArray: JsonArray): RequestMessage {
    var requestMarker: String? = null
    var subscriptionId: String? = null
    val filterList = mutableListOf<NostrFilter>()

    jsonArray.forEachIndexed { index, element ->
        when(index){
            0 -> {
                val requestMarkerElement = element.jsonPrimitive
                if (requestMarkerElement.isString){
                    requestMarker = requestMarkerElement.content
                } else {
                    error("Cannot decode this request filter. Marker content: ${requestMarkerElement.content}")
                }
            }
            1 -> {
                val idStringElement = element.jsonPrimitive
                subscriptionId = idStringElement.content
            }
            else -> {
                val filterElement = jsonDecoder.json.decodeFromJsonElement<NostrFilter>(element)
                filterList.add(filterElement)
            }
        }
    }
    return RequestMessage(requestMarker.toString(), subscriptionId.toString(), filterList)
}

private fun decodeToEventMessage(jsonDecoder: JsonDecoder, jsonArray: JsonArray): ClientEventMessage {
    val eventMarker = jsonArray[0].jsonPrimitive.content
    val event = jsonDecoder.json.decodeFromJsonElement(Event.serializer(), jsonArray[1])
    return ClientEventMessage(eventMarker, event)
}

private fun decodeToCloseRequest(jsonDecoder: JsonDecoder, jsonArray: JsonArray): CloseRequest {
    val closeMarker = jsonArray[0].jsonPrimitive.content
    val subscriptionId = jsonDecoder.json.decodeFromJsonElement(String.serializer(), jsonArray[1])
    return CloseRequest(closeMarker, subscriptionId)
}