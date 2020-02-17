package com.junron.pyrostore

import com.junron.pyrostore.WebsocketMessage.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.util.*

@UnstableDefault
class PyrostoreCollection<T>(
    val name: String,
    private val service: PyrostoreService,
    private val serializer: DeserializationStrategy<T>,
    private val items: MutableList<ItemWrapper<T>> = mutableListOf()
) : List<ItemWrapper<T>> by items {
    private val watchers = mutableListOf<(ChangeType, String?, T?) -> Unit>()

    init {
        GlobalScope.launch {
            service.sendMessage(LoadCollection(name))
        }
    }

    internal fun internalSetCollection(collection: List<CollectionItem>) {
        val items = collection.map {
            ItemWrapper(it.id, Json.parse(serializer, it.data))
        }
        this.items.removeAll { true }
        items.forEach {
            this.items.add(it)
        }
        broadcast(ChangeType.REFRESHED)
    }

    internal fun internalAddItem(item: CollectionItem) {
        val data = Json.parse(serializer, item.data)
        items += ItemWrapper(item.id, data)
        broadcast(ChangeType.CREATED, item.id, data)
    }

    internal fun internalUpdateItem(item: CollectionItem) {
        val data = Json.parse(serializer, item.data)
        this.items.replaceAll { if (it.id == item.id) ItemWrapper(item.id, data) else it }
        broadcast(ChangeType.UPDATED, item.id, data)
    }

    internal fun internalDeleteItem(id: String) {
        this.items.removeIf { it.id == id }
        broadcast(ChangeType.DELETED, id)
    }

    private fun broadcast(type: ChangeType, id: String? = null, item: T? = null) {
        watchers.forEach { it(type, id, item) }
    }

    operator fun plusAssign(item: T) {
        val id = UUID.randomUUID().toString()
//        items += ItemWrapper(id, item)
        service.sendMessage(
            AddItem(
                name, CollectionItem(
                    id,
                    Json.stringify(serializer as SerializationStrategy<T>, item)
                )
            )
        )
    }

    operator fun minusAssign(id: String) {
//        internalDeleteItem(id)
        service.sendMessage(
            DeleteItem(name, id)
        )
    }

    operator fun get(id: String) = items.firstOrNull { it.id == id }

    operator fun set(id: String, item: T) {
        service.sendMessage(
            EditItem(
                name, CollectionItem(
                    id,
                    Json.stringify(serializer as SerializationStrategy<T>, item)
                )
            )
        )
    }

    fun watch(callback: (type: ChangeType, id: String?, item: T?) -> Unit) {
        watchers += callback
    }

    override fun toString() = items.toString()
}

data class ItemWrapper<T>(val id: String, val item: T)
