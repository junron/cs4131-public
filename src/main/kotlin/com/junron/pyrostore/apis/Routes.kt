package com.junron.pyrostore.apis

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.serialization.json.Json

fun Route.apis() {

    get("token-test") {
        if (apiAuth(Api.COVID_19, call))
            call.respondText { "Your token is valid!" }
    }

    get("covid-19") {
        with(call) {
            val token =
                request.headers["x-pyrobase-api-token"] ?: request.queryParameters["pyrobase-api-token"]
                ?: receiveText().substringAfter("\"pyrobase-api-token\":\"").substringBefore("\"")
            val ip = request.origin.remoteHost
            if (token.isBlank()) {
                println("IP $ip Token not used!")
            } else {
                println("IP $ip Token: $token")
            }
        }
        Scrape.initDocument()

        val fullResponse = FullResponse(
            Scrape.getDorscon(),
            Scrape.getCaseData(),
            Scrape.getLastUpdated()
        )

        call.respondText(
            contentType = ContentType.Application.Json,
            text = Json.indented.stringify(FullResponse.serializer(), fullResponse)
        )
    }
}