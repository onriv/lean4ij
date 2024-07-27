package com.github.onriv.ijpluginlean.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import org.junit.Test

class GsonUtilTest {

    @Test
    fun testGsonUtil() {
        val a = Gson().fromJson("""
            {"a":1}
        """.trimIndent(), JsonElement::class.java)
        print(a)
    }

}