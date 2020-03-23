package com.junron.pyrostore.fcm

import kotlinx.serialization.Serializable

@Serializable
data class AddTokenRequest(val token: String, val auth: String)

@Serializable
data class SendNotificationRequest(val targetId: String, val data: Map<String, String>, val auth: String)

@Serializable
data class FcmToken(val token: String, val userId: String)
