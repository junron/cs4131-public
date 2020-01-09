package com.example.msauth.util

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.auth0.android.jwt.Claim
import com.auth0.android.jwt.JWT
import com.example.msauth.R

object Auth {
    lateinit var queue: RequestQueue

    fun init(context: Context) {
        queue = Volley.newRequestQueue(context)
    }

    fun getAccessToken(code: String, callback: (Map<String, Claim>) -> Unit) {
        queue.add(
            object : StringRequest(
                Method.POST,
                "https://login.microsoftonline.com/d72a7172-d5f8-4889-9a85-d7424751592a/oauth2/token",
                { response ->
                    val idToken = response.substringAfter("\"id_token\":\"").substringBefore("\"")
                    callback(JWT(idToken).claims)
                },
                { error ->
                    println(String(error.networkResponse.data))
                    println("Error: $error")
                }
            ) {
                override fun getBody() = ("grant_type=authorization_code&" +
                        "client_id=29ee72fe-a535-4c12-9b2c-c949873ad500&" +
                        "&code=$code").toByteArray()

                override fun getBodyContentType() = "application/x-www-form-urlencoded"
            }
        )
    }
}
