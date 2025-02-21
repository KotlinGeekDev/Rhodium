# Rhodium

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-blue?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlingeekdev/ballast?color=blue)](https://search.maven.org/search?q=g:io.github.kotlingeekdev)

![badge-jvm](http://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-linux](http://img.shields.io/badge/platform-linux-2D3F6C.svg?style=flat)
![badge-mac](http://img.shields.io/badge/platform-macos-111111.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)

A Kotlin Multiplatform library for working with Nostr, with support for JVM, Android, Linux, MacOS/iOS.

Note: This is still in development and very incomplete.


## What is Nostr?
* An introduction or description of Nostr can be found [here](https://github.com/nostr-protocol/nostr).
* The Nostr protocol specs can be found [here](https://github.com/nostr-protocol/nips).

## How to include the libary
You can include the library from either Maven Central or Jitpack.

### Maven
You can include the library in the common source set like this:
```kotlin
dependencies {
    implementation("io.github.kotlingeekdev:rhodium:1.0-beta-17")

}
```

### Jitpack
  Inside your root-level `build.gradle(.kts)` file, you should add `jitpack`:
  ``` kotlin
// build.gradle.kts
allprojects {
    repositories {
        // ...
        maven { setUrl("https://jitpack.io") }
    }
    // ...
}
```

or

``` groovy
// build.gradle
allprojects {
    repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
    // ...
}
```

In newer projects, you need to also update the `settings.gradle(.kts)` file's `dependencyResolutionManagement` block:

```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // <--
        jcenter() // Warning: this repository is going to shut down soon
    }
}
```
then, in your module's `build.gradle(.kts)`, you need to add:
```kotlin
// build.gradle.kts
dependencies {
    //...
    implementation("com.github.KotlinGeekDev.Rhodium:rhodium:1.0-beta-17")


}

```
If you're including it in an Android app, you can just add:
```kotlin
// app/build.gradle.kts
dependencies {
    //...
    implementation("com.github.KotlinGeekDev.Rhodium:rhodium-android:1.0-beta-17")

}
```

## Usage
When publishing an event, or making a subscription/close request to a relay, 
[`ClientMessage`](rhodium-core/src/commonMain/kotlin/rhodium/nostr/client/ClientMessage.kt) is used to encode the request/event,
and anything sent by a relay is encoded as a [`RelayMessage`](rhodium-core/src/commonMain/kotlin/rhodium/nostr/relay/RelayMessage.kt).</p>
Relays can be configured using a `RelayPool`, 
and actual communication with relays is done with the `NostrService`.</p>
You can setup the NostrService with/without a custom relay pool as follows:
```kotlin
// With a custom relay pool. 
val requestRelays = RelayPool.fromUrls("wss://relay1", "wss://relay2")
val clientService = NostrService(
    relayPool = requestRelays
)

//Without a custom pool(using the default pool)
val service = NostrService()
```
You can also use a custom HTTP client for the `NostrService`, as long as it has Websocket support.
```kotlin
val myHttpClient = httpClient { 
    // custom configs(or not)
}
val service = NostrService(customClient = myHttpClient)
```
The current limitation is that the HTTP client needs to be Ktor-compatible, that is, you create a 
custom Ktor engine that uses you client underneath.

Note that if you need to do anything custom, such as using read-only relays,
you will need to setup the list of relays, then use them in the relay pool:
```kotlin
val customRelays = listOf(
    Relay("wss://relay1", readPolicy = true, writePolicy = false), // <-- A relay with custom read/write policy.
    Relay("wss://relay2"),
)

val customPool = RelayPool(relays = customRelays)
```
### Making a subscription request
In order to make a subscription request, you need to construct a `RequestMessage`.
And to do that, you need to pass in a subscriptionId(just a string), and a `NostrFilter`:
```kotlin
val postsCommentsByFiatjafFilter = NostrFilter.newFilter()
                    .kinds(EventKind.TEXT_NOTE.kind, EventKind.COMMENT.kind) // <-- Looking for posts and comments. Other kinds can be added
                    .authors("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d") // <-- The profiles for which we want to find the posts and comments(as indicated by .kinds() above)
                    .limit(20) // <-- Setting a limit is important, to avoid issues with relays
                    .build()

val myRequest = RequestMessage(
    subscriptionId = "fiatjaf_posts_and_comments",
    filters = listOf(postsCommentsByFiatjafFilter)
)
```

If you want to make a request with a single filter, you can do so with `RequestMessage.singleFilterRequest`. 
Using the same example above, we can rewrite it as:

```kotlin
val postsCommentsByFiatjafFilter = ... //Same as above

val myRequest = RequestMessage.singleFilterRequest(  // <- singleFilterRequest is called here.
    subscriptionId = "fiatjaf_posts_and_comments",
    filter = postsByFiatjafFilter
)
```
Now, you can use the `NostrService` to make the request, either using `request()` or `requestWithResult()`. 
They are both `suspend` functions, and as such, should be called within the appropriate context.</p>
**Note**: `requestWithResult` terminates the connection(s) after receiving all the expected messages. This behaviour could be 
modified in the future.

The `request()` function has a set of callbacks which are used to handle the incoming messages,
`onRelayMessage: suspend (Relay, RelayMessage)`, as well as callbacks for handling errors, `onRequestError(Relay, Throwable)`.
An example is given below:
```kotlin
// Example coroutine scope
val appScope = CoroutineScope(Dispatchers.IO)
//Using request() --
appScope.launch {
    clientService.request(
        myRequest, 
        onRequestError = { relay, throwable -> handleError(relay, throwable) }
    ) { relay: Relay, received: RelayMessage ->  // This is a suspend callback, so suspend functions can be used here.
        useMessage(relay, received)
    }
}
```
The `requestWithResult()` function returns a list of events(`List<Event>`), no matter the errors that occur
during its execution. The function however displays the errors using standard output. This may be changed in the future.
```kotlin
//Using requestWithResult() ---
// - Assuming a suspending context:
val events = clientService.requestWithResult(myRequest)
events.forEach { println(it.content) }
// OR, following with the request() example above:
appScope.launch {
  val events = clientService.requestWithResult(myRequest)
  events.forEach { println(it.content) }  
}
```
## License

    MIT License
    
    Copyright (c) 2022 KotlinGeekDev
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
