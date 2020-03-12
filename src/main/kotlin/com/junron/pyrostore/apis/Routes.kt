package com.junron.pyrostore.apis

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.serialization.json.Json

fun Route.apis() {


    get("covid-19") {
//        if(!apiAuth(Api.COVID_19, call)) return@get
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
