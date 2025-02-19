package rhodium.nostr.relay

class Relay(
    val relayURI: String,
    val readPolicy: Boolean = true,
    val writePolicy: Boolean = true
) {
    companion object {
        fun fromUrl(address: String): Relay {
            return Relay(address)
        }
    }

    override fun toString(): String {
        return "Relay(url=$relayURI, read=$readPolicy, write=$writePolicy)"
    }
}