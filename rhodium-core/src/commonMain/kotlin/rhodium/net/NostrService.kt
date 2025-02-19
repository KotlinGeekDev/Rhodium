package rhodium.net

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import rhodium.formattedDateTime
import rhodium.logging.serviceLogger
import rhodium.nostr.*
import rhodium.nostr.client.ClientMessage
import rhodium.nostr.client.RequestMessage
import rhodium.nostr.events.MetadataEvent
import rhodium.nostr.relay.*
import kotlin.coroutines.CoroutineContext


class NostrService(
    val relayPool: RelayPool = RelayPool(),
    private val client: HttpClient = httpClient { install(WebSockets){} }
): CoroutineScope {
    private val serviceDispatcher = Dispatchers.IO.limitedParallelism(
        relayPool.getRelays().size,
        name = "NostrServiceDispatcher"
    )
    private val serviceMutex = Mutex()

    override val coroutineContext: CoroutineContext
        get() = serviceDispatcher

//    suspend fun publishEvent(event: ClientMessage) {
//        when(event) {
//            is ClientEventMessage -> sendEvent(event)
//            is RequestMessage -> sendEvent(event)
//            else -> println("Sending these types is not yet implemented.")
//        }
//    }

    suspend fun sendEvent(message: ClientMessage, onRelayMessage: (Relay, RelayMessage) -> Unit){
        val eventJson = eventMapper.encodeToString(message)
        relayPool.getRelays().forEach {
            client.webSocket(it.relayURI){
                send(eventJson)
                for (frame in incoming){
                    val messageJson = (frame as Frame.Text).readText()
                    val decodedMessage = eventMapper.decodeFromString<RelayMessage>(messageJson)
                    onRelayMessage(it, decodedMessage)

                }
            }
        }
    }

    suspend fun request(
        requestMessage: RequestMessage,
        onRequestError: (Relay, Throwable) -> Unit,
        onRelayMessage: suspend (relay: Relay, received: RelayMessage) -> Unit,
    ) {


        for (relay in relayPool.getRelays()) {
            requestFromRelay(
                requestMessage,
                relay,
                onRelayMessage = onRelayMessage,
                onRequestError = onRequestError
            )
        }
//        val requestJson = eventMapper.encodeToString(requestMessage)
//
//        for (relay in relayPool.getRelays()) {
//            var webSocketSession: WebSocketSession? = null
//            println("Coroutine Scope @ ${relay.relayURI}")
//            try {
//                webSocketSession = client.webSocketSession(urlString = relay.relayURI)
//                webSocketSession.send(requestJson)
//
//                    for (frame in webSocketSession.incoming) {
//                        val received = (frame as Frame.Text).readText()
//                        val receivedMessage = eventMapper.decodeFromString<RelayMessage>(received)
//                        onRelayMessage(relay, receivedMessage)
//                    }
//
//            } catch (e: kotlinx.io.IOException) {
//                onRequestError(relay, e)
//                if (webSocketSession?.isActive == true) webSocketSession.cancel()
//            } catch (err: Exception) {
//                onRequestError(relay, err)
//            } catch (t: Throwable) {
//                onRequestError(relay, t)
//            }
//        }
    }

    private fun requestFromRelay(
        requestMessage: RequestMessage,
        relay: Relay,
        onRelayMessage: suspend (Relay, RelayMessage) -> Unit,
        onRequestError: (Relay, Throwable) -> Unit
    ) {
        val requestJson = eventMapper.encodeToString(requestMessage)

        launch {
            serviceLogger.d("Coroutine Scope @ ${relay.relayURI}")

            try {
                client.webSocket(urlString = relay.relayURI) {
                    send(requestJson)

                    for (frame in incoming) {
                        val received = (frame as Frame.Text).readText()
                        val receivedMessage = eventMapper.decodeFromString<RelayMessage>(received)
                        onRelayMessage(relay, receivedMessage)
                    }
                }
            } catch (e: kotlinx.io.IOException) {
                onRequestError(relay, e)
            } catch (err: Exception) {
                onRequestError(relay, err)
            } catch (t: Throwable) {
                onRequestError(relay, t)
            }
        }
    }

    suspend fun requestWithResult(
        requestMessage: RequestMessage,
        endpoints: List<Relay> = relayPool.getRelays()
    ): List<Event> {

        val results = mutableListOf<Event>()
        val relayAuthCache: MutableMap<Relay, RelayAuthMessage> = mutableMapOf()
        val jobs = mutableListOf<Job>()
        val requestJson = eventMapper.encodeToString(requestMessage)
        val relayErrorCount = atomic(0)
        val relayEoseCount = atomic(0)

        for (endpoint in endpoints) {
            val job = this.launch {
                println(this.coroutineContext.toString())
                try {
                    client.webSocket(endpoint.relayURI) {
                        send(requestJson)

                        for (frame in incoming) {
                            if (frame is Frame.Close) {
                                serviceLogger.i("Received a close frame with reason: ${frame.readReason()}")
                            }
                            else if (frame is Frame.Binary) {
                                serviceLogger.i("Received binary data : ${frame.readBytes()}")
                            }
                            else if (frame is Frame.Text) {
                                val message = eventMapper.decodeFromString<RelayMessage>(frame.readText())
                                when (message) {
                                    is RelayEventMessage -> {
                                        serviceLogger.d("Event message received from ${endpoint.relayURI}")
                                        val event = deserializedEvent(message.eventJson)
                                        serviceLogger.d("Event created on ${formattedDateTime(event.creationDate)}")
                                        serviceLogger.d(event.content)
                                        results.add(event)

                                    }

                                    is CountResponse -> {
                                        serviceLogger.d("Received Count message: $message")
                                    }

                                    is RelayAuthMessage -> {
                                        serviceLogger.i("Received Auth message: $message")
                                        serviceMutex.withLock {
                                            if (relayAuthCache.put(endpoint, message) != null){
                                                serviceLogger.i("Added auth message <-$message-> to cache.")
                                            }
                                        }
                                    }

                                    is EventStatus -> {
                                        serviceLogger.d("Received a status for the sent event:")
                                        serviceLogger.d(message.toString())
                                    }

                                    is RelayEose -> {
                                        relayEoseCount.update { it + 1 }
                                        serviceLogger.d("Relay EOSE received from ${endpoint.relayURI}")
                                        serviceLogger.d(message.toString())
                                        break

                                    }

                                    is CloseMessage -> {
                                        relayEoseCount.update { it + 1 }
                                        serviceLogger.w("Closed by Relay ${endpoint.relayURI} with reason: ${message.errorMessage}")
                                        this.close()
                                    }

                                    is RelayNotice -> {
                                        serviceLogger.i("Received a relay notice: $message")
                                    }
                                }

                            }
                        }
                    }
                } catch (e: Exception) {
                    serviceLogger.e("Failed to connect to ${endpoint.relayURI}: ${e.message}", e)
                    relayErrorCount.update { it + 1 }
                }
            }
            jobs.add(job)
        }


        jobs.forEach { job -> job.join() }
        if (jobs.all { it.isCompleted }) {
            serviceLogger.d("EoseCount : ${relayEoseCount.value}")
            serviceLogger.d("RelayErrorCount: ${relayErrorCount.value}")

//            if (relayEoseCount.value + relayErrorCount.value == endpoints.size){
//                stopService()
//
//            }
            relayEoseCount.update { 0 }
            relayErrorCount.update { 0 }
        }

        return results
    }

    suspend fun getMetadataFor(profileHex: String, preferredRelays: List<String>): MetadataEvent {
        val profileRequest = RequestMessage.singleFilterRequest(
            filter = NostrFilter.newFilter()
                .kinds(EventKind.METADATA.kind)
                .authors(profileHex)
                .limit(1)
                .build()
        )
        val potentialResults = if (preferredRelays.isEmpty())
            requestWithResult(profileRequest) else requestWithResult(profileRequest, preferredRelays.map { Relay(it) })

        return potentialResults.maxBy { it.creationDate }
            .let { MetadataEvent(it.id, it.pubkey, it.creationDate, it.tags, it.content, it.eventSignature) }
    }

    suspend fun fetchRelayListFor(profileHex: String, fetchRelays: List<String>): List<Relay> {
        val relayListRequest = RequestMessage.singleFilterRequest(
            filter = NostrFilter.newFilter()
                .kinds(EventKind.RELAY_LIST.kind)
                .authors(profileHex)
                .limit(1)
                .build()
        )
        val potentialResults = if (fetchRelays.isEmpty())
            requestWithResult(relayListRequest) else requestWithResult(relayListRequest, fetchRelays.map { Relay(it) })

        val mostUpdatedRelayList = potentialResults.maxBy { it.creationDate }
        val potentialRelays = mostUpdatedRelayList.tags.filter { tag -> tag.identifier == "r" }
        if (potentialRelays.isEmpty()) {
            return emptyList()
        }
        else {
            val relays = potentialRelays.map { relayTag ->
                Relay(
                    relayTag.description,
                    readPolicy = if (relayTag.content == null) true else relayTag.content.contentEquals("read"),
                    writePolicy = if (relayTag.content == null) true else relayTag.content.contentEquals("write")
                )
            }

            return relays
        }
    }

    fun clearRelayPool() {
        relayPool.clearPool()
    }

    fun stopService(){
        if (this.isActive) this.serviceDispatcher.cancel()
    }

}

