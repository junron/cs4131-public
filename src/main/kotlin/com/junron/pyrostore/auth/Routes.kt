package com.junron.pyrostore.auth

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.serialization.json.Json

fun Route.certificates() {
    get("/public.key") {
        call.respondText(CertificateAuthority.getPublicKey())
    }

    post("/new") {
        val text = call.receiveText()
        val csr = Json.plain.parse(CSR.serializer(), text)
        val certificate = csr.generateCertificate()
        if (certificate == null) {
            return@post call.respondText("Invalid request")
        } else {
            return@post call.respondText(
                Json.plain.stringify(SignedCertificate.serializer(), certificate),
                ContentType.Application.Json
            )
        }
    }
}
