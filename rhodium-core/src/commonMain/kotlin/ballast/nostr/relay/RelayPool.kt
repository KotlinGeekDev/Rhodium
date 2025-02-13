package ballast.nostr.relay

import ballast.nostr.RelayError
import kotlin.jvm.JvmStatic

class RelayPool {

    private val relayList: MutableList<Relay> = mutableListOf()
    constructor(){
        getDefaultRelays().forEach {
            relayList.add(it)
        }
    }

    constructor(relays: List<Relay>) : this() {
        relays.forEach { relayList.add(it) }
    }

    fun getRelays() = relayList.toList()

    fun addRelay(relay: Relay) {
        if (relayList.add(relay))
            return
        else throw RelayError("The relay ${relay.relayURI} could not be added.")
    }

    fun addRelays(vararg relayUrls: String){
        relayUrls.forEach { url ->
            addRelay(Relay(url))
        }
    }

    fun addRelays(relays: Collection<Relay>) {
        relays.forEach { relayList.add(it) }
    }

    fun addRelayList(listOfRelays: Collection<String>) {
        val relayRefs = listOfRelays.map { Relay(it) }
        addRelays(relayRefs)
    }

    fun removeRelay(relay: Relay) {
        relayList.remove(relay)
    }

    fun clearPool() {
        relayList.clear()
    }

    companion object {

        fun fromUrls(vararg relayUris: String): RelayPool {
            val relayList = relayUris.map { Relay(it) }
            return RelayPool(relayList)
        }

        fun fromUrls(urlList: Collection<String>): RelayPool {
            val relayList = urlList.map { Relay(it) }
            return RelayPool(relayList)
        }

        @JvmStatic
        fun getDefaultRelays(): List<Relay> = listOf(
            Relay("wss://nostr-pub.wellorder.net"),
            Relay("wss://relay.damus.io"),
            Relay("wss://relay.nostr.wirednet.jp"),
            Relay("wss://relay.nostr.band"),
        )
    }

}


