package rhodium.nostr.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rhodium.nostr.Event
import rhodium.nostr.EventKind
import rhodium.nostr.Tag
import rhodium.nostr.eventMapper


class MetadataEvent(
    id: String,
    pubkey: String,
    creationDate: Long,
    tags: List<Tag>,
    content: String,
    signature: String
): Event(id, pubkey, creationDate, eventKind = EventKind.METADATA.kind, tags, content, signature) {

    fun userInfo(): UserInfo = eventMapper.decodeFromString(content)

}

@Serializable()
data class UserInfo(
    val name: String,
    @SerialName("display_name") val displayName: String? = null,
    val about: String?,
    val picture: String? = null,
    val banner: String? = null,
    @SerialName("nip05") val address: String? = null,
    val website: String? = null,
)