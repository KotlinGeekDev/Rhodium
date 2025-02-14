

package ballast.nostr.relay

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * The model that represents the data sent from a relay.
 * It is a base class for all specific relay message types.
 * Currently, there are two types of relay messages, represented by the tags EVENT and NOTICE.
 * You can see their corresponding representations, RelayEventMessage and RelayNotice.
 * @see RelayEventMessage
 * @see RelayNotice
 */
@Serializable(with = RelayMessage.RelayMessageSerializer::class)
sealed class RelayMessage(){
    internal companion object RelayMessageSerializer: KSerializer<RelayMessage> {
        private val listSerializer = ListSerializer(elementSerializer = String.serializer())
        override val descriptor: SerialDescriptor
            get() = listSerializer.descriptor

        override fun deserialize(decoder: Decoder): RelayMessage {
            val jsonMessageDecoder = decoder as JsonDecoder
            val messageJsonArray = jsonMessageDecoder.decodeJsonElement().jsonArray
            if (messageJsonArray.size > 4 || messageJsonArray.size < 2){
                println("Unknown JSON -> $messageJsonArray")
                throw SerializationException("Message type is not supported for message: $messageJsonArray")
            }
            return when (messageJsonArray.size) {
                3 -> {
                    val messageMarker = messageJsonArray[0].jsonPrimitive.content
                    if (messageMarker.contentEquals("EVENT")){
                        val subscriptionId = messageJsonArray[1].jsonPrimitive.content
                        val eventJsonNode = messageJsonArray[2]
                        val eventJson = if (eventJsonNode.jsonObject.isEmpty()) "" else eventJsonNode.jsonObject.toString()
                        RelayEventMessage(messageMarker, subscriptionId, eventJson)
                    }
                    else if (messageMarker.contentEquals("COUNT")){
                        val subscriptionId = messageJsonArray[1].jsonPrimitive.content
                        val countInfo = messageJsonArray[2].jsonObject
                        val actualCount = countInfo["count"]?.jsonPrimitive?.int
                        val isApproximate = countInfo["approximate"]?.jsonPrimitive?.booleanOrNull
                        if (actualCount != null) {
                            if (isApproximate != null)
                                CountResponse(
                                    messageType = messageMarker,
                                    subscriptionId = subscriptionId,
                                    count = actualCount,
                                    isApproximate = isApproximate
                                )
                            else
                                CountResponse(
                                    messageType = messageMarker,
                                    subscriptionId = subscriptionId,
                                    count = actualCount
                                )
                        }
                        else {
                            throw SerializationException("Could not parse count response: $messageJsonArray")
                        }
                    }
                    else {
                        val subscriptionId = messageJsonArray[1].jsonPrimitive.content
                        val errorMessage = messageJsonArray[2].jsonPrimitive.content
                        CloseMessage(messageMarker, subscriptionId, errorMessage)
                    }
                }
                4 -> {
                    val messageMarker = messageJsonArray[0].jsonPrimitive.content
                    val eventId = messageJsonArray[1].jsonPrimitive.content
                    val acceptedStatus = messageJsonArray[2].jsonPrimitive.boolean
                    val additionalInfo = messageJsonArray[3].jsonPrimitive.content
                    EventStatus(messageMarker, eventId, acceptedStatus, additionalInfo)
                }
                else -> {
                    val messageMarker = messageJsonArray[0].jsonPrimitive.content
                    if (messageMarker.contentEquals("EOSE")){
                        val subscriptionId = messageJsonArray[1].jsonPrimitive.content
                        RelayEose(messageMarker, subscriptionId)
                    }
                    else if (messageMarker.contentEquals("NOTICE")){
                        val noticeMessage = messageJsonArray[1].jsonPrimitive.content
                        RelayNotice(messageMarker, noticeMessage)
                    }
                    else if (messageMarker.contentEquals("AUTH")){
                        val challengeString = messageJsonArray[1].jsonPrimitive.content
                        RelayAuthMessage(messageMarker, challengeString)
                    }
                    else {
                        throw SerializationException("Unrecognized message: $messageJsonArray")
                    }
                }
            }
        }

        override fun serialize(encoder: Encoder, value: RelayMessage) {
            val messageEncoder = (encoder as JsonEncoder).json
            val encodedMessage = buildJsonArray {
                when(value){
                    is RelayEventMessage -> {
                        val messageTypeMarker = messageEncoder.encodeToJsonElement(value.messageType)
                        val subscriptionMarker = messageEncoder.encodeToJsonElement(value.subscriptionId)
                        val eventJsonMarker = messageEncoder.encodeToJsonElement(value.eventJson)
                        add(messageTypeMarker)
                        add(subscriptionMarker)
                        add(eventJsonMarker)
                    }
                    is CountResponse -> {
                        val messageTypeMarker = messageEncoder.encodeToJsonElement(value.messageType)
                        val subscriptionMarker = messageEncoder.encodeToJsonElement(value.subscriptionId)
                        val countInfo = buildJsonObject {
                            put("count", value.count)
                            //The assumption here is that the relay mentions a count is approximate if necessary.
                            //If the count is not approximate, there's no need to mention it as false.
                            with(value.isApproximate){
                                if(this == true) put("approximate", this)
                            }
                        }
                        add(messageTypeMarker)
                        add(subscriptionMarker)
                        add(countInfo)
                    }
                    is RelayAuthMessage -> {
                        val messageTag = messageEncoder.encodeToJsonElement(value.messageType)
                        val challengeString = messageEncoder.encodeToJsonElement(value.challenge)
                        add(messageTag)
                        add(challengeString)
                    }
                    is EventStatus -> {
                        val messageTypeMarker = messageEncoder.encodeToJsonElement(value.messageType)
                        val eventId = messageEncoder.encodeToJsonElement(value.eventId)
                        val acceptStatus = messageEncoder.encodeToJsonElement(value.accepted)
                        val additionalInfo = messageEncoder.encodeToJsonElement(value.additionalInfo)
                        add(messageTypeMarker)
                        add(eventId)
                        add(acceptStatus)
                        add(additionalInfo)
                    }
                    is RelayEose -> {
                        val messageTypeMarker = messageEncoder.encodeToJsonElement(value.messageType)
                        val subscriptionId = messageEncoder.encodeToJsonElement(value.subscriptionId)
                        add(messageTypeMarker)
                        add(subscriptionId)
                    }
                    is CloseMessage -> {
                        val messageTypeMarker = messageEncoder.encodeToJsonElement(value.messageType)
                        val subscriptionId = messageEncoder.encodeToJsonElement(value.subscriptionId)
                        val errorMessage = messageEncoder.encodeToJsonElement(value.errorMessage)
                        add(messageTypeMarker)
                        add(subscriptionId)
                        add(errorMessage)
                    }
                    is RelayNotice -> {
                        val messageTypeMarker = messageEncoder.encodeToJsonElement(value.messageType)
                        val contentMarker = messageEncoder.encodeToJsonElement(value.message)
                        add(messageTypeMarker)
                        add(contentMarker)
                    }
                }
            }
            encoder.encodeJsonElement(encodedMessage)
        }

    }
}

