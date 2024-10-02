package lean4ij.infoview

import lean4ij.lsp.data.*
import com.google.common.io.Resources
import com.google.gson.*
import com.intellij.testFramework.TestDataPath
import lean4ij.lsp.LeanLanguageServer.Companion.gson
import org.junit.Test
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class GoalTest {

    @Test
    fun testParseGoal() {
        val s = Resources.toString(Resources.getResource("test_goal.json"), StandardCharsets.UTF_8)
        val t : Any = Gson().fromJson(s, Any::class.java)
        // println(s)

//        GsonBuilder().registerTypeAdapterFactory(
//            // copy from ideaIC-2024.1.3-sources.jar!/org/jetbrains/io/jsonRpc/JsonRpcServer.kt:33
//            object : TypeAdapterFactory {
//                private var typeAdapter: IntArrayListTypeAdapter<IntArrayList>? = null
//
//                override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
//                    if (type.type !== IntArrayList::class.java) {
//                        return null
//                    }
//
//                    if (typeAdapter == null) {
//                        typeAdapter = IntArrayListTypeAdapter()
//                    }
//                    @Suppress("UNCHECKED_CAST")
//                    return typeAdapter as TypeAdapter<T>?
//                }
//            }
        var goals : InteractiveGoals = gson.fromJson(s, InteractiveGoals::class.java)
        val sb = InfoviewRender()
        goals.toInfoViewString(sb)
        println(sb.toString())


    }

    /**
     *
    structure InteractiveGoals where
    goals : Array InteractiveGoal
    deriving RpcEncodable
     */
    fun buildCodeWithInfos(t: Map<String, Any>) {

    }

}
