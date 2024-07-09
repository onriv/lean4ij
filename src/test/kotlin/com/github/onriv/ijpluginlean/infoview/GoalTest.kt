package com.github.onriv.ijpluginlean.infoview

import com.github.onriv.ijpluginlean.lsp.data.*
import com.google.common.io.Resources
import com.google.gson.*
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class GoalTest : BasePlatformTestCase() {

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
        var gson =
            GsonBuilder()
                .registerTypeAdapter(CodeWithInfos::class.java, object : JsonDeserializer<CodeWithInfos> {
                override fun deserialize(p0: JsonElement?, p1: Type?, p2: JsonDeserializationContext?): CodeWithInfos? {
                    if (p0 == null) {
                        return null
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("tag")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonArray("tag")
                        @Suppress("NAME_SHADOWING")
                        val p2 = p2!!
                        val f0 : SubexprInfo = p2.deserialize(p1.get(0), SubexprInfo::class.java)
                        val f1 : CodeWithInfos = p2.deserialize(p1.get(1), CodeWithInfos::class.java)
                        return CodeWithInfosTag(f0, f1)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("append")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonArray("append")
                        @Suppress("NAME_SHADOWING")
                        val p2 = p2!!
                        val r : MutableList<CodeWithInfos> = ArrayList()
                        for (e in p1) {
                            r.add(p2.deserialize(e, CodeWithInfos::class.java))
                        }
                        return CodeWithInfosAppend(r)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("text")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonPrimitive("text").asString
                        return CodeWithInfosText(p1)
                    }
                    throw IllegalStateException(p0.toString())
                }
            }).create()
        var goals : InteractiveGoals = gson.fromJson(s, InteractiveGoals::class.java)
        println(goals.toInfoViewString())


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
