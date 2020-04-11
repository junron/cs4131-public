package com.junron.pyrostore.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.junron.pyrostore.Either
import com.junron.pyrostore.Storage
import com.junron.pyrostore.User
import com.junron.pyrostore.admin.PyroFirebase.firebaseApp
import com.junron.pyrostore.apis.forbidden
import com.junron.pyrostore.auth.auth
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import kotlinx.serialization.json.Json

val tokens = Storage("fcmTokens", FcmToken.serializer())

fun Route.notifications() {
    val messaging = FirebaseMessaging.getInstance(firebaseApp)
    post("addToken") {
        val request = Json.parse(AddTokenRequest.serializer(), call.receiveText())
        val user = (auth(request.auth) as? Either.Left<User>)?.value ?: return@post run {
            call.forbidden()
        }
        if (tokens.any { it.item.token == request.token }) {
            call.respondText { "Token already added" }
            return@post
        }
        val id = user.id ?: return@post
        tokens += FcmToken(request.token, id)
        call.respondText { "Ok" }
    }
    post("sendNotification") {
        val request = Json.parse(SendNotificationRequest.serializer(), call.receiveText())
        (auth(request.auth) as? Either.Left<User>)?.value ?: return@post run {
            call.forbidden()
        }
        val targetTokens = if (request.targetIds.first() == "*") tokens else tokens
            .filter { it.item.userId in request.targetIds }

        if(targetTokens.isEmpty()){
            call.respondText { "No matching ids" }
            return@post
        }

        val response = messaging.sendMulticast(
            MulticastMessage.builder()
                .apply {
                    request.data.forEach { (key, value) -> putData(key, value) }
                    addAllTokens(targetTokens.map { it.item.token })
                }.build()
        )
        call.respondText { "${response.successCount} messages sent successfully" }
        if (response.failureCount > 0) {
            response.responses.forEachIndexed { index, sendResponse ->
                if (sendResponse.isSuccessful) return@post
                println("Removed token ${targetTokens[index].item.token}")
                tokens -= targetTokens[index].id
            }
        }
    }
}
