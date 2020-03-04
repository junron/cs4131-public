package com.junron.pyrostore

import com.junron.pyrostore.WebsocketHandler.handleConnect
import com.junron.pyrostore.WebsocketHandler.handleDisconnect
import com.junron.pyrostore.WebsocketHandler.handleMessage
import com.junron.pyrostore.WebsocketHandler.sendMessage
import com.junron.pyrostore.auth.CertificateAuthority
import com.junron.pyrostore.auth.certificates
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import java.io.File

fun main(args: Array<String>) {
    val port = if (args.size == 1) args[0].toInt() else 8080
    embeddedServer(
        Netty,
        watchPaths = listOf("build/classes/kotlin/pyrobase.main"),
        module = Application::server,
        port = port
    ).start(true)
}

@UseExperimental(UnstableDefault::class)
fun Application.server() {
    install(XForwardedHeaderSupport)
    install(WebSockets)

    CertificateAuthority.loadKeys()

    routing {
        route("/certificates"){
            certificates()
        }

        get("/api/covid-19") {
            val overrideFile = File("override.json")
            val fullResponse =
                if (overrideFile.exists()) {
                    Json.nonstrict.parse(FullResponse.serializer(), overrideFile.readText())
                } else {
                    Scrape.initDocument()
                    FullResponse(
                        Scrape.getDorscon(),
                        Scrape.getCaseData(),
                        Scrape.getLastUpdated()
                    )
                }

            call.respondText(
                contentType = ContentType.Application.Json,
                text = Json.indented.stringify(FullResponse.serializer(), fullResponse)
            )
        }
        webSocket("/websockets") {
            handleConnect(call, this)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val message =
                            try {
                                Json.parse(WebsocketMessage.serializer(), text)
                            } catch (e: JsonDecodingException) {
                                return@webSocket sendMessage(
                                    WebsocketMessage.Error(
                                        "Invalid JSON"
                                    )
                                )
                            }
                        handleMessage(this, message)
                    }
                }
            }
            handleDisconnect(this)
        }
    }
}
