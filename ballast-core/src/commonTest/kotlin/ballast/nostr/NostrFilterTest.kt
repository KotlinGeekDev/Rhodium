package ballast.nostr

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ballast.currentSystemTimestamp
import kotlin.test.Test
import kotlin.test.assertEquals

class NostrFilterTest {
    private val nostrFilterEventMapper = Json

    // For the first filter
    private val eventIdList = listOf("event_id_1", "event_id_2", "event_id_3")
    private val authorList = listOf("author_pubkey_1", "author_pubkey_2")
    private val listOfKinds = listOf(EventKind.TEXT_NOTE.kind)
    private val referencedEventIds = listOf("ref_event_id_1", "ref_event_id_2")
    private val referencedProfiles = listOf("ref_pubkey_1")
    private val upperTimeLimit = currentSystemTimestamp()
    private val lowerTimeLimit = upperTimeLimit - 24 * 60 * 60
    private val maxEventLimit = 25


    // For the second filter
//    private val secondEventIdList = listOf("event_id_4", "event_id_5")
//    private val secondAuthorList = listOf("author_pubkey_3", "author_pubkey_4")
//    private val secondKindList = listOf(EventKind.METADATA, EventKind.RELAY_RECOMMENDATION)
//    private val referencedEventIdList = listOf("ref_event_id_3", "ref_event_id_4")
//    private val referencedProfileList = listOf("ref_pubkey_2", "ref_pubkey_3", "ref_pubkey_4")
//    private val secondMaxEventLimit = 10
//    val filter_2 = NostrFilter(secondEventIdList, secondAuthorList, secondKindList, referencedEventIdList,
//        referencedProfileList, lowerTimeLimit, upperTimeLimit, secondMaxEventLimit)

    @Test
    fun `it serializes the nostr filter correctly`() {
        val currentTimestamp = 1653822739L
        val previousTimestamp = currentTimestamp - 24 * 60 * 60
        val filter = NostrFilter.newFilter()
            .idList(*eventIdList.toTypedArray())
            .authors(*authorList.toTypedArray())
            .kinds(*listOfKinds.toIntArray())
            .eventTagList(*referencedEventIds.toTypedArray())
            .pubkeyTagList(*referencedProfiles.toTypedArray())
            .since(previousTimestamp)
            .until(currentTimestamp)
            .limit(maxEventLimit)
            .build()

        val correctFilterJson =
            """{"ids":["event_id_1","event_id_2","event_id_3"],"authors":["author_pubkey_1","author_pubkey_2"],"kinds":[1],"#e":["ref_event_id_1","ref_event_id_2"],"#p":["ref_pubkey_1"],"since":1653736339,"until":1653822739,"limit":25}"""

        val filterJson = nostrFilterEventMapper.encodeToString(filter)
        println("Correct filterJson: \n $correctFilterJson")
        println("Generated filterJson: \n $filterJson")
        assertEquals(filterJson, correctFilterJson)

    }

    @Test
    fun `the timestamp for the filter is correctly generated`() {
        val filter = NostrFilter.newFilter()
            .idList(*eventIdList.toTypedArray())
            .authors(*authorList.toTypedArray())
            .kinds(*listOfKinds.toIntArray())
            .eventTagList(*referencedEventIds.toTypedArray())
            .pubkeyTagList(*referencedProfiles.toTypedArray())
            .since(lowerTimeLimit)
            .until(upperTimeLimit)
            .limit(maxEventLimit)
            .build()

        val cloneFilter = NostrFilter.newFilter()
            .idList(*eventIdList.toTypedArray())
            .authors(*authorList.toTypedArray())
            .kinds(*listOfKinds.toIntArray())
            .eventTagList(*referencedEventIds.toTypedArray())
            .pubkeyTagList(*referencedProfiles.toTypedArray())
            .since(lowerTimeLimit)
            .until(upperTimeLimit)
            .limit(maxEventLimit)
            .build()

        val filterJson = nostrFilterEventMapper.encodeToString(filter)
        val cloneFilterJson = nostrFilterEventMapper.encodeToString(cloneFilter)

        println(filter)
        println(cloneFilter)
        println("FilterJson: $filterJson")
        println("Clone filterJson: $cloneFilterJson")
        assertEquals(filterJson, cloneFilterJson)
    }

    @Test
    fun `another test for correct serialization`(){
        val currentTimestamp = 1653822739L
        val previousTimestamp = currentTimestamp - 24 * 60 * 60
        val textEventFilter = NostrFilter.newFilter()
            .idList()
            .authors()
            .kinds(EventKind.TEXT_NOTE.kind)
            .eventTagList()
            .pubkeyTagList()
            .since(previousTimestamp)
            .until(currentTimestamp)
            .limit(30)
            .build()
        val filterJson = nostrFilterEventMapper.encodeToString(textEventFilter)
        val correctRequestJson = """{"kinds":[1],"since":1653736339,"until":1653822739,"limit":30}"""
        println(filterJson)
        assertEquals(correctRequestJson, filterJson)
    }

}