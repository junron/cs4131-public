package com.junron.pyrostore.cache

import com.junron.pyrostore.CollectionItem
import com.junron.pyrostore.PyroStore
import com.junron.pyrostore.PyrostoreCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.list

internal class PyrostoreCache<T>(
    private val collection: PyrostoreCollection<T>,
    pyroStore: PyroStore
) {
    private val cacheFile = pyroStore.cacheDir.resolve("${collection.name}.json")

    init {
        if(!cacheFile.exists()){
            cacheFile.createNewFile()
            cacheFile.writeText("[]")
        }
    }

    suspend fun writeItems(){
        withContext(Dispatchers.IO) {
            val items = collection.map { CollectionItem(
                it.id,
                Json.stringify(collection.serializer, it.item)
            ) }
            cacheFile.writeText(Json.stringify(CollectionItem.serializer().list, items))
        }
    }
    suspend fun loadItems(): List<CollectionItem>{
        return withContext(Dispatchers.IO) {
            val fileData = cacheFile.readText()
            return@withContext Json.parse(CollectionItem.serializer().list, fileData)
        }
    }
}
