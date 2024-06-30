package com.github.onriv.ijpluginlean.listeners

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener

class InfoViewHoverListener : EditorMouseMotionListener {
    init {
        println("init called")
    }

    override fun mouseMoved(e: EditorMouseEvent) {
        val line = e.editor.xyToLogicalPosition(e.mouseEvent.point).line
        println("Mouse moved on line: $line")
        // Your custom logic here
    }
}
