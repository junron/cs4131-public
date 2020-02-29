package com.junron.pyrostore

import com.junron.pyrostore.WebsocketMessage.*
import kotlinx.serialization.UnstableDefault

internal object MessageHandler {
    internal val messageIds =
        mutableMapOf<String, ((message: CollectionItem?) -> Unit)?>()

    @UnstableDefault
    fun onMessage(message: WebsocketMessage, pyroStore: PyroStore) {
        when (message) {
            is Error -> println("Error: " + message.message)
            is AuthError -> println("AuthError: " + message.message)
            is ProjectConnected -> {
                println("Connected to project ${message.project.name}")
                pyroStore.project = message.project
                onProjectConnect()
            }
            is Auth -> {
                println("Authenticated as user ${message.user.name}")
                pyroStore.user = message.user
            }
            is ItemAdded -> {
                println("Item added: ${message.item}")
                if (message.messageId in messageIds) {
                    messageIds[message.messageId]?.invoke(message.item)
                    messageIds.remove(message.messageId)
                    return
                }
                val collection = pyroStore.collections.firstOrNull {
                    it.name == message.collectionName
                } ?: return println("Collection not initialized.")
                collection.internalAddItem(message.item)
            }
            is ItemEdited -> {
                println("Item edited: ${message.item}")
                if (message.messageId in messageIds) {
                    messageIds[message.messageId]?.invoke(message.item)
                    messageIds.remove(message.messageId)
                    return
                }
                val collection = pyroStore.collections.firstOrNull {
                    it.name == message.collectionName
                } ?: return println("Collection not initialized.")
                collection.internalUpdateItem(message.item)
            }
            is ItemDeleted -> {
                println("Deleted edited: ${message.id}")
                if (message.messageId in messageIds) {
                    messageIds[message.messageId]?.invoke(null)
                    messageIds.remove(message.messageId)
                    return
                }
                val collection = pyroStore.collections.firstOrNull {
                    it.name == message.collectionName
                } ?: return println("Collection not initialized.")
                collection.internalDeleteItem(message.id)
            }
            is CollectionLoaded -> {
                println("Loaded collection ${message.name}")
                val collection = pyroStore.collections.firstOrNull {
                    it.name == message.name
                } ?: return println("Collection not initialized.")
                collection.internalSetCollection(message.item)
                if (message.messageId in messageIds) {
                    messageIds[message.messageId]?.invoke(null)
                    messageIds.remove(message.messageId)
                }
            }
        }
    }
}
