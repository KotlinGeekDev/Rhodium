package rhodium.nostr

//The general class of Nostr Errors.
sealed class NostrException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

/**
 * This error is thrown when something goes wrong during the Nip05 parsing process.
 */
class Nip05ValidationError(override val message: String) : NostrException(message)

/**
 * This error type is used when an event is invalid.
 * For example, when the event id is not valid, or
 * when the event signature is not valid.
 */
class EventValidationError(override val message: String) : NostrException(message)

/**
 * This error type is used when we receive an event whose kind we
 * do not understand and/or support.
 * This is probably an indication that it(the kind) needs to be supported.
 */
class UnsupportedKindError(override val message: String) : NostrException(message)

/**
 * For handling relay and relay-related errors.
 * For example, to be used when sending data to a relay,
 * or for relay connection management.
 */
open class RelayError(override val message: String) : NostrException(message)

class RelayMessageError(override val message: String) : RelayError(message)


