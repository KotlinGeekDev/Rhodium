package ballast.net

import io.ktor.client.*
import io.ktor.client.engine.cio.*

internal actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(CIO) {
    config(this)
    engine {

    }
}