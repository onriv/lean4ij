package com.github.onriv.ijpluginlean.util

import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.google.gson.Gson
import com.google.gson.JsonElement

object GsonUtil {

     inline fun <reified T> fromJson(json: String) : T {
        return Gson().fromJson(json, T::class.java)
    }

    fun toJsonElement(json: String): JsonElement {
        return Gson().fromJson(json, JsonElement::class.java)
    }

    fun toJson(any: Any): String {
        return Gson().toJson(any)
    }

}