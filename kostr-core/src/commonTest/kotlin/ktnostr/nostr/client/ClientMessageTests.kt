package ktnostr.nostr.client

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktnostr.nostr.EventKind
import ktnostr.nostr.NostrFilter
import kotlin.jvm.JvmStatic
import kotlin.test.Test
import kotlin.test.assertEquals

data class TV(val nostrFilters: List<NostrFilter>, val nostrFilterJson: String)

class ClientMessageTests {
    private val testEventMapper = Json
    private fun stringifyMessage(message: ClientMessage) = testEventMapper.encodeToString(message)

    @Test
    fun `close request decodes properly`(){
        val closeRequest = CloseRequest(subscriptionId = "mySub")
        val closeRequestJson = """["CLOSE", "mySub"]"""
        val decodedCloseRequest = testEventMapper.decodeFromString<ClientMessage>(closeRequestJson)
        assertEquals(closeRequest, decodedCloseRequest)
    }

    @Test
    fun `single filter request message encodes properly`(){
        val requestMessage = RequestMessage(subscriptionId = "mySub", filters = testVectors().first().nostrFilters)
        val resultingJson = stringifyMessage(requestMessage)
        assertEquals(testVectors().first().nostrFilterJson, resultingJson)
    }

    @Test
    fun `single filter request message decodes properly`(){
        val filterRequestJson = testVectors().first().nostrFilterJson
        val resultingFilter = testEventMapper.decodeFromString<ClientMessage>(filterRequestJson)
        val correctRequest = RequestMessage(subscriptionId = "mySub", filters = testVectors().first().nostrFilters)
        assertEquals(correctRequest.toString(), resultingFilter.toString())
    }

    @Test
    fun `multiple filter request message encodes properly`(){
        val requestMessage =
            RequestMessage(subscriptionId = "mySub", filters = testVectors().last().nostrFilters)
        val requestJson = stringifyMessage(requestMessage)
        assertEquals(testVectors().last().nostrFilterJson, requestJson)
    }

    @Test
    fun `single filter auth request encodes properly`(){
        val countRequest = CountRequest(subscriptionId = "mySub", countFilters = countTestVectors().first().nostrFilters)
        val correspondingJson = stringifyMessage(countRequest)
        assertEquals(countTestVectors().first().nostrFilterJson, correspondingJson)
    }

    @Test
    fun `multiple filter auth request encodes properly`(){
        val countRequest = CountRequest(subscriptionId = "mySub", countFilters = countTestVectors().last().nostrFilters)
        val correspondingJson = stringifyMessage(countRequest)
        assertEquals(countTestVectors().last().nostrFilterJson, correspondingJson)
    }

    companion object {
        private val filterOne = NostrFilter.newFilter()
            .idList("event_id_1", "event_id_2", "event_id_3")
            .authors("author_pubkey_1", "author_pubkey_2")
            .kinds(EventKind.TEXT_NOTE.kind)
            .eventTagList("ref_event_id_1", "ref_event_id_2")
            .pubkeyTagList("ref_pubkey_1")
            .since(1653822739L - 24 * 60 * 60)
            .until(1653822739L)
            .limit(25)
            .build()

        private val filterTwo = NostrFilter.newFilter()
            .idList("event_id_4", "event_id_5")
            .authors("author_pubkey_3", "author_pubkey_4")
            .kinds(EventKind.METADATA.kind, EventKind.RELAY_RECOMMENDATION.kind)
            .eventTagList("ref_event_id_3", "ref_event_id_4")
            .pubkeyTagList("ref_pubkey_2", "ref_pubkey_3", "ref_pubkey_4")
            .since(1653822739L - 24 * 60 * 60)
            .until(1653822739L)
            .limit(10)
            .build()

        @JvmStatic
        fun countTestVectors() = listOf(
            TV(listOf(filterOne), """["COUNT","mySub",{"ids":["event_id_1","event_id_2","event_id_3"],"authors":["author_pubkey_1","author_pubkey_2"],"kinds":[1],"#e":["ref_event_id_1","ref_event_id_2"],"#p":["ref_pubkey_1"],"since":1653736339,"until":1653822739,"limit":25}]"""),
            TV(listOf(filterOne, filterTwo), """["COUNT","mySub",{"ids":["event_id_1","event_id_2","event_id_3"],"authors":["author_pubkey_1","author_pubkey_2"],"kinds":[1],"#e":["ref_event_id_1","ref_event_id_2"],"#p":["ref_pubkey_1"],"since":1653736339,"until":1653822739,"limit":25},{"ids":["event_id_4","event_id_5"],"authors":["author_pubkey_3","author_pubkey_4"],"kinds":[0,2],"#e":["ref_event_id_3","ref_event_id_4"],"#p":["ref_pubkey_2","ref_pubkey_3","ref_pubkey_4"],"since":1653736339,"until":1653822739,"limit":10}]""")
        )

        @JvmStatic
        fun testVectors() = listOf(
            TV(listOf(filterOne), """["REQ","mySub",{"ids":["event_id_1","event_id_2","event_id_3"],"authors":["author_pubkey_1","author_pubkey_2"],"kinds":[1],"#e":["ref_event_id_1","ref_event_id_2"],"#p":["ref_pubkey_1"],"since":1653736339,"until":1653822739,"limit":25}]"""),
            TV(listOf(filterTwo), """["REQ","mySub",{"ids":["event_id_4","event_id_5"],"authors":["author_pubkey_3","author_pubkey_4"],"kinds":[0,2],"#e":["ref_event_id_3","ref_event_id_4"],"#p":["ref_pubkey_2","ref_pubkey_3","ref_pubkey_4"],"since":1653736339,"until":1653822739,"limit":10}]"""),
            TV(listOf(filterOne, filterTwo), """["REQ","mySub",{"ids":["event_id_1","event_id_2","event_id_3"],"authors":["author_pubkey_1","author_pubkey_2"],"kinds":[1],"#e":["ref_event_id_1","ref_event_id_2"],"#p":["ref_pubkey_1"],"since":1653736339,"until":1653822739,"limit":25},{"ids":["event_id_4","event_id_5"],"authors":["author_pubkey_3","author_pubkey_4"],"kinds":[0,2],"#e":["ref_event_id_3","ref_event_id_4"],"#p":["ref_pubkey_2","ref_pubkey_3","ref_pubkey_4"],"since":1653736339,"until":1653822739,"limit":10}]""")
        )
    }
}
