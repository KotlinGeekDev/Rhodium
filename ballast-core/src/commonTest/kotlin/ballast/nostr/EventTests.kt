package ballast.nostr

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class EventTests {
    private val testEventMapper = Json
    private val testSecKeyHex = "6ba903b7888191180a0959a6d286b9d0719d33a47395c519ba107470412d2069"
    private val testPubKeyHex = "8565b1a5a63ae21689b80eadd46f6493a3ed393494bb19d0854823a757d8f35f"

    @Test
    fun `it generates the correct raw event json for obtaining the eventId`() {
        val someTags = listOf(
            Tag("p", "42365g3ghgf7gg15hj64jk")
            //Triple("#e", "546454ghgfnfg56456fgngg", "wss://relayer.fiatjaf.com")
        )
        println("Test 1:")
        val rawEventData = listOf(
            testPubKeyHex,
            "1649108200", "1", "Testing some event"
        )
        val rawEventInJson = rawEventJson0(
            rawEventData[0], rawEventData[1].toLong(), rawEventData[2].toInt(),
            someTags, rawEventData[3]
        )
        val correctRawJson =
            "[0,\"8565b1a5a63ae21689b80eadd46f6493a3ed393494bb19d0854823a757d8f35f\",1649108200,1,[[\"p\",\"42365g3ghgf7gg15hj64jk\"]],\"Testing some event\"]"
        println(rawEventInJson)
        assertEquals(rawEventInJson, correctRawJson)

    }

//    @Test
//    fun `it generates correct events with multiple tags`() {
//        assert(true)
//    }

    @Test
    fun `testing the equivalence of event ids during event generation`(){
        val profileEvent = Events.MetadataEvent(testSecKeyHex, testPubKeyHex, profile = "Name.", timeStamp = 1640839235)
        val correspondingEvent = Events.generateEvent(EventKind.METADATA.kind, emptyList(),
            "Name.", testSecKeyHex, testPubKeyHex, timeStamp = 1640839235)
        println("Profile Ev: $profileEvent")
        println("Corr. Ev: $correspondingEvent")
        assertEquals(profileEvent.id, correspondingEvent.id)
    }

    @Test
    fun `the relay auth event is generated correctly`(){
        val testAuthRelay = "host.relay.local"
        val testChallenge = "i6pore5YD2DPHOUFtnqnNclXZlZrtIfEYFUCpoOSj58YQWJd2N27pc1BaMpDqpj8"
        val authEvent = Events.AuthEvent(testSecKeyHex, testPubKeyHex, testAuthRelay, testChallenge, 1725339072)
        val authEventJson = """{"id":"3d96a19522e598a9461d0e211fb893b444ddb731d7db2a695a4dd049e0c08318","pubkey":"8565b1a5a63ae21689b80eadd46f6493a3ed393494bb19d0854823a757d8f35f","created_at":1725339072,"kind":22242,"tags":[["relay","host.relay.local"],["challenge","i6pore5YD2DPHOUFtnqnNclXZlZrtIfEYFUCpoOSj58YQWJd2N27pc1BaMpDqpj8"]],"content":"","sig":"fd3c40858109743b900f24daaf6f000112bc54d13ed9ba17ba3b9b21f566950ba2b5bce6a3a39b6034f8c670444ebafdcd3504cfd89602dd9b105fbf03bc7be1"}"""
        val regeneratedAuthEvent = deserializedEvent(authEventJson)

        assertEquals(authEvent.tags, regeneratedAuthEvent.tags)
    }
}