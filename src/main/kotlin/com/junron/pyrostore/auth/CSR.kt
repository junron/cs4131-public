package com.junron.pyrostore.auth

import com.junron.pyrostore.Either
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CSR(val publicKey: String, val name: String, val id: String, val token: String) {
    private fun verify(): Boolean {
        val result = auth(token)
        if (result is Either.Left) {
            val user = result.value
            return (name == user.name && id == user.id)
        }
        return false
    }

    fun generateCertificate(): SignedCertificate? {
        if(!verify()) return null
        val certificate = Certificate(publicKey,name, id)
        val message = Json.plain.stringify(Certificate.serializer(), certificate)
        return SignedCertificate(certificate, CertificateAuthority.sign(message))
    }
}

@Serializable
data class Certificate(val publicKey: String, val name: String, val id: String)
@Serializable
data class SignedCertificate(val certificate: Certificate, val signature: String)
