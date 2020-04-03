package com.junron.pyrostore.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.junron.pyrostore.Either
import com.junron.pyrostore.User
import kotlinx.serialization.json.Json
import org.whispersystems.curve25519.Curve25519
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.*

private val jwks = JwkProviderBuilder(URL("https://login.microsoftonline.com/common/discovery/keys"))
    .cached(true)
    .build()

fun auth(token: String): Either<User, String> {
    if (token.startsWith("ey")) {
        val decoded = JWT.decode(token)
        val kid = decoded.getHeaderClaim("kid")?.asString() ?: return Either.Right("Key id not found")
        val alg = Algorithm.RSA256(jwks.get(kid).publicKey as RSAPublicKey, null)
        try {
            alg.verify(decoded)
        } catch (exception: JWTVerificationException) {
            return Either.Right("Invalid JWT signature")
        }
        return Either.Left(
            User(
                true,
                decoded.claims["name"]?.asString()!!,
                decoded.claims["unique_name"]?.asString()!!
            )
        )
    } else {
        val (email, signature) = token.split(":")
        return if (CertificateAuthority.verify(email, signature)) {
            Either.Left(User(true, "", email))
        } else {
            Either.Right("Invalid token")
        }
    }
}

fun genFakeCertificate() {
    CertificateAuthority.loadKeys()
    println("Enter email: ")
    val email = readLine()!!
    println("Enter name: ")
    val name = readLine()!!
    val instance = Curve25519.getInstance(Curve25519.BEST)
    val encoder = Base64.getUrlEncoder()
    val keyPair = instance.generateKeyPair()
    val privateKey = encoder.encodeToString(keyPair.privateKey)
    val csr = CSR(encoder.encodeToString(keyPair.publicKey), name, email, "")
    val certificate = csr.generateCertificate(false) ?: return
    val certificateWithToken = SignedCertificateWithToken(certificate, csr.generateToken())
    println(
        Json.plain.stringify(
            SignedCertificateWithToken.serializer(), certificateWithToken
        ) + "||||" + privateKey + "||||" + email + ":" + CertificateAuthority.sign(email)
    )
}

fun main() {
    genFakeCertificate()
}
