package ballast.nostr.relay

import ballast.nostr.RelayError
import kotlin.jvm.JvmStatic

class RelayPool {

    private var relayList: MutableList<Relay>
    constructor(){
        relayList = getDefaultRelays().toMutableList()
    }

    constructor(relays: List<Relay>) : this() {
        this.relayList = relays as MutableList<Relay>
    }

    fun getRelays() = if (relayList.isEmpty()) getDefaultRelays() else relayList.toList()

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

    fun removeRelay(relay: Relay) {
        relayList.remove(relay)
    }

    companion object {

        fun fromUrls(vararg relayUris: String): RelayPool {
            val relayList = relayUris.map { Relay(it) }
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