/**
 * The model representing the case when a relay sends the data we request.
 * Typically, the data is a JSON array of 3 elements, which look like this: [[EVENT, subscriptionId, eventJson]].
 * Though the eventJson returned here is in a String format, it will need to be parsed for a client to make
 * sense of it. You can do so using the provided deserializedEvent() function.
 * @see ballast.nostr.deserializedEvent
 */

@Serializable
data class RelayEventMessage(
    val messageType: String = "EVENT",
    val subscriptionId: String,
    val eventJson: String
) : RelayMessage()

/**
 * Represents the relay response when it requires authentication,
 * when the client sends an event, event filter request, or
 * count request.
 */
@Serializable
data class RelayAuthMessage(
    val messageType: String = "AUTH",
    val challenge: String
): RelayMessage()

/**
 * Represents the response sent by the relay to a count request.
 * If the count is approximate, it is indicated by the 'approximate'
 * field in the response.
 * It is of the form:
 * [[COUNT,subscriptionId,{"count":count,"approximate":true|false}]]
 */
@Serializable
data class CountResponse(
    val messageType: String = "COUNT",
    val subscriptionId: String,
    val count: Int,
    val isApproximate: Boolean = false
): RelayMessage()

/**
 * Represents the relay response to an event being sent to it.
 * It acts as a confirmation signal for client that the relay
 * has accepted(or rejected) the event, as indicated by the
 * 'accepted' field we use here.
 * In the case of rejection, the reason is provided in the
 * response.
 * It is of the form [[OK, event-id, <true|false>, message]].
 */
