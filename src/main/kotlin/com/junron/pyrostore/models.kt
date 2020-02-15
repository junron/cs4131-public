package com.junron.pyrostore

import kotlinx.serialization.Serializable

@Serializable
internal sealed class WebsocketMessage {
    @Serializable
    data class Error(val message: String) : WebsocketMessage()

    @Serializable
    data class AuthError(val message: String) : WebsocketMessage()

    @Serializable
    data class ProjectConnect(val projectName: String) : WebsocketMessage()

    @Serializable
    data class ProjectConnected(val project: Project) : WebsocketMessage()

    @Serializable
    data class Connect(val user: User) : WebsocketMessage()

    @Serializable
    data class Disconnect(val user: User) : WebsocketMessage()

    @Serializable
    data class Auth(val user: User) : WebsocketMessage()

    @Serializable
    data class Message(val message: String, val sender: User, val recipient: User) : WebsocketMessage()

    //     Requests
    @Serializable
    data class AddItem(val collectionName: String, val item: CollectionItem) : WebsocketMessage()

    @Serializable
    data class EditItem(val collectionName: String, val item: CollectionItem) : WebsocketMessage()

    @Serializable
    data class DeleteItem(val collectionName: String, val id: String) : WebsocketMessage()

    @Serializable
    data class LoadCollection(val name: String) : WebsocketMessage()

    //  Collection responses
    @Serializable
    data class ItemAdded(val collectionName: String, val item: CollectionItem) : WebsocketMessage()

    @Serializable
    data class ItemEdited(val collectionName: String, val item: CollectionItem) : WebsocketMessage()

    @Serializable
    data class ItemDeleted(val collectionName: String, val id: String) : WebsocketMessage()

    @Serializable
    data class CollectionLoaded(val name: String, val item: List<CollectionItem>) : WebsocketMessage()
}

@Serializable
internal data class User(
    val authed: Boolean,
    val name: String,
    val id: String? = null,
    val project: Project? = null
)

@Serializable
internal data class Project(
    val name: String,
    val auth: Boolean,
    val collections: List<CollectionConfig>
)

@Serializable
internal data class CollectionConfig(val name: String)

@Serializable
internal data class CollectionItem(val id: String, val data: String)

enum class ChangeType {
    CREATED, UPDATED, DELETED, REFRESHED
}
