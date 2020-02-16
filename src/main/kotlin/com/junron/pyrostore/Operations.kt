package com.junron.pyrostore

import com.junron.pyrostore.WebsocketHandler.sendMessage
import com.junron.pyrostore.WebsocketMessage.*
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File

@UnstableDefault
object Operations {
    suspend fun addItem(collection: CollectionFile, item: CollectionItem, project: Project): Result {
        collection.writeData(collection.data + item)
        WebsocketHandler.broadcast(ItemAdded(collection.name, item), project)
        return Result(false, "Success")
    }

    suspend fun setItem(collection: CollectionFile, item: CollectionItem, project: Project): Result {
        val data = collection.data as MutableList
        data.replaceAll { if (it.id == item.id) item else it }
        collection.writeData(data)
        WebsocketHandler.broadcast(ItemEdited(collection.name, item), project)
        return Result(false, "Success")
    }

    suspend fun deleteItem(collection: CollectionFile, id: String, project: Project): Result {
        val data = collection.data as MutableList
        data.removeIf { it.id == id }
        collection.writeData(data)
        WebsocketHandler.broadcast(ItemDeleted(collection.name, id), project)
        return Result(false, "Success")
    }

    suspend fun loadCollection(collection: CollectionFile, connection: WebSocketSession): Result {
        connection.sendMessage(CollectionLoaded(collection.name, collection.data))
        return Result(false, "Success")
    }
}

@UnstableDefault
fun Project.getCollection(name: String, user: User) =
    this.dataDir
        ?.namespaced(this.collections.firstOrNull { it.name == name }, user)
        ?.getCollectionFile(name)?.let {
            CollectionFile(it, name)
        }

@UnstableDefault
class CollectionFile(private val file: File, val name: String) {
    init {
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("[]")
        }
    }

    val data: List<CollectionItem>
        get() = Json.parse(CollectionItem.serializer().list, file.readText())

    fun writeData(items: List<CollectionItem>) {
        file.writeText(
            Json.stringify(CollectionItem.serializer().list, items)
        )
    }
}

data class Result(val error: Boolean, val message: String)

fun File.getCollectionFile(name: String) = this.resolve("$name.json")

fun File.namespaced(collectionConfig: CollectionConfig?, user: User) =
    if (collectionConfig?.prefix == true) {
        with(user.id?.let { this.resolve(it) }) {
            if (this?.exists() == false) this.mkdirs()
            this
        }
    } else this