@Serializable
data class EventStatus(
    val messageType: String = "OK",
    val eventId: String,
    val accepted: Boolean,
    val additionalInfo: String
): RelayMessage()

/**
 * This is the message the relay sends when it finishes
 * sending all the data it has, according to the filters in
 * your request.
 * However, it will continue to send new data matching your
 * filters in realtime as they are received.
 * The response is of the form [[EOSE, subscription]].
 */
@Serializable
data class RelayEose(
    val messageType: String = "EOSE",
    val subscriptionId: String
): RelayMessage()

/**
 * This is sent by the relay without an explicit signal to
 * close the connection, or disconnection from the relay.
 * The response is of the form [[CLOSED, subscription, error]].
 */
@Serializable
data class CloseMessage(
    val messageType: String = "CLOSED",
    val subscriptionId: String,
    val errorMessage: String
): RelayMessage()

/**
 * The model representing the case when the relay returns a message different from the normal response.
 * The data is a JSON array of 2 elements, which is of the form: [[NOTICE, message]].
 * This could be due to the relay not having the data we need, or something else.
 */

@Serializable
data class RelayNotice(
    val messageType: String,
    val message: String
) : RelayMessage()




//private class RelayMessageConverter : JsonDeserializer<RelayMessage>() {
//
//    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): RelayMessage {
//        val messageTree = p?.readValueAsTree<ArrayNode>()
//        if (messageTree?.size()!! > 3 || messageTree.size() < 2) {
//            throw Exception("This message type is not supported.")
//        }
//
//
//        return if (messageTree.size() == 3) {
//            val messageType = messageTree[0].asText()
//            val subscriptionId = messageTree[1].asText()
//            val content = if (messageTree[2].isContainerNode) messageTree[2].toString() else ""
//            RelayEventMessage(messageType, subscriptionId, content)
//        } else {
//            val messageType = messageTree[0].asText()
//            val message = messageTree[1].asText()
//            RelayNotice(messageType, message)
//        }
//    }
//
//}


//----This function is kept for legacy and last-resort purposes----
//fun String.toRelayMessage(): RelayMessage {
//    val json = this
//    val eventMessageRaw = eventMapper.readValue<List<String>>(json)
//    if (eventMessageRaw.size > 3 || eventMessageRaw.size < 2){
//        println("Unsupported relay message")
//        throw RelayMessageError("This relay message cannot be understood.\n Message: $eventMessageRaw")
//    }
//    else {
//        return when(eventMessageRaw.first()){
//            "EVENT" -> RelayEventMessage(eventMessageRaw[0], eventMessageRaw[1], eventMessageRaw[2])
//            "NOTICE" -> RelayNotice(eventMessageRaw[0], eventMessageRaw[1])
//            else -> throw RelayMessageError("This message does not conform to the standard format of" +
//                    "relay messages. \n MessageJson: $this")
//        }
//    }
//}