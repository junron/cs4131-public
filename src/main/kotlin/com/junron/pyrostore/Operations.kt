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
    suspend fun addItem(name: String, item: CollectionItem, project: Project): Result {
        val collection = project.getCollection(name) ?: return Result(true, "Collection does not exist")
        collection.writeData(collection.data + item)
        WebsocketHandler.broadcast(ItemAdded(name, item), project)
        return Result(false, "Success")
    }

    suspend fun setItem(name: String, item: CollectionItem, project: Project): Result {
        val collection = project.getCollection(name) ?: return Result(true, "Collection does not exist")
        val data = collection.data as MutableList
        data.replaceAll { if (it.id == item.id) item else it }
        collection.writeData(data)
        WebsocketHandler.broadcast(ItemEdited(name, item), project)
        return Result(false, "Success")
    }
    suspend fun deleteItem(name: String, id: String, project: Project): Result {
        val collection = project.getCollection(name) ?: return Result(true, "Collection does not exist")
        val data = collection.data as MutableList
        data.removeIf { it.id == id }
        collection.writeData(data)
        WebsocketHandler.broadcast(ItemDeleted(name, id), project)
        return Result(false, "Success")
    }

    suspend fun loadCollection(name: String, project: Project, connection: WebSocketSession): Result {
        val collection = project.getCollection(name) ?: return Result(true, "Collection does not exist")
        connection.sendMessage(CollectionLoaded(name, collection.data))
        return Result(false, "Success")
    }
}

@UnstableDefault
fun Project.getCollection(name: String) =
    this.dataDir?.getCollectionFile(name)?.let { CollectionFile(it, name) }

@UnstableDefault
class CollectionFile(private val file: File, val name: String) {
    init {
        if(!file.exists()){
            file.createNewFile()
            file.writeText("[]")
        }
    }
    val data: List<CollectionItem>
        get() = Json.parse(CollectionItem.serializer().list, file.readText())

    fun writeData(items: List<CollectionItem>){
        file.writeText(
            Json.stringify(CollectionItem.serializer().list, items)
        )
    }
}

data class Result(val error: Boolean, val message: String)

fun File.getCollectionFile(name: String) = this.resolve("$name.json")
