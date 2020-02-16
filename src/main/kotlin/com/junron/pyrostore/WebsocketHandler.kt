package com.junron.pyrostore

import com.junron.pyrostore.WebsocketMessage.*
import io.ktor.application.ApplicationCall
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.nio.channels.ClosedChannelException

@UnstableDefault
object WebsocketHandler {
    private val connections = mutableMapOf<WebSocketSession, User>()

    suspend fun handleConnect(call: ApplicationCall, context: WebSocketSession) {
        println(call.request.cookies.rawCookies)
        val token = call.request.cookies["user_sess"].makeNullIfEmpty() ?: return run {
            val user = User(false, "Anonymous")
            connections[context] = user
            context.sendMessage(Auth(user))
        }
        when (val result = auth(token)) {
            is Either.Left -> {
                connections[context] = result.value
                context.sendMessage(Auth(result.value))
            }
            is Either.Right -> {
                context.sendMessage(AuthError(result.value))
                context.close(CloseReason(401, "Unauthenticated"))
            }
        }
    }

    suspend fun broadcast(message: WebsocketMessage, project: Project) {
        for (webSocketSession in connections.keys) {
            val user = connections[webSocketSession] ?: continue
            try {
                if (user.project == project) webSocketSession.sendMessage(message)
            } catch (e: ClosedChannelException) {
                connections.remove(webSocketSession)
            }
        }
    }

    suspend fun WebSocketSession.sendMessage(message: WebsocketMessage) {
        this.outgoing.send(Frame.Text(Json.stringify(WebsocketMessage.serializer(), message)))
    }

    fun handleDisconnect(context: WebSocketSession) {
        println("Removed connection")
        connections.remove(context)
    }

    suspend fun handleMessage(session: WebSocketSession, message: WebsocketMessage) {
        with(session) {
            val user = connections[session] ?: return reject("User does not exist")
            when (message) {
                is ProjectConnect -> {
                    val project = Projects[message.projectName] ?: return reject("Project does not exist")
                    if (project.auth && !user.authed) return reject("Authentication required for project")
                    connections[session] = user.copy(project = project)
                    sendMessage(ProjectConnected(project))
                }
                is AddItem -> {
                    val project = user.project ?: return reject("Project does not exist")
                    val result = Operations.addItem(message.collectionName, message.item, project)
                    if (result.error) {
                        reject(result.message)
                    }
                }
                is EditItem -> {
                    val project = user.project ?: return reject("Project does not exist")
                    val result = Operations.setItem(message.collectionName, message.item, project)
                    if (result.error) {
                        reject(result.message)
                    }
                }
                is DeleteItem -> {
                    val project = user.project ?: return reject("Project does not exist")
                    val result = Operations.deleteItem(message.collectionName, message.id, project)
                    if (result.error) {
                        reject(result.message)
                    }
                }
                is LoadCollection -> {
                    val project = user.project ?: return reject("Project does not exist")
                    val result = Operations.loadCollection(message.name, project, this)
                    if (result.error) {
                        reject(result.message)
                    }
                }
            }
        }
    }

    private suspend fun WebSocketSession.reject(message: String) {
        sendMessage(WebsocketMessage.Error(message))
    }
}

private fun String?.makeNullIfEmpty() =
    if (this?.isEmpty() == true) null else this


@Serializable
data class User(
    val authed: Boolean,
    val name: String,
    val id: String? = null,
    val project: Project? = null
)
