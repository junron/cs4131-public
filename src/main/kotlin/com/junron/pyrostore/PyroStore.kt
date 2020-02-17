package com.junron.pyrostore

import com.junron.pyrostore.WebsocketMessage.ProjectConnect
import com.tinder.scarlet.Message
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

@UnstableDefault
class PyroStore {
    private val backoffStrategy = ExponentialWithJitterBackoffStrategy(10, 5000)
    private var projectName = ""
    private var auth = ""
    private var local = false
    private var url = "localhost"
    private lateinit var service: PyrostoreService
    internal val collections = mutableListOf<PyrostoreCollection<*>>()
    lateinit var project: Project
        internal set
    lateinit var user: User
        internal set

    fun project(name: String): PyroStore {
        projectName = name
        return this
    }

    fun auth(token: String): PyroStore {
        auth = token
        return this
    }

    fun remote(url: String): PyroStore {
        this.url = url
        return this
    }

    fun local(): PyroStore {
        local = true
        return this
    }


    suspend fun connect(): PyroStore {
        val client = OkHttpClient()
            .newBuilder()
            .cookieJar(object : CookieJar {
                override fun loadForRequest(url: HttpUrl) = listOf(
                    Cookie.Builder()
                        .domain(this@PyroStore.url)
                        .name("user_sess")
                        .value(auth)
                        .let {
                            if (!local) it.secure() else it
                        }.build()
                )

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}

            })
            .build()

        val scarlet = Scarlet.Builder()
            .webSocketFactory(
                client.newWebSocketFactory(
                    if (local) "ws://localhost:8080/websockets"
                    else "wss://$url/websockets"
                )
            )
            .backoffStrategy(backoffStrategy)
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
            .build()
        service = scarlet.create()

        for (event in service.receive()) {
            when (event) {
                is WebSocket.Event.OnConnectionOpened<*> -> {
                    println("Connected")
                    if (projectName.isNotEmpty()) {
                        service.sendMessage(ProjectConnect(projectName))
                    }
                    onConnect()
                }
                is WebSocket.Event.OnMessageReceived -> {
                    val text = (event.message as Message.Text).value
                    try {
                        val message = Json.parse(WebsocketMessage.serializer(), text)
                        MessageHandler.onMessage(message, this)
                    } catch (e: JsonDecodingException) {
                        println("JSON: $text")
                    }
                }
                is WebSocket.Event.OnConnectionFailed -> {
                    println("Connection failed: ${event.throwable.message}")
                    onDisconnect()
                }
            }
        }

        return this
    }


    fun <T> collection(name: String, serializer: DeserializationStrategy<T>): PyrostoreCollection<T> {
        val collection = PyrostoreCollection<T>(name, service, serializer)
        collections.add(collection)
        return collection
    }

}

interface PyrostoreService {
    @Send
    fun send(text: String)

    @Receive
    fun receive(): ReceiveChannel<WebSocket.Event>
}

internal fun PyrostoreService.sendMessage(message: WebsocketMessage) {
    send(Json.stringify(WebsocketMessage.serializer(), message))
}
