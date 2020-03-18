package com.junron.pyrostore.admin

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.junron.pyrostore.auth.Certificate
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File

@UnstableDefault
object PyroFirebase {
    val mentorReps = Json.parse(String.serializer().list, File("secret/mentorReps.json").readText())
    private val firebaseApp = FirebaseApp.initializeApp(
        FirebaseOptions.builder()
            .setCredentials(
                GoogleCredentials.fromStream(File("secret/admin-sdk.json").inputStream())
            )
            .setServiceAccountId(
                Json.nonstrict.parse(
                    FirebaseCredentials.serializer(),
                    File("secret/admin-sdk.json").readText()
                ).serviceAccountId
            )
            .build()
    )

    fun getJWT(user: Certificate): String {
        val (_, name, id) = user
        return FirebaseAuth.getInstance(firebaseApp)
            .createCustomToken(
                id, mapOf(
                    "name" to name,
                    "email" to id,
                    "mentorRep" to (id in mentorReps)
                )
            )
    }
}

@Serializable
class FirebaseCredentials(
    @SerialName("client_email")
    val serviceAccountId: String
)
