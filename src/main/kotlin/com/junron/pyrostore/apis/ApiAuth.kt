package com.junron.pyrostore.apis

import com.junron.pyrostore.auth.CertificateAuthority
import com.junron.pyrostore.auth.CertificateAuthority.sign
import com.junron.pyrostore.auth.CertificateAuthority.verify
import com.junron.pyrostore.uuid
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.*


suspend fun apiAuth(name: Api, call: ApplicationCall): Boolean {
    val decoder = Base64.getUrlDecoder()
    with(call) {
        val token =
            request.headers["x-pyrobase-api-token"] ?: request.queryParameters["pyrobase-api-token"]
            ?: receiveText().substringAfter("\"pyrobase-api-token\":\"").substringBefore("\"")
        if ("." !in token) return unauth()
        val (tokenData, signature) = token.split(".")
        val tokenInfo: ApiToken
        try {
            tokenInfo =
                Json.parseOrNull(ApiToken.serializer(), decoder.decode(tokenData).toString(Charsets.UTF_8))
                    ?: return unauth()
            if (tokenInfo.apiName != name) return forbidden()
//         Verify signature
            if (!verify(tokenData, signature)) return forbidden()
        } catch (e: IllegalArgumentException) {
            println("Malformed token: $token")
            return forbidden()
        }

        println("Application ${tokenInfo.applicationName} accessed api $name")
        return true
    }
}

private suspend fun ApplicationCall.unauth() = run {
    respond(
        HttpStatusCode.Unauthorized,
        "Unauthenticated"
    )
    false
}

private suspend fun ApplicationCall.forbidden() = run {
    respond(
        HttpStatusCode.Forbidden,
        "Unauthorized"
    )
    false
}

@Serializable
private data class ApiToken(val id: String, val applicationName: String, val apiName: Api)

fun <T> Json.Companion.parseOrNull(serializer: KSerializer<T>, string: String): T? {
    return try {
        parse(serializer, string)
    } catch (e: SerializationException) {
        null
    }
}

fun genToken() {
    CertificateAuthority.loadKeys()
    val encoder = Base64.getUrlEncoder()
    val apis = Api.values()
    println("Available APIs:")
    apis.forEachIndexed { index, api ->
        println("[$index] $api")
    }
    println("Select an API: (0-${apis.lastIndex})")
    val api = apis[readLine()!!.toInt()]
    print("Enter application name: ")
    val appName = readLine()!!.trim()
    println("Token information:")
    println("API: $api")
    println("App name:$appName")
    println("---------")
    val token = ApiToken(uuid(), appName, api)
    val tokenEncoded = encoder.encodeToString(Json.stringify(ApiToken.serializer(), token).toByteArray())
    val signature = sign(tokenEncoded)
    println("Token: $tokenEncoded.$signature")
}

fun main() {
    genToken()
}
