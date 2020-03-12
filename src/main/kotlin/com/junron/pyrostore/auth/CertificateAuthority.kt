package com.junron.pyrostore.auth

import org.whispersystems.curve25519.Curve25519
import java.io.File
import java.util.*

object CertificateAuthority {
    private lateinit var publicKey: ByteArray
    private lateinit var privateKey: ByteArray
    fun loadKeys() {
        if (!File("secret/public.key").exists()) generateKeyPair()
        publicKey = File("secret/public.key").readText().loadKeyBytes()
        privateKey = File("secret/private.key").readText().loadKeyBytes()
    }

    fun sign(message: String): String {
        val encoder = Base64.getEncoder()
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        return encoder.encodeToString(cipher.calculateSignature(privateKey, message.toByteArray()))
    }

    fun verify(message: String, signature: String): Boolean {
        val decoder = Base64.getDecoder()
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        val decodedSignature = decoder.decode(signature)
        return cipher.verifySignature(publicKey, message.toByteArray(), decodedSignature)
    }

    fun getPublicKey(): String = Base64.getEncoder().encodeToString(publicKey)

    private fun String.loadKeyBytes() =
        Base64.getDecoder().decode(this)

    private fun generateKeyPair() {
        val instance = Curve25519.getInstance(Curve25519.BEST)
        val encoder = Base64.getEncoder()
        val keyPair = instance.generateKeyPair()
        File("secret/private.key").writeText(encoder.encodeToString(keyPair.privateKey))
        File("secret/public.key").writeText(encoder.encodeToString(keyPair.publicKey))
    }
}
