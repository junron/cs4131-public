import com.junron.pyrostore.ChangeType
import com.junron.pyrostore.PyroStore
import com.junron.pyrostore.onProjectConnect
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.serializer

@UnstableDefault
fun main() {
    runBlocking {
        val pyrostore = PyroStore()
            .local()
            .project("test")

        pyrostore.onProjectConnect {
            GlobalScope.launch {
                val collection = pyrostore.collection("hello", String.serializer())
                collection.watch { type, id, item ->
                    if(type == ChangeType.REFRESHED){
                        println(collection)
                        return@watch
                    }
                    println(type)
                    println(id)
                    println(item)
                }
                collection += "Hello, world"
            }
        }

        pyrostore.connect()
    }
}
