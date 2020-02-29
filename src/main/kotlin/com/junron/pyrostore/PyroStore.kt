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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File

@UnstableDefault
class PyroStore {
    private val backoffStrategy = ExponentialWithJitterBackoffStrategy(10, 5000)
    private var projectName = ""
    private var auth = ""
    private var local = false
    private var url = "localhost"
    private lateinit var service: PyrostoreService
    internal lateinit var cacheDir: File
    internal val collections = mutableListOf<PyrostoreCollection<*>>()
    var project: Project? = null
        internal set
    lateinit var user: User
        internal set
    private lateinit var queuedRequests: PyrostoreCollection<WebsocketMessage>
    internal var connected: Boolean = false

    fun project(name: String): PyroStore {
        projectName = name
        return this
    }

    fun auth(token: String): PyroStore {
        auth = token
        return this
    }

    fun cache(dir: File): PyroStore {
        cacheDir = dir
        queuedRequests = collection("internal_request_queue", WebsocketMessage.serializer(), true)
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


    fun connect(): PyroStore {
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

        GlobalScope.launch {
            for (event in service.receive()) {
                when (event) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        connected = true

                        println("Connected")
                        if (projectName.isNotEmpty()) {
                            sendMessage(ProjectConnect(projectName))
                        }
                        collections.filter { !it.offline }
                            .forEach {
                                val queuedItems = queuedRequests.filter { itemWrapper ->
                                    itemWrapper.item.collectionName == it.name
                                }
                                queuedItems.forEach { request ->
                                    queuedRequests.minusAssign(request.id)
                                }
                                it.onConnect(queuedItems.map { itemWrapper -> itemWrapper.item })
                            }
                        onConnect()
                    }
                    is WebSocket.Event.OnMessageReceived -> {
                        val text = (event.message as Message.Text).value
                        try {
                            val message = Json.parse(WebsocketMessage.serializer(), text)
                            MessageHandler.onMessage(message, this@PyroStore)
                        } catch (e: JsonDecodingException) {
                            println("JSON: $text")
                        }
                    }
                    is WebSocket.Event.OnConnectionFailed -> {
                        println("Connection failed: ${event.throwable.message}")
                        connected = false
                        project = null
                        onDisconnect()
                    }
                }
            }
        }

        return this
    }


    fun <T> collection(
        name: String,
        serializer: KSerializer<T>,
        offline: Boolean = false
    ): PyrostoreCollection<T> {
        val collection = PyrostoreCollection(
            name,
            serializer = serializer,
            offline = offline,
            service = this
        )
        if (!offline)
            collections.add(collection)
        return collection
    }

    internal fun sendMessage(
        message: WebsocketMessage,
        id: String? = null,
        callback: ((CollectionItem?) -> Unit)? = null
    ) {
        if (connected) {
            println("Sent: $message")
            service.send(Json.stringify(WebsocketMessage.serializer(), message))
        } else {
            println("Queued: $message")
            queuedRequests.plusAssign(message)
        }
        id ?: return
        MessageHandler.messageIds[id] = callback
    }

}

interface PyrostoreService {
    @Send
    fun send(text: String)

    @Receive
    fun receive(): ReceiveChannel<WebSocket.Event>
}


internal val WebsocketMessage.collectionName: String?
    get() = when (this) {
        is WebsocketMessage.AddItem -> this.collectionName
        is WebsocketMessage.EditItem -> this.collectionName
        is WebsocketMessage.DeleteItem -> this.collectionName
        is WebsocketMessage.LoadCollection -> this.name
        else -> null
    }
