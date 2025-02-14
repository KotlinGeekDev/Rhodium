package rhodium

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The function takes a Unix timestamp in and returns
 * a human-readable date and time.
 * @param timestamp The Unix timestamp as a Long
 * @return A human-readable date and time, as a string.
 */

fun formattedDateTime(timestamp: Long): String {
    return Instant.fromEpochSeconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault()).toString()


//    return Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
//        .format(DateTimeFormatter.ofPattern("uuuu MMM d hh:mm a"))
}


/**
 * The function below returns the current Unix timestamp.
 */
fun currentSystemTimestamp(): Long = Clock.System.now().epochSeconds