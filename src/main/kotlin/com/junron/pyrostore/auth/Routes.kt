package com.junron.pyrostore.auth

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
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
            val token = csr.generateToken()
            return@post call.respondText(
                Json.plain.stringify(
                    SignedCertificateWithToken.serializer(), SignedCertificateWithToken(
                        certificate,
                        token
                    )
                ),
                ContentType.Application.Json
            )
        }
    }
}
