package ktnostr.nostr

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class NostrFilter(
    @JsonProperty("ids") val listOfIds: List<String>?,
    @JsonProperty("authors") val authorsList: List<String>?,
    @JsonProperty("kinds") val listOfKinds: List<Int>,
    @JsonProperty("#e") val eventIdList: List<String>?,
    @JsonProperty("#p") val pubkeyList: List<String>?,
    val since: Long,
    val until: Long,
    val limit: Int
)