package com.github.onriv.ijpluginlean.lsp.data

// import com.github.onriv.ijpluginlean.lsp.Range
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

// TODO this is Lean's source code's def, but the json seems to be just String
// data class FVarId (val name: String)

val gson = GsonBuilder()
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
                val f0: SubexprInfo = p2.deserialize(p1.get(0), SubexprInfo::class.java)
                val f1: CodeWithInfos = p2.deserialize(p1.get(1), CodeWithInfos::class.java)
                return CodeWithInfosTag(f0, f1)
            }
            if (p0.isJsonObject && p0.asJsonObject.has("append")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonArray("append")

                @Suppress("NAME_SHADOWING")
                val p2 = p2!!
                val r: MutableList<CodeWithInfos> = ArrayList()
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
    })
    .create()