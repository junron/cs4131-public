package com.junron.pyrostore

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File

object Projects {
    private val projects = Json.parse(Project.serializer().list, File("./projects.json").readText())
    val projectDirs: Map<Project, File>

    init {
        projectDirs = projects.map {
            val dir = File("data/${it.name}")
            if (!dir.exists()) dir.mkdir()
            it to dir
        }.toMap()
    }

    operator fun get(name: String) = projects.firstOrNull {
        it.name == name
    }
}


@Serializable
data class Project(val name: String, val auth: Boolean, val collections: List<CollectionConfig>)

val Project.dataDir: File?
    get() = Projects.projectDirs[this]

@Serializable
data class CollectionConfig(val name: String, val prefix: Boolean?)

@Serializable
data class Collection(val name: String, val values: List<CollectionItem>)

@Serializable
data class CollectionItem(val id: String, val data: String)
