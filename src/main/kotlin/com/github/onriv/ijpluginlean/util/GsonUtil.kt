package com.github.onriv.ijpluginlean.util

import com.google.gson.Gson
import com.google.gson.JsonElement

object GsonUtil {

    fun toJsonElement(json: String): JsonElement {
        return Gson().fromJson(json, JsonElement::class.java)
    }


}