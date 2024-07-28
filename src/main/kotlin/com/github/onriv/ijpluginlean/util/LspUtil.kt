package com.github.onriv.ijpluginlean.util

object LspUtil {
    private const val PREFIX = "file:///"

    fun quote(url: String) : String {
        if (url.startsWith(PREFIX)) {
            return url
        }
        return PREFIX+url
    }

    /**
     * unquoting the path "file:///C:/Users/..." to "C:/Users/.."
     * if it's not with the pattern, then just return it
     */
    fun unquote(url: String) : String {
        if(!url.startsWith(PREFIX)) {
            return url
        }
        return url.substring(PREFIX.length)
    }
}