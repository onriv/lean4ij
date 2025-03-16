package lean4ij.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJson(src: String?): T {
    val type = object : TypeToken<T>() {}.type
    return this.fromJson(src, type)
}

inline fun <reified T> Gson.fromJson(src: JsonElement?): T {
    val type = object : TypeToken<T>() {}.type
    return this.fromJson(src, type)
}

