package ktnostr.net

import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import ktnostr.formattedDateTime
import ktnostr.nostr.Event
import ktnostr.nostr.client.ClientMessage
import ktnostr.nostr.client.RequestMessage
import ktnostr.nostr.deserializedEvent
import ktnostr.nostr.eventMapper
import ktnostr.nostr.relay.*
import kotlin.coroutines.CoroutineContext


class NostrService(private val relayPool: RelayPool = RelayPool()): CoroutineScope {
    private val serviceDispatcher = Dispatchers.IO.limitedParallelism(
        relayPool.getRelays().size,
        name = "NostrServiceDispatcher"
    )
    private val serviceMutex = Mutex()

    private val client = httpClient {
        install(WebSockets){

        }

        install(Logging){

        }
    }

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

    private suspend fun requestFromRelay(
        requestMessage: RequestMessage,
        relay: Relay,
        onRelayMessage: suspend (Relay, RelayMessage) -> Unit,
        onRequestError: (Relay, Throwable) -> Unit
    ) {
        val requestJson = eventMapper.encodeToString(requestMessage)

        println("Coroutine Scope @ ${relay.relayURI}")
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
                                println("Received a close frame with reason: ${frame.readReason()}")
                            }
                            else if (frame is Frame.Binary) {
                                println("Received binary data : ${frame.readBytes()}")
                            }
                            else if (frame is Frame.Text) {
                                val message = eventMapper.decodeFromString<RelayMessage>(frame.readText())
                                when (message) {
                                    is RelayEventMessage -> {
                                        println("Event message received from ${endpoint.relayURI}")
                                        val event = deserializedEvent(message.eventJson)
                                        println("Event created on ${formattedDateTime(event.creationDate)}")
                                        println(event.content)
                                        results.add(event)

                                    }

                                    is CountResponse -> {
                                        println("Received Count message: $message")
                                    }

                                    is RelayAuthMessage -> {
                                        println("Received Auth message: $message")
                                        serviceMutex.withLock {
                                            if (relayAuthCache.put(endpoint, message) != null){
                                                println("Added auth message <-$message-> to cache.")
                                            }
                                        }
                                    }

                                    is EventStatus -> {
                                        println("Received a status for the sent event:")
                                        println(message)
                                    }

                                    is RelayEose -> {
                                        relayEoseCount.update { it + 1 }
                                        println("Relay EOSE received from ${endpoint.relayURI}")
                                        println(message)
                                        break

                                    }

                                    is CloseMessage -> {
                                        relayEoseCount.update { it + 1 }
                                        println("Closed by Relay ${endpoint.relayURI} with reason: ${message.errorMessage}")
                                        this.close()
                                    }

                                    is RelayNotice -> {
                                        println("Received a relay notice: $message")
                                    }
                                }

                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Failed to connect to ${endpoint.relayURI}: ${e.message}")
                    relayErrorCount.update { it + 1 }
                }
            }
            jobs.add(job)
        }


        jobs.forEach { job -> job.join() }
        if (jobs.all { it.isCompleted }) {
            println("EoseCount : ${relayEoseCount.value}")
            println("RelayErrorCount: ${relayErrorCount.value}")
//            if (relayEoseCount.value + relayErrorCount.value == endpoints.size){
//                stopService()
//
//            }
            relayEoseCount.update { 0 }
            relayErrorCount.update { 0 }
        }

        return results
    }

    fun stopService(){
        if (this.isActive) this.serviceDispatcher.cancel()
    }

}

