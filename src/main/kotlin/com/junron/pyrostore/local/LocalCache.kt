package com.junron.pyrostore.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class LocalCache<T>(
    private val name: String,
    private val serializer: KSerializer<T>,
    private val fetch: () -> List<T>,
    private val watch: (List<T>) -> Unit,
    private val items: MutableList<T> = mutableListOf()
) : List<T> by items {
    companion object {
        lateinit var cacheDir: File

        fun init(cacheDir: File) {
            this.cacheDir = cacheDir
        }
    }

    init {
        val cache = cacheDir.resolve("$name.json")
        if (!cache.exists()){
            cache.createNewFile()
            cache.writeText("[]")
        }
    }

    private suspend fun loadCache() = suspendCoroutine<List<T>> {
        val cached = cacheDir.resolve("$name.json")
        val data = Json.parse(serializer.list, cached.readText())
        it.resume(data)
    }

    fun load() {
        GlobalScope.launch(Dispatchers.IO) {
            items.replaceAll(loadCache())
            GlobalScope.launch(Dispatchers.Main) {
                if (items.isNotEmpty())
                    watch(items)
            }
            refresh()
        }
    }

    fun refresh() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                items.replaceAll(fetch())
                cacheDir.resolve("$name.json").writeText(
                    Json.stringify(serializer.list, items)
                )
            } catch (e: Exception) {
                println("Error occurred: $e")
                return@launch
            }
            GlobalScope.launch(Dispatchers.Main) {
                if (items.isNotEmpty())
                    watch(items)
            }
        }
    }


    private fun MutableList<T>.replaceAll(items: List<T>) {
        removeAll { true }
        addAll(items)
    }
}


