package com.junron.pyrostore.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.junron.pyrostore.Either
import com.junron.pyrostore.User
import java.net.URL
import java.security.interfaces.RSAPublicKey

private val jwks = JwkProviderBuilder(URL("https://login.microsoftonline.com/common/discovery/keys"))
    .cached(true)
    .build()

fun auth(token: String): Either<User, String> {
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
}
