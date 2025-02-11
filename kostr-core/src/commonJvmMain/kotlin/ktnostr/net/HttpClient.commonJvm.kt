package ktnostr.net

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(OkHttp) {
    engine {
        preconfigured = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()
    }
    config(this)

}