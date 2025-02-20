//@file:JvmName("Tag")
package rhodium.nostr

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The model representing the event tag. The event tag carries
 * information such as what identifies the tag('p', 'e', etc.), the tag's
 * content, or description(a referenced pubkey, event, etc.),
 * a recommended relay url(optional), which you can add to the list
 * of relays you already have, and a petname or username(optional),
 * when a tag contains an identity's alias(or username).
 *
 * @param identifier The tag key, as a string
 * @param description The tag value, as a string
 * @param content (optional) A custom field with no particular meaning(generally used for relay recommendation), as a string
 */


@Serializable(with = Tag.TagSerializer::class)
data class Tag(
    val identifier: String, val description: String,
    val content: String? = null,
    val customContent: String? = null
) {

    @OptIn(ExperimentalSerializationApi::class)
    internal class TagSerializer : KSerializer<Tag> {
        private val builtinSerializer = arraySerializer
        override val descriptor: SerialDescriptor = SerialDescriptor("Tag", builtinSerializer.descriptor)
        override fun serialize(encoder: Encoder, value: Tag) {
            val arrayOfValues = with(value){
                buildList {
                    add(identifier)
                    add(description)
                    if (content != null) add(content)
                    if (customContent != null) add(customContent)
                }.toTypedArray()
            }
            encoder.encodeSerializableValue(builtinSerializer, arrayOfValues)
        }

        override fun deserialize(decoder: Decoder): Tag {
            val array = decoder.decodeSerializableValue(builtinSerializer)
            val arraySize = array.size
            return when {
                arraySize > 4 || arraySize < 2 -> throw Exception("Incorrect tag format.")
                arraySize == 4 -> Tag(array[0], array[1], array[2], array[3])
                arraySize == 3 -> Tag(array[0], array[1], array[2])
                else -> Tag(array[0], array[1])
            }
        }
    }

}

//----Kept for legacy purposes, or when serialization above does not work----
///**
// * Transforms a list of tags to a list of string arrays.
// * This function is necessary in order for the tag structure to
// * be serialized correctly.
// *
// * @return An array of string arrays
// */
//fun List<Tag>.toStringList(): List<List<String>> {
//    val tagStringList: MutableList<List<String>> = mutableListOf()
//    this.forEach { tag ->
//        val elementList: List<String> = if (tag.customContent != null){
//            listOf(tag.identifier, tag.content, tag.customContent)
//        } else {
//            listOf(tag.identifier, tag.content)
//        }
//        tagStringList.add(elementList)
//    }
//
//    return tagStringList.toList()
//}
