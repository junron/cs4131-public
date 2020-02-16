package com.junron.pyrostore

import com.junron.pyrostore.WebsocketMessage.ProjectConnect
import io.ktor.client.HttpClient
import io.ktor.client.features.cookies.ConstantCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.websocket.ClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.features.websocket.wss
import io.ktor.http.Cookie
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException

@UnstableDefault
class PyroStore {
    private var projectName = ""
    private var auth = ""
    private var local = false
    private var url = "localhost"
    private lateinit var connection: ClientWebSocketSession
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
        val client = HttpClient {
            install(WebSockets)
            install(HttpCookies) {
                storage = ConstantCookiesStorage(Cookie("user_sess", auth, domain = url))
            }
        }
        if (local) {
            client.ws(
                port = 8080,
                path = "/websockets",
                block = ::connectionHandler
            )
        } else {
            client.wss(
                host = url,
                port = 443,
                path = "/websockets",
                block = ::connectionHandler
            )
        }
        return this
    }

    private suspend fun connectionHandler(connection: ClientWebSocketSession) {
        this.connection = connection
        if (projectName.isNotEmpty()) {
            connection.sendMessage(ProjectConnect(projectName))
        }
        onConnect()
        for (frame in connection.incoming) {
            if (frame !is Frame.Text) return
            val message =
                try {
                    Json.parse(WebsocketMessage.serializer(), frame.readText())
                } catch (e: JsonDecodingException) {
                    println("JSON: ${frame.readText()}")
                    return println("JSON decoding error: $e")
                }
            MessageHandler.onMessage(message, this)
        }
        onDisconnect()
    }

    fun <T> collection(name: String, serializer: DeserializationStrategy<T>): PyrostoreCollection<T> {
        val collection = PyrostoreCollection<T>(name, connection, serializer)
        collections.add(collection)
        return collection
    }

}

internal suspend fun WebSocketSession.sendMessage(message: WebsocketMessage) {
    this.outgoing.send(Frame.Text(Json.stringify(WebsocketMessage.serializer(), message)))
}
