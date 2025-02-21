package rhodium.nostr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NostrTests {
    @Test
    fun `it correctly generates the event id`() {
        val eventTestTag = listOf(
            Tag("p", "13adc511de7e1cfcf1c6b7f6365fb5a03442d7bcacf565ea57fa7770912c023d")
        )
        val rawEventTestData = listOf(
            "f86c44a2de95d9149b51c6a29afeabba264c18e2fa7c49de93424a0c56947785",
            "1640839235", "4", "uRuvYr585B80L6rSJiHocw==?iv=oh6LVqdsYYol3JfFnXTbPA=="
        )
        val eventId = getEventId(
            rawEventTestData[0], rawEventTestData[1].toLong(),
            rawEventTestData[2].toInt(), eventTestTag, rawEventTestData[3]
        )
        println("Test Event Id: $eventId")
        assertTrue(eventId == "2be17aa3031bdcb006f0fce80c146dea9c1c0268b0af2398bb673365c6444d45")
    }

    @Test
    fun `it generates a correct event`() {
        val eventTestTag = listOf(
            Tag("p", "13adc511de7e1cfcf1c6b7f6365fb5a03442d7bcacf565ea57fa7770912c023d")
        )
        val rawEventTestData = listOf(
            "f86c44a2de95d9149b51c6a29afeabba264c18e2fa7c49de93424a0c56947785",
            "1640839235", "4", "uRuvYr585B80L6rSJiHocw==?iv=oh6LVqdsYYol3JfFnXTbPA==",
            "a5d9290ef9659083c490b303eb7ee41356d8778ff19f2f91776c8dc4443388a64ffcf336e61af4c25c05ac3ae952d1ced889ed655b67790891222aaa15b99fdd"
        )
        val eventId = getEventId(
            rawEventTestData[0], rawEventTestData[1].toLong(),
            rawEventTestData[2].toInt(), eventTestTag, rawEventTestData[3]
        )
        val testEvent = Event(
            eventId, rawEventTestData[0], rawEventTestData[1].toLong(),
            rawEventTestData[2].toInt(), eventTestTag, rawEventTestData[3], rawEventTestData[4]
        )
        val eventJson = testEvent.serialize()
        val correctTestEventJson =
            """{"id":"2be17aa3031bdcb006f0fce80c146dea9c1c0268b0af2398bb673365c6444d45","pubkey":"f86c44a2de95d9149b51c6a29afeabba264c18e2fa7c49de93424a0c56947785","created_at":1640839235,"kind":4,"tags":[["p","13adc511de7e1cfcf1c6b7f6365fb5a03442d7bcacf565ea57fa7770912c023d"]],"content":"uRuvYr585B80L6rSJiHocw==?iv=oh6LVqdsYYol3JfFnXTbPA==","sig":"a5d9290ef9659083c490b303eb7ee41356d8778ff19f2f91776c8dc4443388a64ffcf336e61af4c25c05ac3ae952d1ced889ed655b67790891222aaa15b99fdd"}"""
        println(eventJson)
        println(" ")
        println(correctTestEventJson)
        assertEquals(correctTestEventJson, eventJson)
    }

    @Test
    fun `it can verify an event`() {

        val testEvent = Event(
            "2be17aa3031bdcb006f0fce80c146dea9c1c0268b0af2398bb673365c6444d45",
            "f86c44a2de95d9149b51c6a29afeabba264c18e2fa7c49de93424a0c56947785",
            1640839235, EventKind.ENCRYPTED_DM.kind,
            listOf(
                Tag("p", "13adc511de7e1cfcf1c6b7f6365fb5a03442d7bcacf565ea57fa7770912c023d")
            ),
            "uRuvYr585B80L6rSJiHocw==?iv=oh6LVqdsYYol3JfFnXTbPA==",
            "a5d9290ef9659083c490b303eb7ee41356d8778ff19f2f91776c8dc4443388a64ffcf336e61af4c25c05ac3ae952d1ced889ed655b67790891222aaa15b99fdd"
        )
        val generatedEventId = getEventId(
            testEvent.pubkey, testEvent.creationDate,
            testEvent.eventKind, testEvent.tags, testEvent.content
        )
        assertEquals(testEvent.id, generatedEventId)
    }

    @Test
    fun `it can generate a subscription event`() {

    }

    @Test
    fun `it can correctly parse a received event`() {
        val testEventJson =
            """{"id":"2be17aa3031bdcb006f0fce80c146dea9c1c0268b0af2398bb673365c6444d45","pubkey":"f86c44a2de95d9149b51c6a29afeabba264c18e2fa7c49de93424a0c56947785","created_at":1640839235,"kind":4,"tags":[["p","13adc511de7e1cfcf1c6b7f6365fb5a03442d7bcacf565ea57fa7770912c023d"]],"content":"uRuvYr585B80L6rSJiHocw==?iv=oh6LVqdsYYol3JfFnXTbPA==","sig":"a5d9290ef9659083c490b303eb7ee41356d8778ff19f2f91776c8dc4443388a64ffcf336e61af4c25c05ac3ae952d1ced889ed655b67790891222aaa15b99fdd"}"""
        val correctlyParsedEvent = Event(
            "2be17aa3031bdcb006f0fce80c146dea9c1c0268b0af2398bb673365c6444d45",
            "f86c44a2de95d9149b51c6a29afeabba264c18e2fa7c49de93424a0c56947785",
            1640839235, EventKind.ENCRYPTED_DM.kind,
            listOf(
                Tag("p", "13adc511de7e1cfcf1c6b7f6365fb5a03442d7bcacf565ea57fa7770912c023d")
            ),
            "uRuvYr585B80L6rSJiHocw==?iv=oh6LVqdsYYol3JfFnXTbPA==",
            "a5d9290ef9659083c490b303eb7ee41356d8778ff19f2f91776c8dc4443388a64ffcf336e61af4c25c05ac3ae952d1ced889ed655b67790891222aaa15b99fdd"
        )
        val event = deserializedEvent(testEventJson)
        println("Des. Event :")
        println(event)
        println("Correct Event:")
        println(correctlyParsedEvent)
        assertEquals(correctlyParsedEvent.toString(), event.toString())
    }
}