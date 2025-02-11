package ktnostr.net

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

internal actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(OkHttp) {
    config(this)

}