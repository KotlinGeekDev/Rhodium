package ballast.net

import io.ktor.client.*

internal expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient