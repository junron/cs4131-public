import com.junron.pyrostore.ChangeType
import com.junron.pyrostore.PyroStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.serializer
import java.io.File

@UnstableDefault
fun main() {
    runBlocking {
        val cacheDir = File("cache")
        cacheDir.mkdir()
        val pyrostore = PyroStore()
            .remote("pyrostore.nushhwboard.ml")
            .cache(cacheDir)
            .project("test")

        pyrostore.connect()


        val collection = pyrostore.collection("hello", String.serializer())
        collection.watch { type, id, item ->
            if (type == ChangeType.REFRESHED) {
                println("Data loaded: $collection")
                return@watch
            }
            println(type)
            println(id)
            println(item)
        }
        collection.plusAssign("Hello, world") {
            println("Added")
        }

    }
}
