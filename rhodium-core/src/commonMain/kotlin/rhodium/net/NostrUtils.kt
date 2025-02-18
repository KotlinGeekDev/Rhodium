package rhodium.net

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import rhodium.nostr.Nip05ValidationError
import rhodium.nostr.arraySerializer
import rhodium.nostr.eventMapper

object NostrUtils {

    suspend fun getProfileInfoFromAddress(
        nip05: String,
        client: HttpClient = httpClient()
    ): Array<String> {
        val nameWithDomain = nip05.split("@")
        if (nameWithDomain.size > 2 || nameWithDomain.isEmpty()) {
            throw Nip05ValidationError("Likely a malformed address.")
        }
        else if (nameWithDomain.size == 1) {
            val domain = nameWithDomain[0]
            if (!UrlUtil.isValidUrl(domain)) throw Nip05ValidationError("Invalid identifier.")
            val urlToUse = "https://${domain}/.well-known/nostr.json?name=_"
            return fetchDetails(urlToUse, "_", client)

        }
        else {
            val finalUrl = "https://${nameWithDomain[1]}/.well-known/nostr.json?name=${nameWithDomain[0]}"
            return fetchDetails(finalUrl, nameWithDomain[0], client)
        }
    }

    private suspend fun fetchDetails(composedUrl: String, userName: String, client: HttpClient): Array<String> {
        val obtainedResponse = client.config { followRedirects = false }.get(urlString = composedUrl)

        if (obtainedResponse.status.value in 200..299){
            val responseData = obtainedResponse.bodyAsText()

            return parseResponseData(responseData, userName)
        }
        else throw ResponseException(obtainedResponse, obtainedResponse.status.description)
    }

    private fun parseResponseData(responseData: String, userName: String): Array<String> {
        val motherObject = eventMapper.parseToJsonElement(responseData).jsonObject
        val namesChild = motherObject["names"]?.jsonObject
        val profile = namesChild?.get(userName)?.jsonPrimitive?.content
        if (profile == null) {
            throw Nip05ValidationError("Could not find a corresponding pubkey for this address.")
        }
        else {
            val relaysChild = motherObject["relays"]?.jsonObject
            val userRelays = relaysChild?.get(profile)?.jsonArray
            if (userRelays == null) {
                return arrayOf(profile)
            }
            else {
                val relayList = eventMapper.decodeFromJsonElement(arraySerializer, userRelays)
                return arrayOf(profile, *relayList)
            }
        }
    }
}