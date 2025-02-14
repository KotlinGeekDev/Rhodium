package rhodium.nostr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NostrFilter private constructor(
    @SerialName("ids") private val listOfIds: List<String>? = null,
    @SerialName("authors") private val authorsList: List<String>? = null,
    @SerialName("kinds") private val listOfKinds: List<Int>,
    @SerialName("#e") private val eventIdList: List<String>? = null,
    @SerialName("#p") private val pubkeyList: List<String>? = null,
    @SerialName("#t") private val topicList: List<String>? = null,
    private val since: Long? = null,
    private val until: Long? = null,
    private val limit: Int = 1
) {

    override fun toString() = """
        Ids:$listOfIds
        Authors:$authorsList
        Kinds:$listOfKinds
        Tags
          Id:$eventIdList
          Pubkey:$pubkeyList
          Topic:$topicList
        Since:$since
        Until:$until
        Limit:$limit  
    """.trimIndent()

    companion object {
        fun newFilter() = Builder()
    }

    class Builder {
        private var listOfIds: List<String>? = null
        private var authorsList: List<String>? = null
        private var listOfKinds: List<Int> = emptyList()
        private var eventTagList: List<String>? = null
        private var pubkeyTagList:List<String>? = null
        private var topicList: List<String>? = null
        private var since: Long? = null
        private var until: Long? = null
        private var limit: Int = 1

        fun idList(vararg iDList: String = emptyArray()) = apply {
            listOfIds = if (iDList.isEmpty()) null else iDList.toList()
        }

        fun authors(vararg authorList: String = emptyArray()) = apply {
            authorsList = if (authorList.isEmpty()) null else authorList.toList()
        }

        fun kinds(vararg kindList: Int) = apply {
            listOfKinds = kindList.toList()
        }

        fun eventTagList(vararg listOfEventTags: String = emptyArray()) = apply {
            eventTagList = if (listOfEventTags.isEmpty()) null else listOfEventTags.toList()
        }

        fun pubkeyTagList(vararg pubkeyList: String = emptyArray()) = apply {
            pubkeyTagList = if (pubkeyList.isEmpty()) null else pubkeyList.toList()
        }

        fun topics(vararg listOfTopics: String = emptyArray()) = apply {
            topicList = if (listOfTopics.isEmpty()) null else listOfTopics.toList()
        }

        fun since(timeStamp: Long? = null) = apply {
            since = timeStamp
        }

        fun until(timeStamp: Long? = null) = apply {
            until = timeStamp
        }

        fun limit(receivingEventLimit: Int) = apply {
            limit = receivingEventLimit
        }

        fun build() = NostrFilter(
            listOfIds = listOfIds,
            authorsList = authorsList,
            listOfKinds = listOfKinds,
            eventIdList = eventTagList,
            pubkeyList = pubkeyTagList,
            topicList = topicList,
            since = since,
            until = until,
            limit = limit
        )
    }
}