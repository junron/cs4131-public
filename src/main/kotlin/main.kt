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
            .project("assignment-2")
            .connect()

        val collection = pyrostore.collection("hello", String.serializer())
        collection.refresh {
            println("Loaded items: $it")
            collection.plusAssign("Hello, world") { item ->
                println("Item added3: $item")
            }
        }

    }
}
